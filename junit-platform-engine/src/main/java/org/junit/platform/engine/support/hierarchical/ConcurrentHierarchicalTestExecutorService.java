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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.groupingBy;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
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

	private final ExecutorService executorService;

	public ConcurrentHierarchicalTestExecutorService() {
		this(ClassLoaderUtils.getDefaultClassLoader());
	}

	ConcurrentHierarchicalTestExecutorService(ClassLoader classLoader) {
		executorService = Executors.newCachedThreadPool(new CustomThreadFactory(classLoader));
	}

	@Override
	public Future<@Nullable Void> submit(TestTask testTask) {
		return submitInternal(testTask);
	}

	@Override
	public void invokeAll(List<? extends TestTask> testTasks) {

		Preconditions.condition(CustomThread.getExecutor() == this,
			"invokeAll() must not be called from a thread that is not part of this executor");

		var childrenByExecutionMode = testTasks.stream().collect(groupingBy(TestTask::getExecutionMode));
		var concurrentChildren = forkConcurrentChildren(childrenByExecutionMode.get(CONCURRENT));
		executeSameThreadChildren(childrenByExecutionMode.get(SAME_THREAD));
		concurrentChildren.join();
	}

	private CompletableFuture<?> forkConcurrentChildren(@Nullable List<? extends TestTask> children) {
		if (children == null) {
			return completedFuture(null);
		}
		CompletableFuture<?>[] futures = children.stream() //
				.map(this::submitInternal) //
				.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	private void executeSameThreadChildren(@Nullable List<? extends TestTask> children) {
		if (children != null) {
			for (var testTask : children) {
				testTask.execute();
			}
		}
	}

	private CompletableFuture<@Nullable Void> submitInternal(TestTask testTask) {
		return toNullable(CompletableFuture.runAsync(testTask::execute, executorService));
	}

	@Override
	public void close() {
		executorService.shutdown();
	}

	@SuppressWarnings("NullAway")
	private static CompletableFuture<@Nullable Void> toNullable(CompletableFuture<Void> future) {
		return future;
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
		public Thread newThread(Runnable r) {
			var thread = new CustomThread(r,
				"junit-%d-worker-%d".formatted(poolNumber, threadNumber.getAndIncrement()));
			thread.setContextClassLoader(classLoader);
			return thread;
		}
	}

	private class CustomThread extends Thread {
		CustomThread(Runnable task, String name) {
			super(task, name);
		}

		static @Nullable ConcurrentHierarchicalTestExecutorService getExecutor() {
			return Thread.currentThread() instanceof CustomThread c ? c.executor() : null;
		}

		private ConcurrentHierarchicalTestExecutorService executor() {
			return ConcurrentHierarchicalTestExecutorService.this;
		}
	}
}
