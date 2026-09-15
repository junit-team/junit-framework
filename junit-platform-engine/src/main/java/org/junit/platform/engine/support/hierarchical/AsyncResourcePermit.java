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
		synchronized (this) {
			return availablePermits;
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
		Permit permit = tryAcquire();
		if (permit != null) {
			return CompletableFuture.completedFuture(permit);
		}
		CompletableFuture<Permit> pending = new CompletableFuture<>();
		synchronized (this) {
			waitingPermits.addLast(pending);
		}
		return pending;
	}

	/**
	 * Attempt to acquire a permit without queuing.
	 *
	 * @return a {@link Permit} if acquired, else {@code null}
	 */
	@org.jspecify.annotations.Nullable
	Permit tryAcquire() {
		synchronized (this) {
			// Do not let a new acquirer jump ahead of already-queued waiters.
			if (!waitingPermits.isEmpty() || availablePermits == 0) {
				return null;
			}
			availablePermits--;
			return new Lease(this);
		}
	}

	private static final class Lease implements Permit {

		private final AsyncResourcePermit gate;
		private volatile boolean released;

		Lease(AsyncResourcePermit gate) {
			this.gate = gate;
		}

		@Override
		public void release() {
			if (!released) {
				released = true;
				gate.release();
			}
		}
	}

	private void release() {
		CompletableFuture<Permit> next;
		synchronized (this) {
			next = waitingPermits.pollFirst();
			if (next == null) {
				availablePermits++;
			}
		}
		if (next != null) {
			// Hand the permit to the longest-waiting acquirer directly.
			next.complete(new Lease(this));
		}
	}
}
