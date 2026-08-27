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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncTestExecution}.
 *
 * @since 6.0
 */
class AsyncTestExecutionTests {

	@Test
	void bridgeCompletesSuccessfullyForBlockingExecution() {
		AtomicBoolean executed = new AtomicBoolean();
		var stage = AsyncTestExecution.bridge(() -> executed.set(true));

		assertThat(completedNormally(stage)).isTrue();
		assertThat(executed).isTrue();
	}

	@Test
	void bridgeCompletesExceptionallyForBlockingFailure() {
		RuntimeException expected = new RuntimeException("boom");
		var stage = AsyncTestExecution.bridge(() -> {
			throw expected;
		});

		assertThatThrownBy(stage.toCompletableFuture()::join).hasRootCause(expected);
	}

	@Test
	void bridgeCompletesExceptionallyForUnrecoverableError() {
		OutOfMemoryError expected = new OutOfMemoryError("boom");
		var stage = AsyncTestExecution.bridge(() -> {
			throw expected;
		});

		assertThatThrownBy(stage.toCompletableFuture()::join).hasRootCause(expected);
	}

	@Test
	void synchronousRunsOnCallingThread() {
		Thread caller = Thread.currentThread();
		AtomicReference<Thread> executionThread = new AtomicReference<>();
		var stage = AsyncTestExecution.synchronous(() -> executionThread.set(Thread.currentThread()));

		assertThat(completedNormally(stage)).isTrue();
		assertThat(executionThread).hasValue(caller);
	}

	@Test
	void synchronousCompletesExceptionallyForFailure() {
		IllegalStateException expected = new IllegalStateException("boom");
		var stage = AsyncTestExecution.synchronous(() -> {
			throw expected;
		});

		assertThatThrownBy(stage.toCompletableFuture()::join).hasRootCause(expected);
	}

	@Test
	void synchronousRethrowsUnrecoverableErrorSynchronously() {
		OutOfMemoryError expected = new OutOfMemoryError("boom");

		assertThatThrownBy(() -> AsyncTestExecution.synchronous(() -> {
			throw expected;
		})).isSameAs(expected);
	}

	private boolean completedNormally(CompletionStage<?> stage) {
		try {
			stage.toCompletableFuture().join();
			return true;
		}
		catch (Throwable throwable) {
			return false;
		}
	}
}
