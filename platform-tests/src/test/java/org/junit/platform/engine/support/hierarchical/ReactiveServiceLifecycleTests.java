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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

@Timeout(15)
class ReactiveServiceLifecycleTests {

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void fatalFailuresReleaseLockAndPermitBeforeCompletionIsObserved(boolean throwDirectly) throws Exception {
		final var fatal = new OutOfMemoryError("test fatal propagation");
		final var entered = new CompletableFuture<Void>();
		final var body = new CompletableFuture<Void>();
		final var lock = new LockManager().getLockForResource(new ExclusiveResource("shared", LockMode.READ_WRITE));
		final var worker = Executors.newFixedThreadPool(2);
		try (final var service = new ReactiveHierarchicalTestExecutorService(1, worker)) {
			final var first = service.submitAsync(new AsyncTask(lock, () -> {
				entered.complete(null);
				if (throwDirectly) {
					throw fatal;
				}
				return body;
			})).toCompletableFuture();
			final var observed = first.handle((__, failure) -> {
				assertThat(failure == fatal ? failure : failure.getCause()).isSameAs(fatal);
				try {
					// Completion callbacks must be able to reuse both resources immediately.
					service.submit(new AsyncTask(lock, () -> completedFuture(null))).get(3, SECONDS);
				}
				catch (Exception exception) {
					throw new AssertionError("Resources were not released before task completion", exception);
				}
				return null;
			});
			entered.get(5, SECONDS);
			body.completeExceptionally(fatal);
			observed.get(5, SECONDS);
		}
	}

	@Test
	void successfulCompletionAlsoReleasesResourcesBeforeCallingDependents() throws Exception {
		final var body = new CompletableFuture<Void>();
		final var entered = new CompletableFuture<Void>();
		final var lock = new LockManager().getLockForResource(new ExclusiveResource("shared", LockMode.READ_WRITE));
		final var worker = Executors.newFixedThreadPool(2);
		try (final var service = new ReactiveHierarchicalTestExecutorService(1, worker)) {
			final var first = service.submitAsync(new AsyncTask(lock, () -> {
				entered.complete(null);
				return body;
			})).toCompletableFuture();
			final var observed = first.thenRun(() -> {
				try {
					service.submit(new AsyncTask(lock, () -> completedFuture(null))).get(3, SECONDS);
				}
				catch (Exception exception) {
					throw new AssertionError("Resources were not released before task completion", exception);
				}
			});
			entered.get(5, SECONDS);
			body.complete(null);
			observed.get(5, SECONDS);
		}
	}

	@Test
	void rejectedSubmissionDoesNotLeakTheOnlyPermit() {
		final var worker = Executors.newSingleThreadExecutor();
		worker.shutdown();
		try (final var service = new ReactiveHierarchicalTestExecutorService(1, worker)) {
			for (int index = 0; index < 2; index++) {
				final var result = service.submitAsync(new AsyncTask(NopLock.INSTANCE, () -> failedFuture(
					new AssertionError("Rejected worker must not run the body")))).toCompletableFuture();
				final var failure = assertThrows(ExecutionException.class, () -> result.get(5, SECONDS));
				assertThat(failure).hasRootCauseInstanceOf(RejectedExecutionException.class);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void aggregateAlwaysPropagatesFatalSiblingFailure(boolean fatalFirst) throws Exception {
		final var fatal = new OutOfMemoryError("fatal sibling");
		final var fatalBody = new CompletableFuture<Void>();
		final var ordinary = new AsyncTask(NopLock.INSTANCE, () -> failedFuture(
			new IllegalStateException("recoverable sibling")));
		final var failingFatally = new AsyncTask(NopLock.INSTANCE, () -> fatalBody);
		final var tasks = fatalFirst ? List.of(failingFatally, ordinary) : List.of(ordinary, failingFatally);
		try (final var service = new ReactiveHierarchicalTestExecutorService(2)) {
			final var result = service.invokeAllAsync(tasks).toCompletableFuture();
			assertThat(result.isDone()).isFalse();
			fatalBody.completeExceptionally(fatal);
			final var failure = assertThrows(ExecutionException.class, () -> result.get(5, SECONDS));
			assertThat(failure).hasRootCause(fatal);
		}
	}

	@NullMarked
	private record AsyncTask(ResourceLock resourceLock, Supplier<CompletionStage<?>> action) implements TestTask {

		@Override
		public ExecutionMode getExecutionMode() {
			return ExecutionMode.CONCURRENT;
		}

		@Override
		public ResourceLock getResourceLock() {
			return resourceLock;
		}

		@Override
		public void execute() {
			throw new AssertionError("Reactive service must invoke executeAsync");
		}

		@Override
		public CompletionStage<?> executeAsync() {
			return action.get();
		}
	}
}
