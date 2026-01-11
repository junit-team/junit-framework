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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;
import java.util.concurrent.Future;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

/**
 * A closeable service that executes {@linkplain TestTask test tasks}.
 *
 * @since 1.3
 * @see HierarchicalTestEngine#createExecutorService(ExecutionRequest)
 * @see SameThreadHierarchicalTestExecutorService
 * @see ForkJoinPoolHierarchicalTestExecutorService
 */
@API(status = STABLE, since = "1.10")
public interface HierarchicalTestExecutorService extends AutoCloseable {

	/**
	 * Submit the supplied {@linkplain TestTask test task} to be executed by
	 * this service.
	 *
	 * <p>Implementations may {@linkplain TestTask#execute() execute} the task
	 * asynchronously as long as its
	 * {@linkplain TestTask#getExecutionMode() execution mode} is
	 * {@linkplain ExecutionMode#CONCURRENT concurrent}.
	 *
	 * <p>Implementations must generally acquire and release the task's
	 * {@linkplain TestTask#getResourceLock() resource lock} before and after its
	 * execution unless they execute all tests in the same thread which
	 * upholds the same guarantees.
	 *
	 * @param testTask the test task to be executed
	 * @return a future that the caller can use to wait for the task's execution
	 * to be finished
	 * @see #invokeAll(List)
	 */
	Future<@Nullable Void> submit(TestTask testTask);

	/**
	 * Invoke all supplied {@linkplain TestTask test tasks} and block until
	 * their execution has finished.
	 *
	 * <p>Implementations may {@linkplain TestTask#execute() execute} one or
	 * multiple of the supplied tasks in parallel as long as their
	 * {@linkplain TestTask#getExecutionMode() execution mode} is
	 * {@linkplain ExecutionMode#CONCURRENT concurrent}.
	 *
	 * <p>Implementations must generally acquire and release each task's
	 * {@linkplain TestTask#getResourceLock() resource lock} before and after its
	 * execution unless they execute all tests in the same thread which
	 * upholds the same guarantees.
	 *
	 * @param testTasks the test tasks to be executed
	 * @see #submit(TestTask)
	 */
	void invokeAll(List<? extends TestTask> testTasks);

	/**
	 * Close this service and let it perform any required cleanup work.
	 *
	 * <p>For example, thread-based implementations should usually close their
	 * thread pools in this method.
	 */
	@Override
	void close();

	/**
	 * An executable task that represents a single test or container.
	 */
	interface TestTask {

		/**
		 * Get the {@linkplain ExecutionMode execution mode} of this task.
		 */
		ExecutionMode getExecutionMode();

		/**
		 * Get the {@linkplain ResourceLock resource lock} of this task.
		 */
		ResourceLock getResourceLock();

		/**
		 * Get the {@linkplain TestDescriptor test descriptor} of this task.
		 *
		 * @throws UnsupportedOperationException if not supported for this TestTask implementation
		 * @since 6.0
		 */
		@API(status = EXPERIMENTAL, since = "6.0")
		default TestDescriptor getTestDescriptor() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Execute this task.
		 */
		void execute();

	}

}
