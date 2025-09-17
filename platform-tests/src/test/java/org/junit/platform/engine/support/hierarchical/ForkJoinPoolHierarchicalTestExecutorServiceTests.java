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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ_WRITE;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;
import org.junit.platform.engine.support.hierarchical.ForkJoinPoolHierarchicalTestExecutorService.TaskEventListener;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

@Timeout(5)
class ForkJoinPoolHierarchicalTestExecutorServiceTests {

	DummyTaskFactory taskFactory = new DummyTaskFactory();
	LockManager lockManager = new LockManager();

	@Test
	void exceptionsFromInvalidConfigurationAreNotSwallowed() {
		var configuration = new DefaultParallelExecutionConfiguration(2, 1, 1, 1, 0, __ -> true);

		JUnitException exception = assertThrows(JUnitException.class, () -> {
			try (var pool = new ForkJoinPoolHierarchicalTestExecutorService(configuration)) {
				assertNotNull(pool, "we won't get here");
			}
		});

		assertThat(exception).hasMessage("Failed to create ForkJoinPool");
		assertThat(exception).rootCause().isInstanceOf(IllegalArgumentException.class);
	}

	static List<Arguments> incompatibleLockCombinations() {
		return List.of(//
			arguments(//
				Set.of(GLOBAL_READ), //
				Set.of(GLOBAL_READ_WRITE) //
			), //
			arguments(//
				Set.of(new ExclusiveResource("a", LockMode.READ)), //
				Set.of(new ExclusiveResource("a", LockMode.READ_WRITE)) //
			), //
			arguments(//
				Set.of(new ExclusiveResource("a", LockMode.READ_WRITE)), //
				Set.of(new ExclusiveResource("a", LockMode.READ_WRITE)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ_WRITE)), //
				Set.of(GLOBAL_READ, new ExclusiveResource("b", LockMode.READ_WRITE)) //
			), //
			arguments(//
				Set.of(new ExclusiveResource("b", LockMode.READ)), //
				Set.of(new ExclusiveResource("a", LockMode.READ)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ_WRITE)), //
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ_WRITE), //
				Set.of(GLOBAL_READ) //
			), //
			arguments(//
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ),
					new ExclusiveResource("b", LockMode.READ), new ExclusiveResource("d", LockMode.READ)),
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ),
					new ExclusiveResource("c", LockMode.READ)) //
			)//
		);
	}

	@SuppressWarnings("NullAway")
	@ParameterizedTest
	@MethodSource("incompatibleLockCombinations")
	void defersTasksWithIncompatibleLocks(Set<ExclusiveResource> initialResources,
			Set<ExclusiveResource> incompatibleResources) throws Throwable {

		var initialLock = lockManager.getLockForResources(initialResources);
		var incompatibleLock = lockManager.getLockForResources(incompatibleResources);

		var deferred = new CountDownLatch(1);
		var deferredTask = new AtomicReference<TestTask>();

		TaskEventListener taskEventListener = testTask -> {
			deferredTask.set(testTask);
			deferred.countDown();
		};

		var incompatibleTask = taskFactory.create("incompatibleTask", incompatibleLock);

		var tasks = runWithAttemptedWorkStealing(taskEventListener, incompatibleTask, initialLock,
			() -> await(deferred, "Interrupted while waiting for task to be deferred"));

		assertEquals(incompatibleTask, deferredTask.get());
		assertEquals(tasks.get("nestedTask").threadName, tasks.get("leafTaskB").threadName);
		assertNotEquals(tasks.get("leafTaskA").threadName, tasks.get("leafTaskB").threadName);
	}

	static List<Arguments> compatibleLockCombinations() {
		return List.of(//
			arguments(//
				Set.of(GLOBAL_READ), //
				Set.of(new ExclusiveResource("a", LockMode.READ)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ), //
				Set.of(new ExclusiveResource("a", LockMode.READ_WRITE)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ), //
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ_WRITE)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ), //
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ)), //
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ),
					new ExclusiveResource("b", LockMode.READ), new ExclusiveResource("c", LockMode.READ)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ)), //
				Set.of(GLOBAL_READ, new ExclusiveResource("b", LockMode.READ)) //
			), //
			arguments(//
				Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ)), //
				Set.of(new ExclusiveResource("a", LockMode.READ), new ExclusiveResource("b", LockMode.READ),
					new ExclusiveResource("c", LockMode.READ)) //
			)//
		);
	}

	@SuppressWarnings("DataFlowIssue")
	@ParameterizedTest
	@MethodSource("compatibleLockCombinations")
	void canWorkStealTaskWithCompatibleLocks(Set<ExclusiveResource> initialResources,
			Set<ExclusiveResource> compatibleResources) throws Throwable {

		var initialLock = lockManager.getLockForResources(initialResources);
		var compatibleLock = lockManager.getLockForResources(compatibleResources);

		var deferredTask = new AtomicReference<TestTask>();

		var workStolen = new CountDownLatch(1);
		var compatibleTask = taskFactory.create("compatibleTask", compatibleLock, workStolen::countDown);

		var tasks = runWithAttemptedWorkStealing(deferredTask::set, compatibleTask, initialLock,
			() -> await(workStolen, "Interrupted while waiting for work to be stolen"));

		assertNull(deferredTask.get());
		assertEquals(tasks.get("nestedTask").threadName, tasks.get("leafTaskB").threadName);
		assertNotEquals(tasks.get("leafTaskA").threadName, tasks.get("leafTaskB").threadName);
	}

	@Test
	void defersTasksWithIncompatibleLocksOnMultipleLevels() throws Throwable {

		var initialLock = lockManager.getLockForResources(
			Set.of(GLOBAL_READ, new ExclusiveResource("a", LockMode.READ)));
		var incompatibleLock1 = lockManager.getLockForResource(new ExclusiveResource("a", LockMode.READ_WRITE));
		var compatibleLock1 = lockManager.getLockForResource(new ExclusiveResource("b", LockMode.READ));
		var incompatibleLock2 = lockManager.getLockForResource(new ExclusiveResource("b", LockMode.READ_WRITE));

		var deferred = new ConcurrentHashMap<TestTask, CountDownLatch>();
		var deferredTasks = new CopyOnWriteArrayList<TestTask>();

		@SuppressWarnings("NullAway")
		TaskEventListener taskEventListener = testTask -> {
			deferredTasks.add(testTask);
			deferred.get(testTask).countDown();
		};

		var incompatibleTask1 = taskFactory.create("incompatibleTask1", incompatibleLock1);
		deferred.put(incompatibleTask1, new CountDownLatch(1));

		var incompatibleTask2 = taskFactory.create("incompatibleTask2", incompatibleLock2);
		deferred.put(incompatibleTask2, new CountDownLatch(1));

		var configuration = new DefaultParallelExecutionConfiguration(2, 2, 2, 2, 1, __1 -> true);

		withForkJoinPoolHierarchicalTestExecutorService(configuration, taskEventListener, service -> {

			var nestedTask2 = createNestedTaskWithTwoConcurrentLeafTasks(service, "2", compatibleLock1,
				List.of(incompatibleTask2), //
				() -> await(deferred.get(incompatibleTask2), incompatibleTask2.identifier + " to be deferred"));

			var nestedTask1 = createNestedTaskWithTwoConcurrentLeafTasks(service, "1", initialLock,
				List.of(incompatibleTask1, nestedTask2), //
				() -> {
					await(deferred.get(incompatibleTask1), incompatibleTask1.identifier + " to be deferred");
					await(nestedTask2.started, nestedTask2.identifier + " to be started");
				});

			service.submit(nestedTask1).get();
		});

		assertThat(deferredTasks) //
				.startsWith(incompatibleTask1, incompatibleTask2) //
				.containsOnly(incompatibleTask1, incompatibleTask2) // incompatibleTask1 may be deferred multiple times
				.containsOnlyOnce(incompatibleTask2);
		assertThat(taskFactory.tasks) //
				.hasSize(3 + 3 + 2) //
				.values().extracting(it -> it.completion.isDone()).containsOnly(true);
		assertThat(taskFactory.tasks) //
				.values().extracting(it -> it.completion.isCompletedExceptionally()).containsOnly(false);
	}

	private Map<String, DummyTestTask> runWithAttemptedWorkStealing(TaskEventListener taskEventListener,
			DummyTestTask taskToBeStolen, ResourceLock initialLock, Runnable waitAction) throws Throwable {

		var configuration = new DefaultParallelExecutionConfiguration(2, 2, 2, 2, 1, __ -> true);

		withForkJoinPoolHierarchicalTestExecutorService(configuration, taskEventListener, service -> {

			var nestedTask = createNestedTaskWithTwoConcurrentLeafTasks(service, "", initialLock,
				List.of(taskToBeStolen), waitAction);

			service.submit(nestedTask).get();
		});

		return taskFactory.tasks;
	}

	private DummyTestTask createNestedTaskWithTwoConcurrentLeafTasks(
			ForkJoinPoolHierarchicalTestExecutorService service, String identifierSuffix, ResourceLock parentLock,
			List<DummyTestTask> tasksToFork, Runnable waitAction) {

		return taskFactory.create("nestedTask" + identifierSuffix, parentLock, () -> {

			var bothLeafTasksAreRunning = new CountDownLatch(2);

			var leafTaskA = taskFactory.create("leafTaskA" + identifierSuffix, NopLock.INSTANCE, () -> {
				tasksToFork.forEach(task -> service.new ExclusiveTask(task).fork());
				bothLeafTasksAreRunning.countDown();
				bothLeafTasksAreRunning.await();
				waitAction.run();
			});

			var leafTaskB = taskFactory.create("leafTaskB" + identifierSuffix, NopLock.INSTANCE, () -> {
				bothLeafTasksAreRunning.countDown();
				bothLeafTasksAreRunning.await();
			});

			service.invokeAll(List.of(leafTaskA, leafTaskB));
		});
	}

	private static void await(CountDownLatch latch, String message) {
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			System.out.println("Interrupted while waiting for " + message);
		}
	}

	private void withForkJoinPoolHierarchicalTestExecutorService(ParallelExecutionConfiguration configuration,
			TaskEventListener taskEventListener, ThrowingConsumer<ForkJoinPoolHierarchicalTestExecutorService> action)
			throws Throwable {
		try (var service = new ForkJoinPoolHierarchicalTestExecutorService(configuration, taskEventListener)) {

			action.accept(service);

			service.forkJoinPool.shutdown();
			assertTrue(service.forkJoinPool.awaitTermination(5, SECONDS), "Pool did not terminate within timeout");
		}
	}

	static final class DummyTestTask implements TestTask {

		private final String identifier;
		private final ResourceLock resourceLock;
		private final Executable action;

		@Nullable
		private volatile String threadName;

		private final CountDownLatch started = new CountDownLatch(1);
		private final CompletableFuture<?> completion = new CompletableFuture<>();

		DummyTestTask(String identifier, ResourceLock resourceLock, Executable action) {
			this.identifier = identifier;
			this.resourceLock = resourceLock;
			this.action = action;
		}

		@Override
		public ExecutionMode getExecutionMode() {
			return CONCURRENT;
		}

		@Override
		public ResourceLock getResourceLock() {
			return resourceLock;
		}

		@Override
		public void execute() {
			threadName = Thread.currentThread().getName();
			started.countDown();
			try {
				action.execute();
				completion.complete(null);
			}
			catch (Throwable e) {
				completion.completeExceptionally(e);
				throw new RuntimeException("Action " + identifier + " failed", e);
			}
		}

		@Override
		public String toString() {
			return identifier;
		}
	}

	static final class DummyTaskFactory {

		final Map<String, DummyTestTask> tasks = new HashMap<>();

		DummyTestTask create(String identifier, ResourceLock resourceLock) {
			return create(identifier, resourceLock, () -> {
			});
		}

		DummyTestTask create(String identifier, ResourceLock resourceLock, Executable action) {
			DummyTestTask task = new DummyTestTask(identifier, resourceLock, action);
			tasks.put(task.identifier, task);
			return task;
		}
	}
}
