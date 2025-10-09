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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

/**
 * @since 6.1
 */
class ConcurrentHierarchicalTestExecutorServiceTests {

	@AutoClose
	@Nullable
	ConcurrentHierarchicalTestExecutorService service;

	@ParameterizedTest
	@EnumSource(ExecutionMode.class)
	void executesSingleTask(ExecutionMode executionMode) throws Exception {

		var task = new TestTaskStub(executionMode);

		var customClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
		try (customClassLoader) {
			service = new ConcurrentHierarchicalTestExecutorService(customClassLoader);
			service.submit(task).get();
		}

		assertThat(task.executionThread).isNotNull().isNotSameAs(Thread.currentThread());
		assertThat(task.executionThread.getName()).matches("junit-\\d+-worker-1");
		assertThat(task.executionThread.getContextClassLoader()).isSameAs(customClassLoader);
	}

	@NullMarked
	private static class TestTaskStub implements TestTask {

		private final ExecutionMode executionMode;
		private final ResourceLock resourceLock;
		private @Nullable Thread executionThread;

		TestTaskStub(ExecutionMode executionMode) {
			this(executionMode, NopLock.INSTANCE);
		}

		TestTaskStub(ExecutionMode executionMode, ResourceLock resourceLock) {
			this.executionMode = executionMode;
			this.resourceLock = resourceLock;
		}

		@Override
		public ExecutionMode getExecutionMode() {
			return executionMode;
		}

		@Override
		public ResourceLock getResourceLock() {
			return resourceLock;
		}

		@Override
		public void execute() {
			executionThread = Thread.currentThread();
		}
	}
}
