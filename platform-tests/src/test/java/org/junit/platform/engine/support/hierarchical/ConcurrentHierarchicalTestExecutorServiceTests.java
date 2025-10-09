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
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.Preconditions;
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

		TestTaskStub<@Nullable Object> task = TestTaskStub.withoutResult(executionMode);

		var customClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
		try (customClassLoader) {
			service = new ConcurrentHierarchicalTestExecutorService(customClassLoader);
			service.submit(task).get();
		}

		assertThat(task.executionThread()).isNotNull().isNotSameAs(Thread.currentThread());
		assertThat(task.executionThread().getName()).matches("junit-\\d+-worker-1");
		assertThat(task.executionThread().getContextClassLoader()).isSameAs(customClassLoader);
	}

	@Test
	@SuppressWarnings("NullAway")
	void invokeAllMustBeExecutedFromWithinThreadPool() {
		var tasks = List.of(TestTaskStub.withoutResult(CONCURRENT));
		service = new ConcurrentHierarchicalTestExecutorService();

		assertPreconditionViolationFor(() -> service.invokeAll(tasks)) //
				.withMessage("invokeAll() must not be called from a thread that is not part of this executor");
	}

	@ParameterizedTest
	@EnumSource(ExecutionMode.class)
	@SuppressWarnings("NullAway")
	void executesSingleChildInSameThreadRegardlessOfItsExecutionMode(ExecutionMode childExecutionMode)
			throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService();

		var child = TestTaskStub.withoutResult(childExecutionMode);
		var root = new TestTaskStub<@Nullable Void>(CONCURRENT,
			Behavior.ofVoid(() -> service.invokeAll(List.of(child))));

		service.submit(root).get();

		assertThat(root.executionThread()).isNotNull();
		assertThat(child.executionThread()).isSameAs(root.executionThread());
	}

	@Test
	@SuppressWarnings("NullAway")
	void executesTwoChildrenConcurrently() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService();

		var latch = new CountDownLatch(2);
		Behavior<Boolean> behavior = () -> {
			latch.countDown();
			return latch.await(100, TimeUnit.MILLISECONDS);
		};

		var children = List.of(new TestTaskStub<>(CONCURRENT, behavior), new TestTaskStub<>(CONCURRENT, behavior));
		var root = new TestTaskStub<@Nullable Void>(CONCURRENT, Behavior.ofVoid(() -> service.invokeAll(children)));

		service.submit(root).get();

		assertThat(children).extracting(TestTaskStub::result).containsOnly(true);
	}

	@Test
	@SuppressWarnings("NullAway")
	void executesTwoChildrenInSameThread() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService();

		var children = List.of(TestTaskStub.withoutResult(SAME_THREAD), TestTaskStub.withoutResult(SAME_THREAD));
		var root = new TestTaskStub<@Nullable Void>(CONCURRENT, Behavior.ofVoid(() -> service.invokeAll(children)));

		service.submit(root).get();

		assertThat(root.executionThread()).isNotNull();
		assertThat(children).extracting(TestTaskStub::executionThread).containsOnly(root.executionThread());
	}

	@NullMarked
	private static final class TestTaskStub<T extends @Nullable Object> implements TestTask {

		private final ExecutionMode executionMode;
		private final Behavior<T> behavior;
		private final ResourceLock resourceLock;
		private @Nullable Thread executionThread;
		private final CompletableFuture<@Nullable T> result = new CompletableFuture<>();

		static <T> TestTaskStub<@Nullable T> withoutResult(ExecutionMode executionMode) {
			return new TestTaskStub<@Nullable T>(executionMode, () -> null);
		}

		TestTaskStub(ExecutionMode executionMode, Behavior<T> behavior) {
			this(executionMode, behavior, NopLock.INSTANCE);
		}

		TestTaskStub(ExecutionMode executionMode, Behavior<T> behavior, ResourceLock resourceLock) {
			this.executionMode = executionMode;
			this.behavior = behavior;
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
			Preconditions.condition(!result.isDone(), "task was already executed");

			executionThread = Thread.currentThread();
			try {
				result.complete(behavior.execute());
			}
			catch (Throwable t) {
				result.completeExceptionally(t);
				throw throwAsUncheckedException(t);
			}
		}

		public @Nullable Thread executionThread() {
			return executionThread;
		}

		public T result() {
			Preconditions.condition(result.isDone(), "task was not executed");
			return result.getNow(null);
		}
	}

	@FunctionalInterface
	interface Behavior<T extends @Nullable Object> {

		static Behavior<@Nullable Void> ofVoid(Executable executable) {
			return () -> {
				executable.execute();
				return null;
			};
		}

		T execute() throws Throwable;
	}
}
