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
import static java.util.concurrent.Future.State.SUCCESS;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;
import org.junit.platform.fakes.TestDescriptorStub;
import org.opentest4j.AssertionFailedError;

/**
 * @since 6.1
 */
@SuppressWarnings("resource")
@Timeout(5)
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
			service = new ConcurrentHierarchicalTestExecutorService(configuration(1), customClassLoader);
			service.submit(task).get();
		}

		task.assertExecutedSuccessfully();

		var executionThread = task.executionThread();
		assertThat(executionThread).isNotNull().isNotSameAs(Thread.currentThread());
		assertThat(executionThread.getName()).matches("junit-\\d+-worker-1");
		assertThat(executionThread.getContextClassLoader()).isSameAs(customClassLoader);
	}

	@Test
	void invokeAllMustBeExecutedFromWithinThreadPool() {
		var tasks = List.of(new TestTaskStub(ExecutionMode.CONCURRENT));
		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));

		assertPreconditionViolationFor(() -> requiredService().invokeAll(tasks)) //
				.withMessage("invokeAll() must be called from a worker thread that belongs to this executor");
	}

	@ParameterizedTest
	@EnumSource(ExecutionMode.class)
	void executesSingleChildInSameThreadRegardlessOfItsExecutionMode(ExecutionMode childExecutionMode)
			throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));

		var child = new TestTaskStub(childExecutionMode);
		var root = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().invokeAll(List.of(child)));

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		child.assertExecutedSuccessfully();

		assertThat(root.executionThread()).isNotNull();
		assertThat(child.executionThread()).isSameAs(root.executionThread());
	}

	@Test
	void executesTwoChildrenConcurrently() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(2));

		var latch = new CountDownLatch(2);
		Executable behavior = () -> {
			latch.countDown();
			latch.await();
		};

		var children = List.of(new TestTaskStub(ExecutionMode.CONCURRENT, behavior),
			new TestTaskStub(ExecutionMode.CONCURRENT, behavior));
		var root = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().invokeAll(children));

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(children).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
	}

	@Test
	void executesTwoChildrenInSameThread() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));

		var children = List.of(new TestTaskStub(ExecutionMode.SAME_THREAD),
			new TestTaskStub(ExecutionMode.SAME_THREAD));
		var root = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().invokeAll(children));

		service.submit(root).get();

		assertThat(root.executionThread()).isNotNull();
		assertThat(children).extracting(TestTaskStub::executionThread).containsOnly(root.executionThread());

		root.assertExecutedSuccessfully();
		assertThat(children).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
	}

	@Test
	void acquiresResourceLockForRootTask() throws Exception {
		var resourceLock = mock(ResourceLock.class);
		when(resourceLock.acquire()).thenReturn(resourceLock);

		var task = new TestTaskStub(ExecutionMode.CONCURRENT).withResourceLock(resourceLock);

		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));
		service.submit(task).get();

		task.assertExecutedSuccessfully();

		var inOrder = inOrder(resourceLock);
		inOrder.verify(resourceLock).acquire();
		inOrder.verify(resourceLock).close();
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void acquiresResourceLockForChildTasks() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(2));

		var resourceLock = mock(ResourceLock.class);
		when(resourceLock.tryAcquire()).thenReturn(true, false);
		when(resourceLock.acquire()).thenReturn(resourceLock);

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT).withResourceLock(resourceLock).withName("child1");
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT).withResourceLock(resourceLock).withName("child2");
		var children = List.of(child1, child2);
		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> requiredService().invokeAll(children)).withName(
			"root");

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(children).allSatisfy(TestTaskStub::assertExecutedSuccessfully);

		assertThat(children).extracting(TestTaskStub::executionThread) //
				.filteredOn(isEqual(root.executionThread())).hasSizeLessThan(2);

		verify(resourceLock, atLeast(2)).tryAcquire();
		verify(resourceLock, atLeast(1)).acquire();
		verify(resourceLock, times(2)).close();
	}

	@Test
	void runsTasksWithoutConflictingLocksConcurrently() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(3));

		var resourceLock = new SingleLock(exclusiveResource(), new ReentrantLock());

		var latch = new CountDownLatch(3);
		Executable behavior = () -> {
			latch.countDown();
			latch.await();
		};
		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior).withResourceLock(resourceLock).withName(
			"child1");
		var child2 = new TestTaskStub(ExecutionMode.SAME_THREAD).withResourceLock(resourceLock).withName("child2");
		var leaf1 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior).withName("leaf1");
		var leaf2 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior).withName("leaf2");
		var leaves = List.of(leaf1, leaf2);
		var child3 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().invokeAll(leaves)).withName(
			"child3");
		var children = List.of(child1, child2, child3);
		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> requiredService().invokeAll(children)).withName(
			"root");

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(children).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(leaves).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
	}

	@Test
	void prioritizesChildrenOfStartedContainers() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(2));

		var leavesStarted = new CountDownLatch(2);
		var leaf = new TestTaskStub(ExecutionMode.CONCURRENT, leavesStarted::countDown) //
				.withName("leaf").withLevel(3);
		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().submit(leaf).get()) //
				.withName("child1").withLevel(2);
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, leavesStarted::countDown) //
				.withName("child2").withLevel(2);
		var child3 = new TestTaskStub(ExecutionMode.SAME_THREAD, leavesStarted::await) //
				.withName("child3").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD,
			() -> requiredService().invokeAll(List.of(child1, child2, child3))) //
					.withName("root").withLevel(1);

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(List.of(child1, child2, child3)).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		leaf.assertExecutedSuccessfully();

		assertThat(leaf.startTime).isBeforeOrEqualTo(child2.startTime);
	}

	@Test
	void prioritizesTestsOverContainers() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(2));

		var leavesStarted = new CountDownLatch(2);
		var leaf = new TestTaskStub(ExecutionMode.CONCURRENT, leavesStarted::countDown) //
				.withName("leaf").withLevel(3).withType(TEST);
		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().submit(leaf).get()) //
				.withName("child1").withLevel(2).withType(CONTAINER);
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, leavesStarted::countDown) //
				.withName("child2").withLevel(2).withType(TEST);
		var child3 = new TestTaskStub(ExecutionMode.SAME_THREAD, leavesStarted::await) //
				.withName("child3").withLevel(2).withType(TEST);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD,
			() -> requiredService().invokeAll(List.of(child1, child2, child3))) //
					.withName("root").withLevel(1);

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(List.of(child1, child2, child3)).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		leaf.assertExecutedSuccessfully();

		assertThat(child2.startTime).isBeforeOrEqualTo(child1.startTime);
	}

	private static ExclusiveResource exclusiveResource() {
		return new ExclusiveResource("key", ExclusiveResource.LockMode.READ_WRITE);
	}

	private ConcurrentHierarchicalTestExecutorService requiredService() {
		return requireNonNull(service);
	}

	private static ParallelExecutionConfiguration configuration(int parallelism) {
		return new DefaultParallelExecutionConfiguration(parallelism, parallelism, 256 + parallelism, parallelism, 0,
			__ -> true);
	}

	@NullMarked
	private static final class TestTaskStub implements TestTask {

		private final ExecutionMode executionMode;
		private final Executable behavior;

		private ResourceLock resourceLock = NopLock.INSTANCE;
		private @Nullable String name;
		private int level = 1;
		private TestDescriptor.Type type = TEST;

		private final CompletableFuture<@Nullable Void> result = new CompletableFuture<>();
		private volatile @Nullable Instant startTime;
		private volatile @Nullable Thread executionThread;

		TestTaskStub(ExecutionMode executionMode) {
			this(executionMode, () -> {
			});
		}

		TestTaskStub(ExecutionMode executionMode, Executable behavior) {
			this.executionMode = executionMode;
			this.behavior = behavior;
		}

		TestTaskStub withName(String name) {
			this.name = name;
			return this;
		}

		TestTaskStub withLevel(int level) {
			this.level = level;
			return this;
		}

		TestTaskStub withType(TestDescriptor.Type type) {
			this.type = type;
			return this;
		}

		TestTaskStub withResourceLock(ResourceLock resourceLock) {
			this.resourceLock = resourceLock;
			return this;
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
		public TestDescriptor getTestDescriptor() {
			var name = String.valueOf(this.name);
			var uniqueId = UniqueId.root("root", name);
			for (var i = 1; i < level; i++) {
				uniqueId = uniqueId.append("child", name);
			}
			return new TestDescriptorStub(uniqueId, name) {
				@Override
				public Type getType() {
					return type;
				}
			};
		}

		@Override
		public void execute() {
			startTime = Instant.now();
			Preconditions.condition(!result.isDone(), "task was already executed");

			executionThread = Thread.currentThread();
			try {
				behavior.execute();
				result.complete(null);
			}
			catch (Throwable t) {
				result.completeExceptionally(t);
				throw throwAsUncheckedException(t);
			}
		}

		void assertExecutedSuccessfully() {
			if (result.isCompletedExceptionally()) {
				throw new AssertionFailedError("Failure during execution", result.exceptionNow());
			}
			assertThat(result.state()).isEqualTo(SUCCESS);
		}

		@Nullable
		Thread executionThread() {
			return executionThread;
		}

		@Override
		public String toString() {
			return "%s @ %s".formatted(new ToStringBuilder(this).append("name", name), Integer.toHexString(hashCode()));
		}
	}

}
