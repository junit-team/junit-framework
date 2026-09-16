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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
