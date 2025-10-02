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
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

/**
 * Interceptor for the execution of {@link TestTask TestTasks}.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Implementation of this interface must either declare a single constructor
 * or rely on the default constructor. If they declare a constructor, it may
 * optionally declare a single parameter of type {@link Context}.
 *
 * @since 6.1
 * @see Context
 */
@API(status = EXPERIMENTAL, since = "6.1")
public interface ParallelExecutionInterceptor extends AutoCloseable {

	/**
	 * Execute the supplied {@link TestTask}.
	 *
	 * <p>Implementations must ensure the supplied task was executed exactly
	 * once before this method returns unless execution is interrupted.
	 *
	 * @param testTask the task to executed
	 * @throws InterruptedException in case the current thread was interrupted
	 * while waiting for the test task to be executed in a different thread
	 */
	void execute(TestTask testTask) throws InterruptedException;

	/**
	 * Release any resources held by this interceptor.
	 */
	@Override
	void close();

	/**
	 * Context of a {@link ParallelExecutionInterceptor}.
	 *
	 * @since 6.1
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	sealed interface Context permits DefaultParallelExecutionInterceptorContext {

		/**
		 * {@return the configuration to use for parallel test execution}
		 */
		ParallelExecutionConfiguration getConfiguration();

	}

	/**
	 * Default interceptor implementation that executes any passed
	 * {@link TestTask} directly.
	 *
	 * @since 6.1
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	final class Default implements ParallelExecutionInterceptor {

		public static final Default INSTANCE = new Default();

		private Default() {
		}

		@Override
		public void execute(TestTask testTask) {
			testTask.execute();
		}

		@Override
		public void close() {
			// do nothing
		}
	}

	/**
	 * Interceptor that runs {@link TestTask TestTasks} with
	 * {@link ExecutionMode#CONCURRENT ExecutionMode.CONCURRENT} for descriptors
	 * of type {@link TestDescriptor.Type#TEST TEST} in a separate thread pool
	 * with a fixed number of threads.
	 *
	 * @since 6.1
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	final class FixedThreadPoolForTests implements ParallelExecutionInterceptor {

		private final ExecutorService executorService;

		FixedThreadPoolForTests(Context context) {
			this.executorService = Executors.newFixedThreadPool(context.getConfiguration().getParallelism());
		}

		@Override
		public void execute(TestTask testTask) throws InterruptedException {
			if (shouldRunInSeparateThread(testTask)) {
				executeInThreadPool(testTask);
			}
			else {
				testTask.execute();
			}
		}

		private boolean shouldRunInSeparateThread(TestTask task) {
			return task.getExecutionMode() == CONCURRENT //
					&& task.getTestDescriptor().getType() == TEST;
		}

		private void executeInThreadPool(TestTask testTask) throws InterruptedException {
			var newContextClassLoader = Thread.currentThread().getContextClassLoader();
			Runnable runnable = () -> {
				Thread.currentThread().setContextClassLoader(newContextClassLoader);
				testTask.execute();
			};
			try {
				CompletableFuture.runAsync(runnable, executorService).get();
			}
			catch (ExecutionException ex) {
				if (ex.getCause() != null) {
					throw ExceptionUtils.throwAsUncheckedException(ex.getCause());
				}
				throw ExceptionUtils.throwAsUncheckedException(ex);
			}
		}

		@Override
		public void close() {
			executorService.shutdownNow();
		}

	}

}
