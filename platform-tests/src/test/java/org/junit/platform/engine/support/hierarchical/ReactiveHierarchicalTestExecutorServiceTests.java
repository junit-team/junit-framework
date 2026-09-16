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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

/**
 * Tests for {@link ReactiveHierarchicalTestExecutorService}.
 *
 * @since 6.2
 */
@Timeout(10)
class ReactiveHierarchicalTestExecutorServiceTests {

	private final LockManager lockManager = new LockManager();

	@Test
	void runsUnrelatedTasksConcurrentlyWithoutBlocking() throws Exception {
		var nop = NopLock.INSTANCE;
		List<String> events = new CopyOnWriteArrayList<>();
		CountDownLatch release = new CountDownLatch(1);

		try (var service = new ReactiveHierarchicalTestExecutorService(2)) {
			var a = service.submit(task("a", nop, () -> {
				events.add("a-start");
				awaitLatch(release);
				events.add("a-end");
			}));
			var b = service.submit(task("b", nop, () -> events.add("b-start")));

			// "b" shares no lock with "a", so it may run concurrently even while "a" is blocked.
			awaitUpTo(() -> events.contains("a-start") && events.contains("b-start"));
			assertThat(events).contains("a-start");

			release.countDown();
			a.get();
			b.get();
		}
	}

	@Test
	void enforcesExclusiveAccessForExclusiveResource() throws Exception {
		var lock = lockManager.getLockForResource(new ExclusiveResource("shared", LockMode.READ_WRITE));
		AtomicInteger maxActive = new AtomicInteger();
		AtomicInteger active = new AtomicInteger();

		try (var service = new ReactiveHierarchicalTestExecutorService(2)) {
			List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < 20; i++) {
				futures.add(service.submit(task("task-" + i, lock, () -> {
					int now = active.incrementAndGet();
					maxActive.accumulateAndGet(now, Math::max);
					sleepQuietly(1);
					active.decrementAndGet();
				})));
			}
			for (var future : futures) {
				future.get();
			}
		}
		assertThat(maxActive.get()).isOne();
	}

	@Test
	void invokeAllExecutesAllSuppliedTasks() {
		var nop = NopLock.INSTANCE;
		var executed = new AtomicInteger();
		// @formatter:off
		List<TestTaskStub> tasks = List.of(
				new TestTaskStub(nop, executed::incrementAndGet),
				new TestTaskStub(nop, executed::incrementAndGet)
		);
		// @formatter:on

		try (var service = new ReactiveHierarchicalTestExecutorService(2)) {
			service.invokeAll(tasks);
		}
		assertEquals(2, executed.get());
	}

	private static TestTaskStub task(String name, ResourceLock lock, Executable action) {
		return new TestTaskStub(lock, action);
	}

	private static void awaitLatch(CountDownLatch latch) {
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void awaitUpTo(BooleanSupplier condition) {
		long deadline = System.currentTimeMillis() + 2_000;
		while (!condition.getAsBoolean()) {
			assertThat(System.currentTimeMillis()).as("condition not met within timeout").isLessThan(deadline);
			sleepQuietly(5);
		}
	}

	private static final class TestTaskStub implements HierarchicalTestExecutorService.TestTask {

		private final ResourceLock lock;
		private final Executable action;

		TestTaskStub(ResourceLock lock, Executable action) {
			this.lock = lock;
			this.action = action;
		}

		@Override
		public ExecutionMode getExecutionMode() {
			return ExecutionMode.CONCURRENT;
		}

		@Override
		public ResourceLock getResourceLock() {
			return lock;
		}

		@Override
		public void execute() {
			try {
				action.execute();
			}
			catch (Throwable e) {
				throw new AssertionError("task threw unexpectedly", e);
			}
		}
	}
}
