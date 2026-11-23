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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.support.hierarchical.AsyncResourcePermit.Permit;

/**
 * Tests for {@link AsyncResourcePermit}.
 *
 * @since 6.2
 */
class AsyncResourcePermitTests {

	@Test
	void acquiresUpToMaxPermitsImmediately() {
		var permit = new AsyncResourcePermit(2);

		var first = permit.acquire().toCompletableFuture();
		var second = permit.acquire().toCompletableFuture();

		assertThat(first).isDone();
		assertThat(second).isDone();
		assertThat(permit.availablePermits()).isZero();
	}

	@Test
	void queuesAcquirerWhenExhaustedUntilRelease() {
		var permit = new AsyncResourcePermit(1);

		var first = permit.acquire().toCompletableFuture();
		var thirdBeyond = permit.acquire().toCompletableFuture();

		assertThat(first).isDone();
		assertThat(thirdBeyond).isNotDone();

		var held = first.join();
		assertThat(held).isNotNull();
		held.release();

		assertThat(thirdBeyond).isDone();
	}

	@Test
	void handsPermitsToWaitersNotBackToPool() {
		var permit = new AsyncResourcePermit(1);

		Permit first = permit.acquire().toCompletableFuture().join();
		var second = permit.acquire().toCompletableFuture();

		first.release();

		// The released permit goes to the waiting acquirer, so a new acquirer
		// must still wait until the second acquirer releases.
		var third = permit.acquire().toCompletableFuture();
		assertThat(second).isDone();
		assertThat(third).isNotDone();

		second.join().release();
		assertThat(third).isDone();
	}

	@Test
	void throwsForNonPositiveMaxPermits() {
		assertThatThrownBy(() -> new AsyncResourcePermit(0)).isInstanceOf(PreconditionViolationException.class);
	}

	@Test
	void permitReleasedAtMostOnce() {
		var permit = new AsyncResourcePermit(1);

		Permit held = permit.acquire().toCompletableFuture().join();
		held.release();
		held.release();

		assertThat(permit.availablePermits()).isOne();
	}

	@Test
	void skipsCancelledWaitersWithoutLosingPermits() {
		final var permit = new AsyncResourcePermit(1);
		final var held = permit.acquire().toCompletableFuture().join();
		final var cancelled = permit.acquire().toCompletableFuture();
		final var next = permit.acquire().toCompletableFuture();

		assertThat(cancelled.cancel(false)).isTrue();
		assertThat(permit.tryAcquire()).isNull();
		held.release();

		assertThat(cancelled).isCancelled();
		assertThat(next).isDone();
		assertThat(permit.availablePermits()).isZero();
		next.join().release();
		assertThat(permit.availablePermits()).isOne();
	}

	@Test
	void returnsPermitToPoolWhenAllWaitersAreCancelled() {
		final var permit = new AsyncResourcePermit(1);
		final var held = permit.acquire().toCompletableFuture().join();
		final var first = permit.acquire().toCompletableFuture();
		final var second = permit.acquire().toCompletableFuture();
		first.cancel(false);
		second.cancel(false);

		held.release();

		assertThat(permit.availablePermits()).isOne();
		final var next = permit.tryAcquire();
		assertThat(next).isNotNull();
		next.release();
		assertThat(permit.availablePermits()).isOne();
	}

	@Test
	void keepsConcurrencyBoundedAcrossQueuedHandoffs() {
		final var permit = new AsyncResourcePermit(3);
		final List<CompletableFuture<Permit>> acquisitions = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			acquisitions.add(permit.acquire().toCompletableFuture());
		}

		for (int i = 0; i < acquisitions.size(); i++) {
			// Three holders at most; each release admits exactly one queued acquirer.
			for (int j = i; j < acquisitions.size(); j++) {
				assertThat(acquisitions.get(j).isDone()).isEqualTo(j < i + permit.maxPermits());
			}
			assertThat(permit.availablePermits()).isEqualTo(Math.max(0, i + permit.maxPermits() - acquisitions.size()));
			acquisitions.get(i).join().release();
		}
		assertThat(permit.availablePermits()).isEqualTo(permit.maxPermits());
	}

	@Test
	void completesWaitersOutsideTheLock() {
		final var permit = new AsyncResourcePermit(1);
		final var held = permit.acquire().toCompletableFuture().join();
		try (final var executor = Executors.newSingleThreadExecutor()) {
			final var callback = permit.acquire().thenAccept(next -> {
				// A different thread must be able to inspect the gate while this callback runs.
				assertDoesNotThrow(
					() -> assertThat(executor.submit(permit::availablePermits).get(5, TimeUnit.SECONDS)).isZero());
				next.release();
			}).toCompletableFuture();

			held.release();

			assertThat(callback).isDone();
			callback.join();
			assertThat(permit.availablePermits()).isOne();
		}
	}

	@Test
	void concurrentAcquireAndReleaseDoNotLoseWakeups() throws Exception {
		final var permit = new AsyncResourcePermit(1);
		try (final var executor = Executors.newFixedThreadPool(2)) {
			for (int i = 0; i < 200; i++) {
				final var held = permit.acquire().toCompletableFuture().join();
				final var start = new CountDownLatch(1);
				final var acquiring = executor.submit(() -> {
					start.await();
					return permit.acquire().toCompletableFuture();
				});
				final var releasing = executor.submit(() -> {
					start.await();
					held.release();
					return null;
				});
				start.countDown();
				final var acquired = acquiring.get(5, TimeUnit.SECONDS);
				releasing.get(5, TimeUnit.SECONDS);

				// Both racing operations have returned: no additional release may be needed.
				assertThat(acquired).isDone();
				assertThat(permit.availablePermits()).isZero();
				acquired.join().release();
				assertThat(permit.availablePermits()).isOne();
			}
		}
	}

	@Test
	void concurrentCancellationAndReleaseKeepPermitBalance() throws Exception {
		final var permit = new AsyncResourcePermit(1);
		try (final var executor = Executors.newFixedThreadPool(2)) {
			for (int i = 0; i < 200; i++) {
				final var held = permit.acquire().toCompletableFuture().join();
				final var pending = permit.acquire().toCompletableFuture();
				final var start = new CountDownLatch(1);
				final var cancelling = executor.submit(() -> {
					start.await();
					final var cancelled = pending.cancel(false);
					held.release();
					return cancelled;
				});
				final var releasing = executor.submit(() -> {
					start.await();
					held.release();
					held.release();
					return null;
				});
				start.countDown();
				final var cancelled = cancelling.get(5, TimeUnit.SECONDS);
				releasing.get(5, TimeUnit.SECONDS);

				assertThat(pending).isDone();
				if (!cancelled) {
					pending.join().release();
				}
				assertThat(permit.availablePermits()).isOne();
			}
		}
	}

	@Test
	void isFifoFairUnderContention() {
		var permit = new AsyncResourcePermit(1);
		Permit first = permit.acquire().toCompletableFuture().join();

		List<CompletableFuture<Permit>> order = new ArrayList<>();
		var a = permit.acquire().toCompletableFuture();
		var b = permit.acquire().toCompletableFuture();
		a.whenComplete((p, t) -> order.add(a));
		b.whenComplete((p, t) -> order.add(b));

		first.release();
		// The first released permit goes to the head of the FIFO (a).
		assertThat(a).isDone();
		assertThat(b).isNotDone();

		a.join().release();
		assertThat(b).isDone();
		assertThat(order).containsExactly(a, b);
	}
}
