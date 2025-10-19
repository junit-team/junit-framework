/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ_WRITE;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.UniqueId;

/**
 * @since 6.1
 */
@API(status = EXPERIMENTAL, since = "6.1")
public class ConcurrentHierarchicalTestExecutorService implements HierarchicalTestExecutorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentHierarchicalTestExecutorService.class);

	private final WorkQueue workQueue = new WorkQueue();
	private final ExecutorService threadPool;
	private final int parallelism;
	private final WorkerLeaseManager workerLeaseManager;

	public ConcurrentHierarchicalTestExecutorService(ConfigurationParameters configurationParameters) {
		this(DefaultParallelExecutionConfigurationStrategy.toConfiguration(configurationParameters));
	}

	public ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration) {
		this(configuration, ClassLoaderUtils.getDefaultClassLoader());
	}

	ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration, ClassLoader classLoader) {
		ThreadFactory threadFactory = new WorkerThreadFactory(classLoader);
		threadPool = new ThreadPoolExecutor(configuration.getCorePoolSize(), configuration.getMaxPoolSize(),
			configuration.getKeepAliveSeconds(), SECONDS, new SynchronousQueue<>(), threadFactory);
		parallelism = configuration.getParallelism();
		workerLeaseManager = new WorkerLeaseManager(parallelism, this::maybeStartWorker);
		LOGGER.trace(() -> "initialized thread pool for parallelism of " + configuration.getParallelism());
	}

	@Override
	public void close() {
		LOGGER.trace(() -> "shutting down thread pool");
		threadPool.shutdownNow();
	}

	@Override
	public Future<@Nullable Void> submit(TestTask testTask) {
		LOGGER.trace(() -> "submit: " + testTask);

		var workerThread = WorkerThread.get();
		if (workerThread == null) {
			return enqueue(testTask).future();
		}

		if (testTask.getExecutionMode() == SAME_THREAD) {
			workerThread.executeTask(testTask);
			return completedFuture(null);
		}

		return new WorkStealingFuture(enqueue(testTask));
	}

	@Override
	public void invokeAll(List<? extends TestTask> testTasks) {
		LOGGER.trace(() -> "invokeAll: " + testTasks);

		var workerThread = WorkerThread.get();
		Preconditions.condition(workerThread != null && workerThread.executor() == this,
			"invokeAll() must be called from a worker thread that belongs to this executor");

		workerThread.invokeAll(testTasks);
	}

	private WorkQueue.Entry enqueue(TestTask testTask) {
		var entry = workQueue.add(testTask);
		maybeStartWorker();
		return entry;
	}

	private void forkAll(Collection<WorkQueue.Entry> entries) {
		if (entries.isEmpty()) {
			return;
		}
		workQueue.addAll(entries);
		// start at most (parallelism - 1) new workers as this method is called from a worker thread holding a lease
		for (int i = 0; i < Math.min(parallelism - 1, entries.size()); i++) {
			maybeStartWorker();
		}
	}

	private void maybeStartWorker() {
		maybeStartWorker(() -> false);
	}

	private void maybeStartWorker(BooleanSupplier doneCondition) {
		if (threadPool.isShutdown() || workQueue.isEmpty() || doneCondition.getAsBoolean()) {
			return;
		}
		var workerLease = workerLeaseManager.tryAcquire();
		if (workerLease == null) {
			return;
		}
		try {
			threadPool.execute(() -> {
				LOGGER.trace(() -> "starting worker");
				try {
					WorkerThread.getOrThrow().processQueueEntries(workerLease, doneCondition);
				}
				finally {
					workerLease.release(false);
					LOGGER.trace(() -> "stopping worker");
				}
				maybeStartWorker(doneCondition);
			});
		}
		catch (RejectedExecutionException e) {
			workerLease.release(false);
			if (threadPool.isShutdown() || workerLeaseManager.isAtLeastOneLeaseTaken()) {
				return;
			}
			LOGGER.error(e, () -> "failed to submit worker to thread pool");
			throw e;
		}
	}

	private class WorkerThreadFactory implements ThreadFactory {

		private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final int poolNumber;
		private final ClassLoader classLoader;

		WorkerThreadFactory(ClassLoader classLoader) {
			this.classLoader = classLoader;
			this.poolNumber = POOL_NUMBER.getAndIncrement();
		}

		@Override
		public Thread newThread(Runnable runnable) {
			var thread = new WorkerThread(runnable,
				"junit-%d-worker-%d".formatted(poolNumber, threadNumber.getAndIncrement()));
			thread.setContextClassLoader(classLoader);
			return thread;
		}
	}

	private class WorkerThread extends Thread {

		@Nullable
		WorkerLease workerLease;

		WorkerThread(Runnable runnable, String name) {
			super(runnable, name);
		}

		static @Nullable WorkerThread get() {
			if (Thread.currentThread() instanceof WorkerThread workerThread) {
				return workerThread;
			}
			return null;
		}

		static WorkerThread getOrThrow() {
			var workerThread = get();
			if (workerThread == null) {
				throw new IllegalStateException("Not on a worker thread");
			}
			return workerThread;
		}

		ConcurrentHierarchicalTestExecutorService executor() {
			return ConcurrentHierarchicalTestExecutorService.this;
		}

		void processQueueEntries(WorkerLease workerLease, BooleanSupplier doneCondition) {
			this.workerLease = workerLease;
			while (!threadPool.isShutdown()) {
				if (doneCondition.getAsBoolean()) {
					LOGGER.trace(() -> "yielding resource lock");
					break;
				}
				var entry = workQueue.poll();
				if (entry == null) {
					LOGGER.trace(() -> "no queue entry available");
					break;
				}
				LOGGER.trace(() -> "processing: " + entry.task);
				execute(entry);
			}
		}

		<T> T runBlocking(BooleanSupplier doneCondition, BlockingAction<T> blockingAction) throws InterruptedException {
			var workerLease = requireNonNull(this.workerLease);
			workerLease.release(doneCondition);
			try {
				return blockingAction.run();
			}
			finally {
				try {
					workerLease.reacquire();
				}
				catch (InterruptedException e) {
					interrupt();
				}
			}
		}

		void invokeAll(List<? extends TestTask> testTasks) {

			if (testTasks.isEmpty()) {
				return;
			}

			if (testTasks.size() == 1) {
				executeTask(testTasks.get(0));
				return;
			}

			List<TestTask> isolatedTasks = new ArrayList<>(testTasks.size());
			List<TestTask> sameThreadTasks = new ArrayList<>(testTasks.size());
			var forkedChildren = forkConcurrentChildren(testTasks, isolatedTasks::add, sameThreadTasks);
			executeAll(sameThreadTasks);
			var queueEntriesByResult = tryToStealWorkWithoutBlocking(forkedChildren);
			tryToStealWorkWithBlocking(queueEntriesByResult);
			waitFor(queueEntriesByResult);
			executeAll(isolatedTasks);
		}

		private List<WorkQueue.Entry> forkConcurrentChildren(List<? extends TestTask> children,
				Consumer<TestTask> isolatedTaskCollector, List<TestTask> sameThreadTasks) {

			List<WorkQueue.Entry> queueEntries = new ArrayList<>(children.size());
			for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
				TestTask child = children.get(i);
				if (requiresGlobalReadWriteLock(child)) {
					isolatedTaskCollector.accept(child);
				}
				else if (child.getExecutionMode() == SAME_THREAD) {
					sameThreadTasks.add(child);
				}
				else {
					queueEntries.add(WorkQueue.Entry.createIndexed(i, child));
				}
			}

			if (!queueEntries.isEmpty()) {
				if (sameThreadTasks.isEmpty()) {
					// hold back one task for this thread
					var lastEntry = queueEntries.stream().max(naturalOrder()).orElseThrow();
					queueEntries.remove(lastEntry);
					sameThreadTasks.add(lastEntry.task);
				}
				forkAll(queueEntries);
			}

			return queueEntries;
		}

		private Map<WorkStealResult, List<WorkQueue.Entry>> tryToStealWorkWithoutBlocking(
				List<WorkQueue.Entry> forkedChildren) {

			Map<WorkStealResult, List<WorkQueue.Entry>> queueEntriesByResult = new EnumMap<>(WorkStealResult.class);
			if (!forkedChildren.isEmpty()) {
				forkedChildren.sort(reverseOrder());
				tryToStealWork(forkedChildren, BlockingMode.NON_BLOCKING, queueEntriesByResult);
			}
			return queueEntriesByResult;
		}

		private void tryToStealWorkWithBlocking(Map<WorkStealResult, List<WorkQueue.Entry>> queueEntriesByResult) {
			var childrenRequiringResourceLocks = queueEntriesByResult.remove(WorkStealResult.RESOURCE_LOCK_UNAVAILABLE);
			if (childrenRequiringResourceLocks == null) {
				return;
			}
			tryToStealWork(childrenRequiringResourceLocks, BlockingMode.BLOCKING, queueEntriesByResult);
		}

		private void tryToStealWork(List<WorkQueue.Entry> children, BlockingMode blocking,
				Map<WorkStealResult, List<WorkQueue.Entry>> queueEntriesByResult) {
			for (var entry : children) {
				var state = tryToStealWork(entry, blocking);
				queueEntriesByResult.computeIfAbsent(state, __ -> new ArrayList<>()).add(entry);
			}
		}

		private WorkStealResult tryToStealWork(WorkQueue.Entry entry, BlockingMode blockingMode) {
			if (entry.future.isDone()) {
				return WorkStealResult.EXECUTED_BY_DIFFERENT_WORKER;
			}
			var claimed = workQueue.remove(entry);
			if (claimed) {
				LOGGER.trace(() -> "stole work: " + entry);
				var executed = executeStolenWork(entry, blockingMode);
				if (executed) {
					return WorkStealResult.EXECUTED_BY_THIS_WORKER;
				}
				workQueue.reAdd(entry);
				return WorkStealResult.RESOURCE_LOCK_UNAVAILABLE;
			}
			return WorkStealResult.EXECUTED_BY_DIFFERENT_WORKER;
		}

		private void waitFor(Map<WorkStealResult, List<WorkQueue.Entry>> queueEntriesByResult) {
			var children = queueEntriesByResult.get(WorkStealResult.EXECUTED_BY_DIFFERENT_WORKER);
			if (children == null) {
				return;
			}
			var future = toCombinedFuture(children);
			try {
				if (future.isDone()) {
					// no need to release worker lease
					future.join();
				}
				else {
					runBlocking(future::isDone, () -> {
						LOGGER.trace(() -> "blocking for forked children : %s".formatted(children));
						return future.join();
					});
				}
			}
			catch (InterruptedException e) {
				currentThread().interrupt();
			}
		}

		private static boolean requiresGlobalReadWriteLock(TestTask testTask) {
			return testTask.getResourceLock().getResources().contains(GLOBAL_READ_WRITE);
		}

		private void executeAll(List<? extends TestTask> children) {
			if (children.isEmpty()) {
				return;
			}
			LOGGER.trace(() -> "running %d children directly".formatted(children.size()));
			if (children.size() == 1) {
				executeTask(children.get(0));
				return;
			}
			for (var testTask : children) {
				executeTask(testTask);
			}
		}

		private boolean executeStolenWork(WorkQueue.Entry entry, BlockingMode blockingMode) {
			return switch (blockingMode) {
				case NON_BLOCKING -> tryExecute(entry);
				case BLOCKING -> {
					execute(entry);
					yield true;
				}
			};
		}

		private boolean tryExecute(WorkQueue.Entry entry) {
			try {
				var executed = tryExecuteTask(entry.task);
				if (executed) {
					entry.future.complete(null);
				}
				return executed;
			}
			catch (Throwable t) {
				entry.future.completeExceptionally(t);
				return true;
			}
		}

		private void execute(WorkQueue.Entry entry) {
			try {
				executeTask(entry.task);
			}
			catch (Throwable t) {
				entry.future.completeExceptionally(t);
			}
			finally {
				entry.future.complete(null);
			}
		}

		@SuppressWarnings("try")
		private void executeTask(TestTask testTask) {
			var executed = tryExecuteTask(testTask);
			if (!executed) {
				var resourceLock = testTask.getResourceLock();
				try (var ignored = runBlocking(() -> false, () -> {
					LOGGER.trace(() -> "blocking for resource lock: " + resourceLock);
					return resourceLock.acquire();
				})) {
					LOGGER.trace(() -> "acquired resource lock: " + resourceLock);
					doExecute(testTask);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				finally {
					LOGGER.trace(() -> "released resource lock: " + resourceLock);
				}
			}
		}

		private boolean tryExecuteTask(TestTask testTask) {
			var resourceLock = testTask.getResourceLock();
			if (resourceLock.tryAcquire()) {
				LOGGER.trace(() -> "acquired resource lock: " + resourceLock);
				try (resourceLock) {
					doExecute(testTask);
					return true;
				}
				finally {
					LOGGER.trace(() -> "released resource lock: " + resourceLock);
				}
			}
			else {
				LOGGER.trace(() -> "failed to acquire resource lock: " + resourceLock);
			}
			return false;
		}

		private void doExecute(TestTask testTask) {
			LOGGER.trace(() -> "executing: " + testTask);
			try {
				testTask.execute();
			}
			finally {
				LOGGER.trace(() -> "finished executing: " + testTask);
			}
		}

		private static CompletableFuture<?> toCombinedFuture(List<WorkQueue.Entry> entries) {
			if (entries.size() == 1) {
				return entries.get(0).future();
			}
			var futures = entries.stream().map(WorkQueue.Entry::future).toArray(CompletableFuture<?>[]::new);
			return CompletableFuture.allOf(futures);
		}

		private enum WorkStealResult {
			EXECUTED_BY_DIFFERENT_WORKER, RESOURCE_LOCK_UNAVAILABLE, EXECUTED_BY_THIS_WORKER
		}

		private interface BlockingAction<T> {
			T run() throws InterruptedException;
		}

	}

	private static class WorkStealingFuture extends BlockingAwareFuture<@Nullable Void> {

		private final WorkQueue.Entry entry;

		WorkStealingFuture(WorkQueue.Entry entry) {
			super(entry.future);
			this.entry = entry;
		}

		@Override
		protected @Nullable Void handle(Callable<@Nullable Void> callable) throws Exception {
			var workerThread = WorkerThread.get();
			if (workerThread == null || entry.future.isDone()) {
				return callable.call();
			}
			workerThread.tryToStealWork(entry, BlockingMode.BLOCKING);
			if (entry.future.isDone()) {
				return callable.call();
			}
			// TODO steal other dynamic children until future is done and check again before blocking
			LOGGER.trace(() -> "blocking for child task");
			return workerThread.runBlocking(entry.future::isDone, () -> {
				try {
					return callable.call();
				}
				catch (Exception ex) {
					throw throwAsUncheckedException(ex);
				}
			});
		}

	}

	private enum BlockingMode {
		NON_BLOCKING, BLOCKING
	}

	private static class WorkQueue {

		private final EntryOrdering ordering = new EntryOrdering();
		private final Queue<Entry> queue = new PriorityBlockingQueue<>();

		Entry add(TestTask task) {
			Entry entry = Entry.create(task);
			LOGGER.trace(() -> "forking: " + entry.task);
			return doAdd(entry);
		}

		void addAll(Collection<Entry> entries) {
			entries.forEach(this::doAdd);
		}

		void reAdd(Entry entry) {
			LOGGER.trace(() -> "re-enqueuing: " + entry.task);
			ordering.incrementAttempts(entry);
			doAdd(entry);
		}

		private Entry doAdd(Entry entry) {
			var added = queue.add(entry);
			if (!added) {
				throw new IllegalStateException("Could not add entry to the queue for task: " + entry.task);
			}
			return entry;
		}

		@Nullable
		Entry poll() {
			return queue.poll();
		}

		boolean remove(Entry entry) {
			return queue.remove(entry);
		}

		boolean isEmpty() {
			return queue.isEmpty();
		}

		private record Entry(TestTask task, CompletableFuture<@Nullable Void> future, int level, int index)
				implements Comparable<Entry> {

			static Entry create(TestTask task) {
				int level = task.getTestDescriptor().getUniqueId().getSegments().size();
				return new Entry(task, new CompletableFuture<>(), level, 0);
			}

			static Entry createIndexed(int index, TestTask task) {
				int level = task.getTestDescriptor().getUniqueId().getSegments().size();
				return new Entry(task, new CompletableFuture<>(), level, index);
			}

			@SuppressWarnings("FutureReturnValueIgnored")
			Entry {
				future.whenComplete((__, t) -> {
					if (t == null) {
						LOGGER.trace(() -> "completed normally: " + this.task());
					}
					else {
						LOGGER.trace(t, () -> "completed exceptionally: " + this.task());
					}
				});
			}

			@Override
			public int compareTo(Entry that) {
				var result = Integer.compare(that.level, this.level);
				if (result != 0) {
					return result;
				}
				result = Boolean.compare(this.isContainer(), that.isContainer());
				if (result != 0) {
					return result;
				}
				return Integer.compare(that.index, this.index);
			}

			private boolean isContainer() {
				return task.getTestDescriptor().isContainer();
			}

		}

		static class EntryOrdering implements Comparator<Entry> {

			private final ConcurrentMap<UniqueId, Integer> attempts = new ConcurrentHashMap<>();

			@Override
			public int compare(Entry a, Entry b) {
				var result = a.compareTo(b);
				if (result == 0) {
					result = Integer.compare(attempts(b), attempts(a));
				}
				return result;
			}

			void incrementAttempts(Entry entry) {
				attempts.compute(key(entry), (key, n) -> {
					if (n == null) {
						registerForKeyRemoval(entry, key);
						return 1;
					}
					return n + 1;
				});
			}

			private int attempts(Entry entry) {
				return attempts.getOrDefault(key(entry), 0);
			}

			@SuppressWarnings("FutureReturnValueIgnored")
			private void registerForKeyRemoval(Entry entry, UniqueId key) {
				entry.future.whenComplete((__, ___) -> attempts.remove(key));
			}

			private static UniqueId key(Entry entry) {
				return entry.task.getTestDescriptor().getUniqueId();
			}
		}

	}

	static class WorkerLeaseManager {

		private final int parallelism;
		private final Semaphore semaphore;
		private final Consumer<BooleanSupplier> compensation;

		WorkerLeaseManager(int parallelism, Consumer<BooleanSupplier> compensation) {
			this.parallelism = parallelism;
			this.semaphore = new Semaphore(parallelism);
			this.compensation = compensation;
		}

		@Nullable
		WorkerLease tryAcquire() {
			boolean acquired = semaphore.tryAcquire();
			if (acquired) {
				LOGGER.trace(() -> "acquired worker lease for new worker (available: %d)".formatted(
					semaphore.availablePermits()));
				return new WorkerLease(this::release);
			}
			return null;
		}

		private ReacquisitionToken release(boolean compensate, BooleanSupplier doneCondition) {
			semaphore.release();
			LOGGER.trace(() -> "release worker lease (available: %d)".formatted(semaphore.availablePermits()));
			if (compensate) {
				compensation.accept(doneCondition);
			}
			return new ReacquisitionToken();
		}

		public boolean isAtLeastOneLeaseTaken() {
			return semaphore.availablePermits() < parallelism;
		}

		private class ReacquisitionToken {

			private boolean used = false;

			void reacquire() throws InterruptedException {
				Preconditions.condition(!used, "Lease was already reacquired");
				used = true;
				semaphore.acquire();
				LOGGER.trace(() -> "reacquired worker lease (available: %d)".formatted(semaphore.availablePermits()));
			}
		}
	}

	static class WorkerLease implements AutoCloseable {

		private final BiFunction<Boolean, BooleanSupplier, WorkerLeaseManager.ReacquisitionToken> releaseAction;
		private WorkerLeaseManager.@Nullable ReacquisitionToken reacquisitionToken;

		WorkerLease(BiFunction<Boolean, BooleanSupplier, WorkerLeaseManager.ReacquisitionToken> releaseAction) {
			this.releaseAction = releaseAction;
		}

		@Override
		public void close() {
			release(true);
		}

		public void release(BooleanSupplier doneCondition) {
			release(true, doneCondition);
		}

		void release(boolean compensate) {
			release(compensate, () -> false);
		}

		void release(boolean compensate, BooleanSupplier doneCondition) {
			if (reacquisitionToken == null) {
				reacquisitionToken = releaseAction.apply(compensate, doneCondition);
			}
		}

		void reacquire() throws InterruptedException {
			Preconditions.notNull(reacquisitionToken, "Cannot reacquire an unreleased WorkerLease");
			reacquisitionToken.reacquire();
			reacquisitionToken = null;
		}
	}
}
