/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ_WRITE;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
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
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.ParallelHierarchicalTestExecutorServiceFactory.ParallelExecutorServiceType;

/**
 * An {@linkplain HierarchicalTestExecutorService executor service} based on a
 * regular thread pool that executes {@linkplain TestTask test tasks} with the
 * configured parallelism.
 *
 * @since 6.1
 * @see ParallelHierarchicalTestExecutorServiceFactory
 * @see ParallelExecutorServiceType#WORKER_THREAD_POOL
 * @see DefaultParallelExecutionConfigurationStrategy
 */
@API(status = EXPERIMENTAL, since = "6.1")
public final class WorkerThreadPoolHierarchicalTestExecutorService implements HierarchicalTestExecutorService {

	/*
		This implementation is based on a regular thread pool and a work queue shared among all worker threads.

		Each worker thread scans the shared work queue for tasks to run. Since the tasks represent hierarchically
		structured tests, container tasks will call `submit(TestTask)` or `invokeAll(List<TestTask>)` for their
		children, recursively.

		To maintain the desired parallelism -- regardless whether the user code performs any blocking operations --
		a fixed number of worker leases is configured. Whenever a task is submitted to the work queue to be executed
		concurrently, an attempt is made to acquire a worker lease. If a worker lease was acquired, a worker thread is
		started. Each worker thread attempts to "steal" queue entries for its children and execute them itself prior to
		waiting for its children to finish.

		To optimize CPU utilization, whenever a worker thread does need to block, it temporarily gives up its worker
		lease and attempts to start another worker thread to compensate for the reduced `parallelism`. If the max pool
	    size does not permit starting another thread, the attempt is ignored in case there are still other active worker
	    threads.

		The same happens in case a resource lock needs to be acquired.

		To minimize the number of idle workers, worker threads will prefer to steal top level tasks, while working
		through their own task hierarchy in a depth first fashion. Furthermore, child tasks with execution mode
		`CONCURRENT` are submitted to the shared queue prior to executing those with execution mode `SAME_THREAD`
		directly.
	*/

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThreadPoolHierarchicalTestExecutorService.class);

	private final WorkQueue workQueue = new WorkQueue();
	private final ExecutorService threadPool;
	private final int parallelism;
	private final WorkerLeaseManager workerLeaseManager;

	/**
	 * Create a new {@code WorkerThreadPoolHierarchicalTestExecutorService}
	 * based on the supplied {@link ParallelExecutionConfiguration}.
	 *
	 * <p>The following attributes of the supplied configuration are applied to
	 * the thread pool used by this executor service:
	 *
	 * <ul>
	 *     <li>{@link ParallelExecutionConfiguration#getParallelism()}</li>
	 *     <li>{@link ParallelExecutionConfiguration#getCorePoolSize()}</li>
	 *     <li>{@link ParallelExecutionConfiguration#getMaxPoolSize()}</li>
	 *     <li>{@link ParallelExecutionConfiguration#getKeepAliveSeconds()}</li>
	 * </ul>
	 *
	 * <p>The remaining attributes, such as
	 * {@link ParallelExecutionConfiguration#getMinimumRunnable()} and
	 * {@link ParallelExecutionConfiguration#getSaturatePredicate()}, are
	 * ignored.
	 *
	 * @see ParallelHierarchicalTestExecutorServiceFactory#create(ConfigurationParameters)
	 */
	WorkerThreadPoolHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration) {
		this(configuration, ClassLoaderUtils.getDefaultClassLoader());
	}

	// package-private for testing
	WorkerThreadPoolHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration,
			ClassLoader classLoader) {
		ThreadFactory threadFactory = new WorkerThreadFactory(classLoader);
		parallelism = configuration.getParallelism();
		workerLeaseManager = new WorkerLeaseManager(parallelism, this::maybeStartWorker);
		var rejectedExecutionHandler = new LeaseAwareRejectedExecutionHandler(workerLeaseManager);
		threadPool = new ThreadPoolExecutor(configuration.getCorePoolSize(), configuration.getMaxPoolSize(),
			configuration.getKeepAliveSeconds(), SECONDS, new SynchronousQueue<>(), threadFactory,
			rejectedExecutionHandler);
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
			return enqueue(testTask, 0).future();
		}

		if (testTask.getExecutionMode() == SAME_THREAD) {
			workerThread.executeTask(testTask);
			return completedFuture(null);
		}

		var entry = enqueue(testTask, workerThread.nextChildIndex());
		workerThread.trackSubmittedChild(entry);
		return new WorkStealingFuture(entry);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implNote This method must be called from within a worker thread that
	 * belongs to this executor.
	 */
	@Override
	public void invokeAll(List<? extends TestTask> testTasks) {
		LOGGER.trace(() -> "invokeAll: " + testTasks);

		var workerThread = WorkerThread.get();
		Preconditions.condition(workerThread != null && workerThread.executor() == this,
			"invokeAll() must be called from a worker thread that belongs to this executor");

		workerThread.invokeAll(testTasks);
	}

	private WorkQueue.Entry enqueue(TestTask testTask, int index) {
		var entry = workQueue.add(testTask, index);
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
		threadPool.execute(new RunLeaseAwareWorker(workerLease, doneCondition,
			() -> WorkerThread.getOrThrow().processQueueEntries(workerLease, doneCondition),
			() -> this.maybeStartWorker(doneCondition)));
	}

	private record RunLeaseAwareWorker(WorkerLease workerLease, BooleanSupplier parentDoneCondition, Runnable work,
			Runnable onWorkerFinished) implements Runnable {

		@Override
		public void run() {
			LOGGER.trace(() -> "starting worker");
			try {
				work.run();
			}
			finally {
				workerLease.release(false);
				LOGGER.trace(() -> "stopping worker");
			}
			onWorkerFinished.run();
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

		private final Deque<State> stateStack = new ArrayDeque<>();

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

		WorkerThreadPoolHierarchicalTestExecutorService executor() {
			return WorkerThreadPoolHierarchicalTestExecutorService.this;
		}

		void processQueueEntries(WorkerLease workerLease, BooleanSupplier doneCondition) {
			this.workerLease = workerLease;
			while (!threadPool.isShutdown()) {
				if (doneCondition.getAsBoolean()) {
					LOGGER.trace(() -> "yielding resource lock");
					break;
				}
				if (workQueue.isEmpty()) {
					LOGGER.trace(() -> "no queue entries available");
					break;
				}
				processQueueEntries();
			}
		}

		private void processQueueEntries() {
			var entriesRequiringResourceLocks = new ArrayList<WorkQueue.Entry>();

			for (var entry : workQueue) {
				var result = tryToStealWork(entry, BlockingMode.NON_BLOCKING);
				if (result == WorkStealResult.EXECUTED_BY_THIS_WORKER) {
					// After executing a test a significant amount of time has passed.
					// Process the queue from the beginning
					return;
				}
				if (result == WorkStealResult.RESOURCE_LOCK_UNAVAILABLE) {
					entriesRequiringResourceLocks.add(entry);
				}
			}

			for (var entry : entriesRequiringResourceLocks) {
				var result = tryToStealWork(entry, BlockingMode.BLOCKING);
				if (result == WorkStealResult.EXECUTED_BY_THIS_WORKER) {
					return;
				}
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
			var queueEntries = forkConcurrentChildren(testTasks, isolatedTasks::add, sameThreadTasks);
			executeAll(sameThreadTasks);
			var queueEntriesByResult = tryToStealWorkWithoutBlocking(queueEntries);
			tryToStealWorkWithBlocking(queueEntriesByResult);
			waitFor(queueEntriesByResult);
			executeAll(isolatedTasks);
		}

		private List<WorkQueue.Entry> forkConcurrentChildren(List<? extends TestTask> children,
				Consumer<TestTask> isolatedTaskCollector, List<TestTask> sameThreadTasks) {

			List<WorkQueue.Entry> queueEntries = new ArrayList<>(children.size());
			for (TestTask child : children) {
				if (requiresGlobalReadWriteLock(child)) {
					isolatedTaskCollector.accept(child);
				}
				else if (child.getExecutionMode() == SAME_THREAD) {
					sameThreadTasks.add(child);
				}
				else {
					queueEntries.add(new WorkQueue.Entry(child, nextChildIndex()));
				}
			}

			if (!queueEntries.isEmpty()) {
				queueEntries.sort(WorkQueue.Entry.CHILD_COMPARATOR);
				if (sameThreadTasks.isEmpty()) {
					// hold back one task for this thread
					var firstEntry = queueEntries.remove(0);
					sameThreadTasks.add(firstEntry.task);
				}
				forkAll(queueEntries);
			}
			return queueEntries;
		}

		private Map<WorkStealResult, List<WorkQueue.Entry>> tryToStealWorkWithoutBlocking(
				Iterable<WorkQueue.Entry> queueEntries) {

			Map<WorkStealResult, List<WorkQueue.Entry>> queueEntriesByResult = new EnumMap<>(WorkStealResult.class);
			tryToStealWork(queueEntries, BlockingMode.NON_BLOCKING, queueEntriesByResult);
			return queueEntriesByResult;
		}

		private void tryToStealWorkWithBlocking(Map<WorkStealResult, List<WorkQueue.Entry>> queueEntriesByResult) {
			var entriesRequiringResourceLocks = queueEntriesByResult.remove(WorkStealResult.RESOURCE_LOCK_UNAVAILABLE);
			if (entriesRequiringResourceLocks == null) {
				return;
			}
			tryToStealWork(entriesRequiringResourceLocks, BlockingMode.BLOCKING, queueEntriesByResult);
		}

		private void tryToStealWork(Iterable<WorkQueue.Entry> entries, BlockingMode blocking,
				Map<WorkStealResult, List<WorkQueue.Entry>> queueEntriesByResult) {
			for (var entry : entries) {
				var result = tryToStealWork(entry, blocking);
				queueEntriesByResult.computeIfAbsent(result, __ -> new ArrayList<>()).add(entry);
			}
		}

		private WorkStealResult tryToStealWork(WorkQueue.Entry entry, BlockingMode blockingMode) {
			if (entry.future.isDone()) {
				return WorkStealResult.EXECUTED_BY_DIFFERENT_WORKER;
			}
			var claimed = workQueue.remove(entry);
			if (claimed) {
				LOGGER.trace(() -> "stole work: " + entry.task);
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
			stateStack.push(new State());
			try {
				testTask.execute();
			}
			finally {
				stateStack.pop();
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

		private int nextChildIndex() {
			return stateStack.element().nextChildIndex();
		}

		private void trackSubmittedChild(WorkQueue.Entry entry) {
			stateStack.element().trackSubmittedChild(entry);
		}

		private void tryToStealWorkFromSubmittedChildren() {
			var currentState = stateStack.element();
			var currentSubmittedChildren = currentState.submittedChildren;
			if (currentSubmittedChildren == null || currentSubmittedChildren.isEmpty()) {
				return;
			}
			currentSubmittedChildren.sort(WorkQueue.Entry.CHILD_COMPARATOR);
			var iterator = currentSubmittedChildren.iterator();
			while (iterator.hasNext()) {
				WorkQueue.Entry entry = iterator.next();
				var result = tryToStealWork(entry, BlockingMode.NON_BLOCKING);
				if (result.isExecuted()) {
					iterator.remove();
				}
			}
			currentState.clearIfEmpty();
		}

		private static class State {

			private int nextChildIndex = 0;

			@Nullable
			private List<WorkQueue.Entry> submittedChildren;

			private void trackSubmittedChild(WorkQueue.Entry entry) {
				if (submittedChildren == null) {
					submittedChildren = new ArrayList<>();
				}
				submittedChildren.add(entry);
			}

			private void clearIfEmpty() {
				if (submittedChildren != null && submittedChildren.isEmpty()) {
					submittedChildren = null;
				}
			}

			private int nextChildIndex() {
				return nextChildIndex++;
			}
		}

		private enum WorkStealResult {
			EXECUTED_BY_DIFFERENT_WORKER, RESOURCE_LOCK_UNAVAILABLE, EXECUTED_BY_THIS_WORKER;

			private boolean isExecuted() {
				return this != RESOURCE_LOCK_UNAVAILABLE;
			}
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
			workerThread.tryToStealWorkFromSubmittedChildren();
			if (entry.future.isDone()) {
				return callable.call();
			}
			LOGGER.trace(() -> "blocking for child task: " + entry.task);
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

	private static class WorkQueue implements Iterable<WorkQueue.Entry> {

		private final Set<Entry> queue = new ConcurrentSkipListSet<>(Entry.QUEUE_COMPARATOR);

		Entry add(TestTask task, int index) {
			Entry entry = new Entry(task, index);
			LOGGER.trace(() -> "forking: " + entry.task);
			return doAdd(entry);
		}

		void addAll(Collection<Entry> entries) {
			entries.forEach(this::doAdd);
		}

		void reAdd(Entry entry) {
			LOGGER.trace(() -> "re-enqueuing: " + entry.task);
			doAdd(entry);
		}

		private Entry doAdd(Entry entry) {
			var added = queue.add(entry);
			if (!added) {
				throw new IllegalStateException("Could not add entry to the queue for task: " + entry.task);
			}
			return entry;
		}

		boolean remove(Entry entry) {
			return queue.remove(entry);
		}

		boolean isEmpty() {
			return queue.isEmpty();
		}

		@Override
		public Iterator<Entry> iterator() {
			return queue.iterator();
		}

		private static final class Entry {

			private static final Comparator<Entry> QUEUE_COMPARATOR = comparing(Entry::level).reversed() //
					.thenComparing(Entry::isContainer) // tests before containers
					.thenComparing(Entry::index) //
					.thenComparing(Entry::uniqueId, new SameLengthUniqueIdComparator());

			private static final Comparator<Entry> CHILD_COMPARATOR = comparing(Entry::isContainer).reversed() // containers before tests
					.thenComparing(Entry::index);

			private final TestTask task;
			private final CompletableFuture<@Nullable Void> future;
			private final int index;

			@SuppressWarnings("FutureReturnValueIgnored")
			Entry(TestTask task, int index) {
				this.future = new CompletableFuture<>();
				this.future.whenComplete((__, t) -> {
					if (t == null) {
						LOGGER.trace(() -> "completed normally: " + task);
					}
					else {
						LOGGER.trace(t, () -> "completed exceptionally: " + task);
					}
				});
				this.task = task;
				this.index = index;
			}

			private int index() {
				return this.index;
			}

			private int level() {
				return uniqueId().getSegments().size();
			}

			private boolean isContainer() {
				return task.getTestDescriptor().isContainer();
			}

			private UniqueId uniqueId() {
				return task.getTestDescriptor().getUniqueId();
			}

			CompletableFuture<@Nullable Void> future() {
				return future;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj == this) {
					return true;
				}
				if (obj == null || obj.getClass() != this.getClass()) {
					return false;
				}
				var that = (Entry) obj;
				return Objects.equals(this.uniqueId(), that.uniqueId()) && this.index == that.index;
			}

			@Override
			public int hashCode() {
				return Objects.hash(uniqueId(), index);
			}

			@Override
			public String toString() {
				return new ToStringBuilder(this) //
						.append("task", task) //
						.append("index", index) //
						.toString();
			}

			private static class SameLengthUniqueIdComparator implements Comparator<UniqueId> {

				@Override
				public int compare(UniqueId a, UniqueId b) {
					var aIterator = a.getSegments().iterator();
					var bIterator = b.getSegments().iterator();

					// ids have the same length
					while (aIterator.hasNext()) {
						var aCurrent = aIterator.next();
						var bCurrent = bIterator.next();
						int result = compareBy(aCurrent, bCurrent);
						if (result != 0) {
							return result;
						}
					}
					return 0;
				}

				private static int compareBy(UniqueId.Segment a, UniqueId.Segment b) {
					int result = a.getType().compareTo(b.getType());
					if (result != 0) {
						return result;
					}
					return a.getValue().compareTo(b.getValue());
				}
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

		@Override
		public String toString() {
			return new ToStringBuilder(this) //
					.append("parallelism", parallelism) //
					.append("semaphore", semaphore) //
					.toString();
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

	private record LeaseAwareRejectedExecutionHandler(WorkerLeaseManager workerLeaseManager)
			implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			if (!(r instanceof RunLeaseAwareWorker worker)) {
				return;
			}
			worker.workerLease.release(false);
			if (executor.isShutdown() || workerLeaseManager.isAtLeastOneLeaseTaken()
					|| worker.parentDoneCondition.getAsBoolean()) {
				return;
			}
			throw new RejectedExecutionException("Task with " + workerLeaseManager + " rejected from " + executor);
		}
	}
}
