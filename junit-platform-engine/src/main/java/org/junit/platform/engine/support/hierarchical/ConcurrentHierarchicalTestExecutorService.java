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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

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
		return toNullable(CompletableFuture.runAsync(testTask::execute, executorService));
	}

	@Override
	public void invokeAll(List<? extends TestTask> testTasks) {
		testTasks.forEach(TestTask::execute);
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void close() {
		executorService.shutdown();
	}

	@SuppressWarnings("NullAway")
	private static Future<@Nullable Void> toNullable(Future<Void> voidCompletableFuture) {
		return voidCompletableFuture;
	}

	static class CustomThreadFactory implements ThreadFactory {

		private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final int poolNumber;
		private final ClassLoader classLoader;

		public CustomThreadFactory(ClassLoader classLoader) {
			this.classLoader = classLoader;
			this.poolNumber = POOL_NUMBER.getAndIncrement();
		}

		@Override
		public Thread newThread(Runnable r) {
			var thread = new Thread(r, "junit-%d-worker-%d".formatted(poolNumber, threadNumber.getAndIncrement()));
			thread.setContextClassLoader(classLoader);
			return thread;
		}
	}
}
