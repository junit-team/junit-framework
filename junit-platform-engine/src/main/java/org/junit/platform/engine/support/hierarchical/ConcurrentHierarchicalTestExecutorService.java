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

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.groupingBy;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 6.1
 */
@API(status = EXPERIMENTAL, since = "6.1")
public class ConcurrentHierarchicalTestExecutorService implements HierarchicalTestExecutorService {

	private final WorkQueue workQueue = new WorkQueue();
	private final ExecutorService threadPool;

	public ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration) {
		this(configuration, ClassLoaderUtils.getDefaultClassLoader());
	}

	ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration, ClassLoader classLoader) {
		ThreadFactory threadFactory = new CustomThreadFactory(classLoader);
		threadPool = new ThreadPoolExecutor(configuration.getCorePoolSize(), configuration.getMaxPoolSize(),
			configuration.getKeepAliveSeconds(), SECONDS, new SynchronousQueue<>(), threadFactory);
	}

	@Override
	public Future<@Nullable Void> submit(TestTask testTask) {
		return enqueue(testTask).future();
	}

	@Override
	public void invokeAll(List<? extends TestTask> testTasks) {

		Preconditions.condition(WorkerThread.getExecutor() == this,
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
		var concurrentlyExecutedChildren = stealWork(queueEntries);
		if (!concurrentlyExecutedChildren.isEmpty()) {
			// TODO give up worker lease
			toCompletableFuture(concurrentlyExecutedChildren).join();
			// TODO get worker lease
		}
	}

	private WorkQueue.Entry enqueue(TestTask testTask) {
		// TODO check if worker needs to be started
		var entry = workQueue.add(testTask);
		startWorker();
		return entry;
	}

	private void startWorker() {
		threadPool.execute(() -> {
			while (!threadPool.isShutdown()) {
				try {
					// TODO get worker lease
					var entry = workQueue.poll();
					if (entry == null) {
						// TODO give up worker lease
						// nothing to do -> done
						break;
					}
					executeEntry(entry);
				}
				catch (InterruptedException ignore) {
					// ignore spurious interrupts
				}
			}
		});
	}

	private static CompletableFuture<?> toCompletableFuture(List<CompletableFuture<?>> futures) {
		if (futures.size() == 1) {
			return futures.get(0);
		}
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	private List<CompletableFuture<?>> stealWork(List<WorkQueue.Entry> queueEntries) {
		if (queueEntries.isEmpty()) {
			return List.of();
		}
		List<CompletableFuture<?>> futures = new ArrayList<>(queueEntries.size());
		var iterator = queueEntries.listIterator(queueEntries.size());
		for (var entry = iterator.previous(); iterator.hasPrevious(); entry = iterator.previous()) {
			var claimed = workQueue.remove(entry);
			if (claimed) {
				var executed = entry.tryExecute();
				if (!executed) {
					workQueue.add(entry);
					futures.add(entry.future);
				}
			}
			else {
				futures.add(entry.future);
			}
		}
		return futures;
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
		if (children.size() == 1) {
			executeTask(children.get(0));
			return;
		}
		for (var testTask : children) {
			executeTask(testTask);
		}
	}

	@Override
	public void close() {
		threadPool.shutdownNow();
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

		WorkerThread(Runnable runnable, String name) {
			super(runnable, name);
		}

		static @Nullable ConcurrentHierarchicalTestExecutorService getExecutor() {
			return Thread.currentThread() instanceof WorkerThread c ? c.executor() : null;
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

		private record Entry(TestTask task, CompletableFuture<@Nullable Void> future) {
			boolean tryExecute() {
				try {
					var executed = tryExecuteTask(task);
					if (executed) {
						future.complete(null);
					}
					return executed;
				}
				catch (Throwable t) {
					future.completeExceptionally(t);
					return true;
				}
			}
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
			// TODO start another worker to compensate?
			try (var ignored = testTask.getResourceLock().acquire()) {
				testTask.execute();
			}
			catch (InterruptedException ex) {
				throw throwAsUncheckedException(ex);
			}
		}
	}

	private static boolean tryExecuteTask(TestTask testTask) {
		var resourceLock = testTask.getResourceLock();
		if (resourceLock.tryAcquire()) {
			try (resourceLock) {
				testTask.execute();
				return true;
			}
		}
		return false;
	}

}
