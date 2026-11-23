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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;

/**
 * A reactive, bounded concurrency permit.
 *
 * <p>Replaces the blocking {@code Semaphore}-based worker lease for the
 * non-blocking execution path. Acquiring a permit returns a
 * {@link CompletionStage} that completes only once a permit becomes available;
 * no thread is ever parked while waiting. Releasing a permit hands it to the
 * longest-waiting acquirer in FIFO order.
 *
 * <p>The {@link CompletionStage} is used purely as a signal that a permit is
 * held; its payload is intentionally ignored.
 *
 * @since 6.2
 */
@API(status = EXPERIMENTAL, since = "6.2")
final class AsyncResourcePermit {

	private final int maxPermits;
	private final ReentrantLock lock = new ReentrantLock();
	private int availablePermits;
	private final Deque<CompletableFuture<Permit>> waitingPermits = new ArrayDeque<>();

	/**
	 * A token held by the current owner of a permit. Must be released exactly
	 * once via {@link Permit#release()}.
	 */
	interface Permit {

		/**
		 * Release this permit, handing it to the longest-waiting acquirer if
		 * any.
		 */
		void release();
	}

	/**
	 * Create a permit gate allowing up to {@code maxPermits} concurrent holders.
	 *
	 * @param maxPermits the maximum number of permits to allow; must be positive
	 * @throws org.junit.platform.commons.PreconditionViolationException if
	 * {@code maxPermits} is not positive
	 */
	AsyncResourcePermit(int maxPermits) {
		Preconditions.condition(maxPermits > 0,
			"maxPermits must be a positive number of permits, but was " + maxPermits);
		this.maxPermits = maxPermits;
		this.availablePermits = maxPermits;
	}

	int availablePermits() {
		lock.lock();
		try {
			return availablePermits;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Return the maximum number of permits this gate allows.
	 */
	int maxPermits() {
		return maxPermits;
	}

	/**
	 * Acquire a permit asynchronously.
	 *
	 * <p>The returned stage completes (a) immediately if a permit is available
	 * or (b) when a previously held permit is released and this acquirer reaches
	 * the front of the FIFO queue. The {@link Permit} token is the completion
	 * value of the stage.
	 */
	CompletionStage<Permit> acquire() {
		lock.lock();
		try {
			// Check availability and register a waiter atomically with respect to release.
			if (waitingPermits.isEmpty() && availablePermits > 0) {
				availablePermits--;
				return CompletableFuture.completedFuture(new Lease(this));
			}
			final var pending = new CompletableFuture<Permit>();
			waitingPermits.addLast(pending);
			return pending;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Attempt to acquire a permit without queuing.
	 *
	 * @return a {@link Permit} if acquired, else {@code null}
	 */
	@org.jspecify.annotations.Nullable
	Permit tryAcquire() {
		lock.lock();
		try {
			// Do not let a new acquirer jump ahead of already-queued waiters.
			if (!waitingPermits.isEmpty() || availablePermits == 0) {
				return null;
			}
			availablePermits--;
			return new Lease(this);
		}
		finally {
			lock.unlock();
		}
	}

	private static final class Lease implements Permit {

		private final AsyncResourcePermit gate;
		private final AtomicBoolean released = new AtomicBoolean();

		Lease(AsyncResourcePermit gate) {
			this.gate = gate;
		}

		@Override
		public void release() {
			if (released.compareAndSet(false, true)) {
				gate.release();
			}
		}
	}

	private void release() {
		while (true) {
			final CompletableFuture<Permit> next;
			lock.lock();
			try {
				next = waitingPermits.pollFirst();
				if (next == null) {
					availablePermits++;
					return;
				}
			}
			finally {
				lock.unlock();
			}
			// Run callbacks outside the lock. If cancellation wins the handoff,
			// keep the permit and try the next waiter instead of losing capacity.
			if (next.complete(new Lease(this))) {
				return;
			}
		}
	}
}
