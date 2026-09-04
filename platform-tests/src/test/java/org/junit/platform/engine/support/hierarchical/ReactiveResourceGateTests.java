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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;

/**
 * Tests for {@link ReactiveResourceGate}.
 *
 * @since 6.2
 */
class ReactiveResourceGateTests {

	private final ReactiveResourceGate gate = new ReactiveResourceGate();

	@Test
	void acquiresUncontendedExclusiveLock() {
		var lock = lockOf("key", LockMode.READ_WRITE);

		var acquired = gate.acquire(lock).toCompletableFuture();

		assertThat(acquired.join()).isSameAs(lock);
		gate.release(lock);
	}

	@Test
	void holdsExclusiveLockUntilReleased() {
		var lock = lockOf("key", LockMode.READ_WRITE);

		var first = gate.acquire(lock).toCompletableFuture();
		var second = gate.acquire(lockOf("key", LockMode.READ_WRITE)).toCompletableFuture();

		// The second acquisition must not complete while the first is held.
		assertThat(first).isDone();
		assertThat(second).isNotDone();

		gate.release(lock);

		// Releasing hands the lock to the waiting (FIFO) acquirer.
		assertThat(second).isDone();
		gate.release(lockOf("key", LockMode.READ_WRITE));
	}

	@Test
	void servicesIndependentResourcesConcurrently() {
		var a = lockOf("a", LockMode.READ_WRITE);
		var b = lockOf("b", LockMode.READ_WRITE);

		var acquiredA = gate.acquire(a).toCompletableFuture();
		var acquiredB = gate.acquire(b).toCompletableFuture();

		assertThat(acquiredA).isDone();
		assertThat(acquiredB).isDone();

		gate.release(a);
		gate.release(b);
	}

	@Test
	void allowsConcurrentReadHolders() {
		var readA = gate.acquire(lockOf("key", LockMode.READ)).toCompletableFuture();
		var readB = gate.acquire(lockOf("key", LockMode.READ)).toCompletableFuture();

		assertThat(readA).isDone();
		assertThat(readB).isDone();
	}

	@Test
	void writerDoesNotProceedWhileReadersHold() {
		var reader = lockOf("key", LockMode.READ);
		gate.acquire(reader);
		gate.acquire(lockOf("key", LockMode.READ));

		var writer = gate.acquire(lockOf("key", LockMode.READ_WRITE)).toCompletableFuture();

		assertThat(writer).isNotDone();
	}

	@Test
	void fifoHandoffUnderContention() {
		// Acquire the key exclusively, then enqueue two waiters.
		var holder = lockOf("key", LockMode.READ_WRITE);
		gate.acquire(holder);

		var first = gate.acquire(lockOf("key", LockMode.READ_WRITE)).toCompletableFuture();
		var second = gate.acquire(lockOf("key", LockMode.READ_WRITE)).toCompletableFuture();

		List<CompletableFuture<?>> completionOrder = new ArrayList<>();
		first.whenComplete((a, b) -> completionOrder.add(first));
		second.whenComplete((a, b) -> completionOrder.add(second));

		gate.release(holder);
		// After the first release, the first waiter takes the key.
		assertThat(first).isDone();
		assertThat(second).isNotDone();

		gate.release(lockOf("key", LockMode.READ_WRITE));
		assertThat(second).isDone();
		assertThat(completionOrder).containsExactly(first, second);
	}

	@Test
	void completesNopLockImmediately() {
		var nop = NopLock.INSTANCE;

		var acquired = gate.acquire(nop).toCompletableFuture();

		assertThat(acquired).isDone();
		assertThat(acquired.join()).isSameAs(nop);
	}

	private static ResourceLock lockOf(String key, LockMode mode) {
		var lock = new ReentrantLock();
		return new SingleLock(new ExclusiveResource(key, mode), lock);
	}
}
