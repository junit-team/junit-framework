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

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Future.State.SUCCESS;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

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
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;
import org.junit.platform.fakes.TestDescriptorStub;
import org.opentest4j.AssertionFailedError;

/**
 * @since 6.1
 */
@SuppressWarnings("resource")
@Timeout(5)
class WorkerThreadPoolHierarchicalTestExecutorServiceTests {

	@AutoClose
	@Nullable
	WorkerThreadPoolHierarchicalTestExecutorService service;

	@ParameterizedTest
	@EnumSource(ExecutionMode.class)
	void executesSingleTask(ExecutionMode executionMode) throws Exception {

		var task = new TestTaskStub(executionMode);

		var customClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
		try (customClassLoader) {
			service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(1), customClassLoader);
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
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(1));

		assertPreconditionViolationFor(() -> requiredService().invokeAll(tasks)) //
				.withMessage("invokeAll() must be called from a worker thread that belongs to this executor");
	}

	@ParameterizedTest
	@EnumSource(ExecutionMode.class)
	void executesSingleChildInSameThreadRegardlessOfItsExecutionMode(ExecutionMode childExecutionMode)
			throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(1));

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
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2));

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
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(1));

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

		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(1));
		service.submit(task).get();

		task.assertExecutedSuccessfully();

		var inOrder = inOrder(resourceLock);
		inOrder.verify(resourceLock).acquire();
		inOrder.verify(resourceLock).close();
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void acquiresResourceLockForChildTasks() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2));

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
				.filteredOn(isEqual(root.executionThread())).hasSizeLessThanOrEqualTo(2);

		verify(resourceLock, atLeast(2)).tryAcquire();
		verify(resourceLock, atLeast(1)).acquire();
		verify(resourceLock, times(2)).close();
	}

	@Test
	void runsTasksWithoutConflictingLocksConcurrently() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(3));

		var resourceLock = new SingleLock(exclusiveResource(LockMode.READ_WRITE), new ReentrantLock());

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
	void processingQueueEntriesSkipsOverUnavailableResources() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2));

		var resourceLock = new SingleLock(exclusiveResource(LockMode.READ_WRITE), new ReentrantLock());

		var lockFreeChildrenStarted = new CountDownLatch(2);
		var child1Started = new CountDownLatch(1);

		Executable child1Behaviour = () -> {
			child1Started.countDown();
			lockFreeChildrenStarted.await();
		};
		Executable child4Behaviour = () -> {
			lockFreeChildrenStarted.countDown();
			child1Started.await();
		};

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, child1Behaviour) //
				.withResourceLock(resourceLock) //
				.withName("child1");
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, lockFreeChildrenStarted::countDown) //
				.withName("child2"); //
		var child3 = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withResourceLock(resourceLock) //
				.withName("child3");
		var child4 = new TestTaskStub(ExecutionMode.CONCURRENT, child4Behaviour) //
				.withName("child4");
		var children = List.of(child1, child2, child3, child4);
		var root = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().invokeAll(children)) //
				.withName("root");

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(children).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(child4.executionThread).isEqualTo(child2.executionThread);
		assertThat(child3.startTime).isAfterOrEqualTo(child2.startTime);
	}

	@Test
	void invokeAllQueueEntriesSkipsOverUnavailableResources() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2));

		var resourceLock = new SingleLock(exclusiveResource(LockMode.READ_WRITE), new ReentrantLock());

		var lockFreeChildrenStarted = new CountDownLatch(2);
		var child2Started = new CountDownLatch(1);

		Executable child1Behaviour = () -> {
			lockFreeChildrenStarted.countDown();
			child2Started.await();
		};
		Executable child2Behaviour = () -> {
			child2Started.countDown();
			lockFreeChildrenStarted.await();
		};

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, child1Behaviour) //
				.withName("child1");
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, child2Behaviour) //
				.withResourceLock(resourceLock) //
				.withName("child2");
		var child3 = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withResourceLock(resourceLock) //
				.withName("child3"); //
		var child4 = new TestTaskStub(ExecutionMode.CONCURRENT, lockFreeChildrenStarted::countDown) //
				.withName("child4");
		var children = List.of(child1, child2, child3, child4);
		var root = new TestTaskStub(ExecutionMode.CONCURRENT, () -> requiredService().invokeAll(children)) //
				.withName("root");

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(children).allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(child1.executionThread).isEqualTo(child4.executionThread);
		assertThat(child3.startTime).isAfterOrEqualTo(child4.startTime);
	}

	@Test
	void prioritizesChildrenOfStartedContainers() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2, 2));

		var leafSubmitted = new CountDownLatch(1);
		var child2AndLeafStarted = new CountDownLatch(2);

		var leaf = new TestTaskStub(ExecutionMode.CONCURRENT, child2AndLeafStarted::countDown) //
				.withName("leaf").withLevel(3);

		Executable child3Behavior = () -> {
			var future = requiredService().submit(leaf);
			leafSubmitted.countDown();
			child2AndLeafStarted.await();
			future.get();
		};

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, leafSubmitted::await) //
				.withName("child1").withLevel(2);
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, child2AndLeafStarted::countDown) //
				.withName("child2").withLevel(2);
		var child3 = new TestTaskStub(ExecutionMode.CONCURRENT, child3Behavior) //
				.withType(CONTAINER).withName("child3").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD,
			() -> requiredService().invokeAll(List.of(child1, child2, child3))) //
					.withName("root").withLevel(1);

		service.submit(root).get();

		root.assertExecutedSuccessfully();
		assertThat(List.of(root, child1, child2, leaf, child3)).allSatisfy(TestTaskStub::assertExecutedSuccessfully);

		assertThat(leaf.startTime).isBeforeOrEqualTo(child2.startTime);
		assertThat(leaf.executionThread).isSameAs(child2.executionThread).isNotSameAs(child3.executionThread);
	}

	@Test
	void prioritizesTestsOverContainers() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2));

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

	@Test
	void limitsWorkerThreadsToMaxPoolSize() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(3, 3));

		CountDownLatch latch = new CountDownLatch(3);
		Executable behavior = () -> {
			latch.countDown();
			latch.await();
		};
		var leaf1a = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("leaf1a").withLevel(3);
		var leaf1b = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("leaf1b").withLevel(3);
		var leaf2a = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("leaf2a").withLevel(3);
		var leaf2b = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("leaf2b").withLevel(3);

		// When executed, there are 2 worker threads active and 1 available.
		// Both invokeAlls race each other trying to start 1 more.
		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT,
			() -> requiredService().invokeAll(List.of(leaf1a, leaf1b))) //
					.withName("child1").withLevel(2);
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT,
			() -> requiredService().invokeAll(List.of(leaf2a, leaf2b))) //
					.withName("child2").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD,
			() -> requiredService().invokeAll(List.of(child1, child2))) //
					.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(List.of(root, child1, child2, leaf1a, leaf1b, leaf2a, leaf2b)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(Stream.of(leaf1a, leaf1b, leaf2a, leaf2b).map(TestTaskStub::executionThread).distinct()) //
				.hasSize(3);
	}

	@Test
	void stealsBlockingChildren() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2, 2));

		var child1Started = new CountDownLatch(1);
		var leaf2aStarted = new CountDownLatch(1);
		var leaf2bStarted = new CountDownLatch(1);
		var readWriteLock = new ReentrantReadWriteLock();
		var readOnlyResourceLock = new SingleLock(exclusiveResource(LockMode.READ), readWriteLock.readLock()) {
			@Override
			public void release() {
				super.release();
				try {
					leaf2aStarted.await();
				}
				catch (InterruptedException e) {
					fail(e);
				}
			}
		};
		var readWriteResourceLock = new SingleLock(exclusiveResource(LockMode.READ_WRITE), readWriteLock.writeLock());

		var leaf2a = new TestTaskStub(ExecutionMode.CONCURRENT, leaf2aStarted::countDown) //
				.withResourceLock(readWriteResourceLock) //
				.withName("leaf2a").withLevel(3);
		var leaf2b = new TestTaskStub(ExecutionMode.SAME_THREAD, leaf2bStarted::countDown) //
				.withName("leaf2b").withLevel(3);

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			child1Started.countDown();
			leaf2bStarted.await();
		}) //
				.withResourceLock(readOnlyResourceLock) //
				.withName("child1").withLevel(2);
		var child2 = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> {
			child1Started.await();
			requiredService().invokeAll(List.of(leaf2a, leaf2b));
		}) //
				.withName("child2").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD,
			() -> requiredService().invokeAll(List.of(child1, child2))) //
					.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(List.of(root, child1, child2, leaf2a, leaf2b)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(List.of(leaf2a, leaf2b)).map(TestTaskStub::executionThread) //
				.containsOnly(child2.executionThread);
	}

	@Test
	void executesChildrenInOrder() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(1, 1));

		var leaf1a = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf1a").withLevel(2);
		var leaf1b = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf1b").withLevel(2);
		var leaf1c = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf1c").withLevel(2);
		var leaf1d = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf1d").withLevel(2);

		List<TestTaskStub> children = Arrays.asList(leaf1d, leaf1a, leaf1b, leaf1c);
		Collections.shuffle(children);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, //
			() -> requiredService().invokeAll(children)) //
					.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(List.of(root, leaf1a, leaf1b, leaf1c, leaf1d)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);

		assertThat(children) //
				.extracting(TestTaskStub::startTime) //
				.isSorted();
	}

	@Test
	void testsAreStolenRatherThanContainers() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2, 2));

		// Execute tasks pairwise
		CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
		Executable behavior = cyclicBarrier::await;

		// With half of the leaves being containers
		var container1 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("container1").withType(CONTAINER).withLevel(2);
		var container2 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("container2").withType(CONTAINER).withLevel(2);
		var container3 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("container3").withType(CONTAINER).withLevel(2);

		// And half of the leaves being tests, to be stolen
		var test1 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("test1").withType(TEST).withLevel(2);
		var test2 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("test2").withType(TEST).withLevel(2);
		var test3 = new TestTaskStub(ExecutionMode.CONCURRENT, behavior) //
				.withName("test3").withType(TEST).withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD,
			() -> requiredService().invokeAll(List.of(container1, container2, container3, test1, test2, test3))) //
					.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(List.of(root, container1, container2, container3, test1, test2, test3)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);

		// If the last test node was stolen
		assertThat(container1.executionThread).isNotEqualTo(test3.executionThread);
		// Then it must follow that the test nodes were stolen
		assertThat(Stream.of(container1, container2, container3)) //
				.extracting(TestTaskStub::executionThread) //
				.containsOnly(container1.executionThread);
		assertThat(Stream.of(test1, test2, test3)) //
				.extracting(TestTaskStub::executionThread) //
				.containsOnly(test3.executionThread);

		assertThat(Stream.of(container1, container2, container3)) //
				.extracting(TestTaskStub::startTime) //
				.isSorted();
		assertThat(Stream.of(test1, test2, test3)) //
				.extracting(TestTaskStub::startTime) //
				.isSorted();
	}

	@Test
	void stealsDynamicChildren() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2, 2));

		var child1Started = new CountDownLatch(1);
		var child2Finished = new CountDownLatch(1);
		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			child1Started.countDown();
			child2Finished.await();
		}) //
				.withName("child1").withLevel(2);
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, child2Finished::countDown) //
				.withName("child2").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> {
			var future1 = requiredService().submit(child1);
			child1Started.await();
			var future2 = requiredService().submit(child2);
			future1.get();
			future2.get();
		}) //
				.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(Stream.of(root, child1, child2)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(child2.executionThread).isEqualTo(root.executionThread).isNotEqualTo(child1.executionThread);
	}

	@Test
	void stealsDynamicChildrenInOrder() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2, 2));

		var child1Started = new CountDownLatch(1);
		var childrenSubmitted = new CountDownLatch(1);
		var childrenFinished = new CountDownLatch(2);
		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			child1Started.countDown();
			childrenSubmitted.await();
		}) //
				.withName("child1").withLevel(2);
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, childrenFinished::countDown) //
				.withName("child2").withLevel(2);
		var child3 = new TestTaskStub(ExecutionMode.CONCURRENT, childrenFinished::countDown) //
				.withName("child3").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> {
			var future1 = requiredService().submit(child1);
			child1Started.await();
			var future2 = requiredService().submit(child2);
			var future3 = requiredService().submit(child3);
			childrenSubmitted.countDown();
			childrenFinished.await();
			future1.get();
			future2.get();
			future3.get();
		}) //
				.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(Stream.of(root, child1, child2, child3)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(List.of(child1, child2, child3)) //
				.extracting(TestTaskStub::startTime) //
				.isSorted();
	}

	@Test
	void executesDynamicChildrenInSubmitOrder() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(1, 1));

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("child1").withLevel(2);
		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("child2").withLevel(2);
		var child3 = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("child3").withLevel(2);
		var child4 = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("child3").withLevel(2);

		List<TestTaskStub> children = Arrays.asList(child1, child2, child3, child4);
		Collections.shuffle(children);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> {
			var executor = requiredService();
			var features = children.stream().map(executor::submit).toList();
			for (var future : features) {
				future.get();
			}
		}) //
				.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(Stream.of(root, child1, child2)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);

		assertThat(children) //
				.extracting(TestTaskStub::startTime) //
				.isSorted();
	}

	@Test
	void stealsNestedDynamicChildren() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2, 2));

		var barrier = new CyclicBarrier(2);

		var leaf1a = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf1a").withLevel(3);
		var leaf1b = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf1b").withLevel(3);

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			barrier.await();
			var futureA = requiredService().submit(leaf1a);
			barrier.await();
			var futureB = requiredService().submit(leaf1b);
			futureA.get();
			futureB.get();
			barrier.await();
		}) //
				.withName("child1").withLevel(2);

		var leaf2a = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf2a").withLevel(3);
		var leaf2b = new TestTaskStub(ExecutionMode.CONCURRENT) //
				.withName("leaf2b").withLevel(3);

		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			barrier.await();
			var futureA = requiredService().submit(leaf2a);
			barrier.await();
			var futureB = requiredService().submit(leaf2b);
			futureB.get();
			futureA.get();
			barrier.await();
		}) //
				.withName("child2").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> {
			var future1 = requiredService().submit(child1);
			var future2 = requiredService().submit(child2);
			future1.get();
			future2.get();
		}) //
				.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(Stream.of(root, child1, child2, leaf1a, leaf1b, leaf2a, leaf2b)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);
		assertThat(child2.executionThread).isNotEqualTo(child1.executionThread);
		assertThat(child1.executionThread).isEqualTo(leaf1a.executionThread).isEqualTo(leaf1b.executionThread);
		assertThat(child2.executionThread).isEqualTo(leaf2a.executionThread).isEqualTo(leaf2b.executionThread);
	}

	@Test
	void stealsSiblingDynamicChildrenOnly() throws Exception {
		service = new WorkerThreadPoolHierarchicalTestExecutorService(configuration(2, 3));

		var child1Started = new CountDownLatch(1);
		var child3Started = new CountDownLatch(1);
		var leaf2ASubmitted = new CountDownLatch(1);
		var leaf2AStarted = new CountDownLatch(1);

		var child1 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			child1Started.countDown();
			leaf2ASubmitted.await();
		}) //
				.withName("child1").withLevel(2);

		var leaf2a = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			leaf2AStarted.countDown();
			child3Started.await();
		}) //
				.withName("leaf1a").withLevel(3);

		var child2 = new TestTaskStub(ExecutionMode.CONCURRENT, () -> {
			var futureA = requiredService().submit(leaf2a);
			leaf2ASubmitted.countDown();
			leaf2AStarted.await();
			futureA.get();
		}) //
				.withName("child2").withType(CONTAINER).withLevel(2);

		var child3 = new TestTaskStub(ExecutionMode.CONCURRENT, child3Started::countDown) //
				.withName("child3").withLevel(2);

		var root = new TestTaskStub(ExecutionMode.SAME_THREAD, () -> {
			var future1 = requiredService().submit(child1);
			child1Started.await();
			var future2 = requiredService().submit(child2);
			var future3 = requiredService().submit(child3);
			future1.get();
			future2.get();
			future3.get();
		}) //
				.withName("root").withLevel(1);

		service.submit(root).get();

		assertThat(Stream.of(root, child1, child2, leaf2a, child3)) //
				.allSatisfy(TestTaskStub::assertExecutedSuccessfully);

		assertThat(child2.executionThread).isNotEqualTo(child1.executionThread).isNotEqualTo(child3.executionThread);
		assertThat(child1.executionThread).isNotEqualTo(child3.executionThread);
		assertThat(child1.executionThread).isEqualTo(leaf2a.executionThread);
	}

	private static ExclusiveResource exclusiveResource(LockMode lockMode) {
		return new ExclusiveResource("key", lockMode);
	}

	private WorkerThreadPoolHierarchicalTestExecutorService requiredService() {
		return requireNonNull(service);
	}

	private static ParallelExecutionConfiguration configuration(int parallelism) {
		return configuration(parallelism, 256 + parallelism);
	}

	private static ParallelExecutionConfiguration configuration(int parallelism, int maxPoolSize) {
		return new DefaultParallelExecutionConfiguration(parallelism, parallelism, maxPoolSize, parallelism, 0,
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

		@Nullable
		Instant startTime() {
			return startTime;
		}

		@Override
		public String toString() {
			return "%s @ %s".formatted(new ToStringBuilder(this).append("name", name), Integer.toHexString(hashCode()));
		}
	}

}
