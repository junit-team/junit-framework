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

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.groupingBy;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 6.1
 */
@API(status = EXPERIMENTAL, since = "6.1")
public class ConcurrentHierarchicalTestExecutorService implements HierarchicalTestExecutorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentHierarchicalTestExecutorService.class);

	private final WorkQueue workQueue = new WorkQueue();
	private final ExecutorService threadPool;
	private final WorkerLeaseManager workerLeaseManager;

	public ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration) {
		this(configuration, ClassLoaderUtils.getDefaultClassLoader());
	}

	ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration, ClassLoader classLoader) {
		ThreadFactory threadFactory = new CustomThreadFactory(classLoader);
		threadPool = new ThreadPoolExecutor(configuration.getCorePoolSize(), configuration.getMaxPoolSize(),
			configuration.getKeepAliveSeconds(), SECONDS, new SynchronousQueue<>(), threadFactory);
		workerLeaseManager = new WorkerLeaseManager(configuration.getParallelism());
	}

	@Override
	public void close() {
		threadPool.shutdownNow();
	}

	@Override
	public Future<@Nullable Void> submit(TestTask testTask) {
		LOGGER.trace(() -> "submit: " + testTask);
		return enqueue(testTask).future();
	}

	@Override
	public void invokeAll(List<? extends TestTask> testTasks) {
		LOGGER.trace(() -> "invokeAll: " + testTasks);

		var workerThread = WorkerThread.get();
		Preconditions.condition(workerThread != null && workerThread.executor() == this,
			"invokeAll() must not be called from a thread that is not part of this executor");

		if (testTasks.isEmpty()) {
			return;
		}

		if (testTasks.size() == 1) {
			executeTask(testTasks.get(0));
			return;
		}

		var childrenByExecutionMode = testTasks.stream().collect(groupingBy(TestTask::getExecutionMode));
		var queueEntries = forkAll(childrenByExecutionMode.get(CONCURRENT));
		executeAll(childrenByExecutionMode.get(SAME_THREAD));
		var remainingForkedChildren = stealWork(queueEntries);
		waitFor(remainingForkedChildren);
	}

	private static void waitFor(List<WorkQueue.Entry> children) {
		if (children.isEmpty()) {
			return;
		}
		var future = toCombinedFuture(children);
		try {
			if (future.isDone()) {
				// no need to release worker lease
				future.join();
			}
			else {
				WorkerThread.getOrThrow().runBlocking(() -> {
					LOGGER.trace(() -> "blocking for forked children: " + children);
					return future.join();
				});
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private WorkQueue.Entry enqueue(TestTask testTask) {
		var entry = workQueue.add(testTask);
		maybeStartWorker();
		return entry;
	}

	private void maybeStartWorker() {
		if (threadPool.isShutdown() || !workerLeaseManager.isLeaseAvailable() || workQueue.isEmpty()) {
			return;
		}
		try {
			threadPool.execute(() -> WorkerThread.getOrThrow().processQueueEntries());
		}
		catch (RejectedExecutionException e) {
			if (threadPool.isShutdown()) {
				return;
			}
			throw e;
		}
	}

	private static CompletableFuture<?> toCombinedFuture(List<WorkQueue.Entry> entries) {
		if (entries.size() == 1) {
			return entries.get(0).future();
		}
		var futures = entries.stream().map(WorkQueue.Entry::future).toArray(CompletableFuture<?>[]::new);
		return CompletableFuture.allOf(futures);
	}

	private List<WorkQueue.Entry> stealWork(List<WorkQueue.Entry> queueEntries) {
		if (queueEntries.isEmpty()) {
			return List.of();
		}
		List<WorkQueue.Entry> concurrentlyExecutedChildren = new ArrayList<>(queueEntries.size());
		var iterator = queueEntries.listIterator(queueEntries.size());
		while (iterator.hasPrevious()) {
			var entry = iterator.previous();
			var claimed = workQueue.remove(entry);
			if (claimed) {
				LOGGER.trace(() -> "stole work: " + entry);
				var executed = tryExecute(entry);
				if (!executed) {
					workQueue.add(entry);
					concurrentlyExecutedChildren.add(entry);
				}
			}
			else {
				concurrentlyExecutedChildren.add(entry);
			}
		}
		return concurrentlyExecutedChildren;
	}

	private List<WorkQueue.Entry> forkAll(@Nullable List<? extends TestTask> children) {
		if (children == null) {
			return List.of();
		}
		if (children.size() == 1) {
			return List.of(enqueue(children.get(0)));
		}
		return children.stream() //
				.map(ConcurrentHierarchicalTestExecutorService.this::enqueue) //
				.toList();
	}

	private void executeAll(@Nullable List<? extends TestTask> children) {
		if (children == null) {
			return;
		}
		LOGGER.trace(() -> "Running SAME_THREAD children: " + children);
		if (children.size() == 1) {
			executeTask(children.get(0));
			return;
		}
		for (var testTask : children) {
			executeTask(testTask);
		}
	}

	private static boolean tryExecute(WorkQueue.Entry entry) {
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

	private void executeEntry(WorkQueue.Entry entry) {
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
			var workerThread = WorkerThread.getOrThrow();
			try (var ignored = workerThread.runBlocking(() -> {
				LOGGER.trace(() -> "blocking for resource lock: " + resourceLock);
				return resourceLock.acquire();
			})) {
				doExecute(testTask);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static boolean tryExecuteTask(TestTask testTask) {
		var resourceLock = testTask.getResourceLock();
		if (resourceLock.tryAcquire()) {
			try (resourceLock) {
				doExecute(testTask);
				return true;
			}
		}
		return false;
	}

	private static void doExecute(TestTask testTask) {
		LOGGER.trace(() -> "executing: " + testTask);
		try {
			testTask.execute();
		}
		finally {
			LOGGER.trace(() -> "finished executing: " + testTask);
		}
	}

	private class CustomThreadFactory implements ThreadFactory {

		private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final int poolNumber;
		private final ClassLoader classLoader;

		CustomThreadFactory(ClassLoader classLoader) {
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

		void processQueueEntries() {
			while (!threadPool.isShutdown()) {
				try {
					var entry = workQueue.poll();
					if (entry == null) {
						break;
					}
					LOGGER.trace(() -> "processing: " + entry);
					workerLease = workerLeaseManager.tryAcquire();
					if (workerLease == null) {
						workQueue.add(entry);
						break;
					}
					try {
						executeEntry(entry);
					}
					finally {
						workerLease.release();
					}
				}
				catch (InterruptedException ignore) {
					// ignore spurious interrupts
				}
			}
		}

		<T> T runBlocking(BlockingAction<T> blockingAction) throws InterruptedException {
			var workerLease = requireNonNull(this.workerLease);
			workerLease.release();
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

		interface BlockingAction<T> {
			T run() throws InterruptedException;
		}

		private ConcurrentHierarchicalTestExecutorService executor() {
			return ConcurrentHierarchicalTestExecutorService.this;
		}

	}

	private static class WorkQueue {

		private final BlockingQueue<Entry> queue = new LinkedBlockingQueue<>();

		Entry add(TestTask task) {
			return add(new Entry(task, new CompletableFuture<>()));
		}

		Entry add(Entry entry) {
			LOGGER.trace(() -> "forking: " + entry);
			var added = queue.add(entry);
			if (!added) {
				throw new IllegalStateException("Could not add entry to the queue for task: " + entry.task);
			}
			return entry;
		}

		@Nullable
		Entry poll() throws InterruptedException {
			return queue.poll(1, SECONDS);
		}

		boolean remove(Entry entry) {
			return queue.remove(entry);
		}

		boolean isEmpty() {
			return queue.isEmpty();
		}

		private record Entry(TestTask task, CompletableFuture<@Nullable Void> future) {
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
		}

	}

	private class WorkerLeaseManager {

		private final Semaphore semaphore;

		WorkerLeaseManager(int parallelism) {
			semaphore = new Semaphore(parallelism);
		}

		@Nullable
		WorkerLease tryAcquire() {
			try {
				boolean acquired = semaphore.tryAcquire(1, SECONDS);
				return acquired ? new WorkerLease(this::release) : null;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}

		private ReacquisitionToken release() {
			LOGGER.trace(() -> "releasing worker lease");
			semaphore.release();
			maybeStartWorker();
			return new ReacquisitionToken();
		}

		boolean isLeaseAvailable() {
			return semaphore.availablePermits() > 0;
		}

		private class ReacquisitionToken {

			private boolean used = false;

			void reacquire() throws InterruptedException {
				Preconditions.condition(!used, "Lease was already reacquired");
				used = true;
				LOGGER.trace(() -> "reacquiring worker lease");
				semaphore.acquire();
			}
		}
	}

	private static class WorkerLease {

		private final Supplier<WorkerLeaseManager.ReacquisitionToken> releaseAction;
		private WorkerLeaseManager.@Nullable ReacquisitionToken reacquisitionToken;

		WorkerLease(Supplier<WorkerLeaseManager.ReacquisitionToken> releaseAction) {
			LOGGER.trace(() -> "acquiring worker lease");
			this.releaseAction = releaseAction;
		}

		void release() {
			if (reacquisitionToken == null) {
				reacquisitionToken = releaseAction.get();
			}
		}

		void reacquire() throws InterruptedException {
			Preconditions.notNull(reacquisitionToken, "Cannot reacquire an unreleased WorkerLease");
			reacquisitionToken.reacquire();
			reacquisitionToken = null;
		}
	}

}
