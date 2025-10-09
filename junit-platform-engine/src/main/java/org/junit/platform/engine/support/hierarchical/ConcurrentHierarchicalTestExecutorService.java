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

import static java.util.stream.Collectors.groupingBy;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
	private final ExecutorService executorService;

	public ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration) {
		this(configuration, ClassLoaderUtils.getDefaultClassLoader());
	}

	ConcurrentHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration, ClassLoader classLoader) {
		executorService = Executors.newFixedThreadPool(configuration.getParallelism(),
			new CustomThreadFactory(classLoader));
		for (var i = 0; i < configuration.getParallelism(); i++) {
			startWorker();
		}
	}

	@Override
	public Future<@Nullable Void> submit(TestTask testTask) {
		return enqueue(testTask).completion.thenApply(__ -> null);
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
		}
	}

	private WorkQueue.Entry enqueue(TestTask testTask) {
		// TODO check if worker needs to be started
		startWorker();
		return workQueue.add(testTask);
	}

	private void startWorker() {
		executorService.execute(() -> {
			while (!executorService.isShutdown()) {
				try {
					// TODO get worker lease
					var entry = workQueue.poll(30, TimeUnit.SECONDS);
					if (entry == null) {
						// TODO give up worker lease
						// nothing to do -> exiting
						return;
					}
					entry.execute();
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
				entry.execute();
			}
			else {
				futures.add(entry.completion);
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

	private static void executeTask(TestTask testTask) {
		testTask.execute();
	}

	@Override
	public void close() {
		executorService.shutdown();
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

		private final BlockingQueue<Entry> queue = new ArrayBlockingQueue<>(1024);

		Entry add(TestTask task) {
			var entry = new Entry(task, new CompletableFuture<>());
			queue.add(entry);
			return entry;
		}

		@Nullable
		Entry poll(long timeout, TimeUnit unit) throws InterruptedException {
			return queue.poll(timeout, unit);
		}

		boolean remove(Entry entry) {
			return queue.remove(entry);
		}

		private record Entry(TestTask task, CompletableFuture<?> completion) {
			void execute() {
				try {
					executeTask(task);
					completion.complete(null);
				}
				catch (Throwable t) {
					completion.completeExceptionally(t);
				}
			}
		}

	}

}
