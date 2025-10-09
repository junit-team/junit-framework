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

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

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

		var task = TestTaskStub.withoutResult(executionMode);

		var customClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
		try (customClassLoader) {
			service = new ConcurrentHierarchicalTestExecutorService(configuration(1), customClassLoader);
			service.submit(task).get();
		}

		assertThat(task.executionThread()).isNotNull().isNotSameAs(Thread.currentThread());
		assertThat(task.executionThread().getName()).matches("junit-\\d+-worker-1");
		assertThat(task.executionThread().getContextClassLoader()).isSameAs(customClassLoader);
	}

	@Test
	void invokeAllMustBeExecutedFromWithinThreadPool() {
		var tasks = List.of(TestTaskStub.withoutResult(CONCURRENT));
		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));

		assertPreconditionViolationFor(() -> requiredService().invokeAll(tasks)) //
				.withMessage("invokeAll() must not be called from a thread that is not part of this executor");
	}

	@ParameterizedTest
	@EnumSource(ExecutionMode.class)
	void executesSingleChildInSameThreadRegardlessOfItsExecutionMode(ExecutionMode childExecutionMode)
			throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));

		var child = TestTaskStub.withoutResult(childExecutionMode);
		var root = TestTaskStub.withoutResult(CONCURRENT, () -> requiredService().invokeAll(List.of(child)));

		service.submit(root).get();

		assertThat(root.executionThread()).isNotNull();
		assertThat(child.executionThread()).isSameAs(root.executionThread());
	}

	@Test
	void executesTwoChildrenConcurrently() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(2));

		var latch = new CountDownLatch(2);
		ThrowingSupplier<Boolean> behavior = () -> {
			latch.countDown();
			return latch.await(100, TimeUnit.MILLISECONDS);
		};

		var children = List.of(TestTaskStub.withResult(CONCURRENT, behavior),
			TestTaskStub.withResult(CONCURRENT, behavior));
		var root = TestTaskStub.withoutResult(CONCURRENT, () -> requiredService().invokeAll(children));

		service.submit(root).get();

		assertThat(children).extracting(TestTaskStub::result).containsOnly(true);
	}

	@Test
	void executesTwoChildrenInSameThread() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));

		var children = List.of(TestTaskStub.withoutResult(SAME_THREAD), TestTaskStub.withoutResult(SAME_THREAD));
		var root = TestTaskStub.withoutResult(CONCURRENT, () -> requiredService().invokeAll(children));

		service.submit(root).get();

		assertThat(root.executionThread()).isNotNull();
		assertThat(children).extracting(TestTaskStub::executionThread).containsOnly(root.executionThread());
	}

	@Test
	void acquiresResourceLockForRootTask() throws Exception {
		var resourceLock = mock(ResourceLock.class);
		when(resourceLock.acquire()).thenReturn(resourceLock);

		var task = TestTaskStub.withoutResult(CONCURRENT).withResourceLock(resourceLock);

		service = new ConcurrentHierarchicalTestExecutorService(configuration(1));
		service.submit(task).get();

		assertThat(task.executionThread()).isNotNull();

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

		var child1 = TestTaskStub.withoutResult(CONCURRENT).withResourceLock(resourceLock);
		var child2 = TestTaskStub.withoutResult(CONCURRENT).withResourceLock(resourceLock);
		var children = List.of(child1, child2);
		var root = TestTaskStub.withoutResult(SAME_THREAD, () -> requiredService().invokeAll(children));

		service.submit(root).get();

		assertThat(root.executionThread()).isNotNull();
		assertThat(children).extracting(TestTaskStub::executionThread) //
				.doesNotContainNull() //
				.filteredOn(isEqual(root.executionThread())).hasSizeLessThan(2);

		verify(resourceLock, atLeast(2)).tryAcquire();
		verify(resourceLock).acquire();
		verify(resourceLock, times(2)).close();
		verifyNoMoreInteractions(resourceLock);
	}

	@Test
	void runsTasksWithoutConflictingLocksConcurrently() throws Exception {
		service = new ConcurrentHierarchicalTestExecutorService(configuration(3));

		var resourceLock = new SingleLock(exclusiveResource(), new ReentrantLock());

		var latch = new CountDownLatch(3);
		ThrowingSupplier<Boolean> behavior = () -> {
			latch.countDown();
			return latch.await(100, TimeUnit.MILLISECONDS);
		};
		var child1 = TestTaskStub.withResult(CONCURRENT, behavior).withResourceLock(resourceLock).withName("child1");
		var child2 = TestTaskStub.withoutResult(SAME_THREAD).withResourceLock(resourceLock).withName("child2");
		var leaf1 = TestTaskStub.withResult(CONCURRENT, behavior).withName("leaf1");
		var leaf2 = TestTaskStub.withResult(CONCURRENT, behavior).withName("leaf2");
		var leafs = List.of(leaf1, leaf2);
		var child3 = TestTaskStub.withoutResult(CONCURRENT, () -> requiredService().invokeAll(leafs)).withName(
			"child3");
		var children = List.of(child1, child2, child3);
		var root = TestTaskStub.withoutResult(SAME_THREAD, () -> requiredService().invokeAll(children)).withName(
			"root");

		service.submit(root).get();

		printTimeline(Stream.concat(Stream.of(root), Stream.concat(children.stream(), leafs.stream())));

		assertThat(root.executionThread()).isNotNull();
		assertThat(children).extracting(TestTaskStub::executionThread).doesNotContainNull();
		assertThat(leafs).extracting(TestTaskStub::executionThread).doesNotContainNull();
		assertThat(Stream.concat(Stream.of(child1), leafs.stream())).extracting(TestTaskStub::result) //
				.containsOnly(
			true);
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
	private static final class TestTaskStub<T extends @Nullable Object> implements TestTask {

		private final ExecutionMode executionMode;
		private final ThrowingSupplier<T> behavior;

		private ResourceLock resourceLock = NopLock.INSTANCE;
		private @Nullable String name;

		private final CompletableFuture<@Nullable T> result = new CompletableFuture<>();
		private @Nullable Instant startTime;
		private @Nullable Instant endTime;
		private @Nullable Thread executionThread;

		static TestTaskStub<?> withoutResult(ExecutionMode executionMode) {
			return new TestTaskStub<@Nullable Void>(executionMode, () -> null);
		}

		static TestTaskStub<?> withoutResult(ExecutionMode executionMode, Executable executable) {
			return new TestTaskStub<@Nullable Void>(executionMode, () -> {
				executable.execute();
				return null;
			});
		}

		@SuppressWarnings("SameParameterValue")
		static <T> TestTaskStub<T> withResult(ExecutionMode executionMode, ThrowingSupplier<T> supplier) {
			return new TestTaskStub<>(executionMode, supplier);
		}

		TestTaskStub(ExecutionMode executionMode, ThrowingSupplier<T> behavior) {
			this.executionMode = executionMode;
			this.behavior = behavior;
		}

		TestTaskStub<T> withResourceLock(ResourceLock resourceLock) {
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
		public void execute() {
			startTime = Instant.now();
			try {
				Preconditions.condition(!result.isDone(), "task was already executed");

				executionThread = Thread.currentThread();
				try {
					result.complete(behavior.get());
				}
				catch (Throwable t) {
					result.completeExceptionally(t);
					throw throwAsUncheckedException(t);
				}
			}
			finally {
				endTime = Instant.now();
			}
		}

		@Nullable Thread executionThread() {
			return executionThread;
		}

		T result() {
			Preconditions.condition(result.isDone(), "task was not executed");
			return result.getNow(null);
		}

		TestTaskStub<T> withName(String name) {
			this.name = name;
			return this;
		}
	}

	static void printTimeline(Stream<TestTaskStub<?>> taskStream) {
		var allTasks = taskStream.toList();
		assertThat(allTasks.stream().filter(task -> task.executionThread() == null)) //
				.describedAs(				"Unexecuted tasks").isEmpty();
		var statistics = allTasks.stream() //
				.flatMap(task -> Stream.concat(Stream.of(task.startTime),
						Optional.ofNullable(task.endTime).stream())).mapToLong(
						instant -> requireNonNull(instant).toEpochMilli()) //
				.summaryStatistics();
		var rangeMillis = statistics.getMax() - statistics.getMin();
		var width = 100;
		var scale = (double) width / rangeMillis;
		var sortedTasks = allTasks.stream() //
				.sorted(comparing(task -> requireNonNull(task.startTime))) //
				.toList();
		var tasksByThread = sortedTasks.stream() //
				.sorted(comparingLong(
						testTaskStub -> requireNonNull(testTaskStub.executionThread()).threadId())).collect(
						groupingBy(task -> requireNonNull(task.executionThread), LinkedHashMap::new, toList()));
		ToIntFunction<@Nullable Instant> indexFunction = instant -> (int) ((requireNonNull(
				instant).toEpochMilli() - statistics.getMin()) * scale);
		tasksByThread.forEach((thread, tasks) -> printTimelineForThread(requireNonNull(thread), tasks, width, indexFunction));
	}

	private static void printTimelineForThread(Thread thread, List<TestTaskStub<?>> tasks, int width, ToIntFunction<@Nullable Instant> indexFunction) {
		System.out.printf("%n%s (%d)%n", thread.getName(), tasks.size());
		StringBuilder builder = new StringBuilder();
		for (var task : tasks) {
			builder.append(".".repeat(width + 1));
			int startIndex = indexFunction.applyAsInt(task.startTime);
			builder.setCharAt(startIndex, '<');
			if (task.endTime == null) {
				builder.setCharAt(startIndex + 1, '-');
				builder.setCharAt(startIndex + 2, '?');
			}
			else {
				int endIndex = indexFunction.applyAsInt(task.endTime);
				if (endIndex == startIndex) {
					builder.setCharAt(endIndex, 'O');
				}
				else {
					for (int i = startIndex + 1; i < endIndex; i++) {
						builder.setCharAt(i, '-');
					}
					builder.setCharAt(endIndex, '>');
				}
			}
			builder.append("   ").append(task.name);
			System.out.println(builder);
			builder.setLength(0);
		}
	}

}
