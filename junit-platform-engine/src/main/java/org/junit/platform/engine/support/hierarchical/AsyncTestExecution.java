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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * Support for bridging blocking execution into the non-blocking
 * {@link CompletionStage} world.
 *
 * <p>The returned stage is intentionally type-agnostic: it is used only as a
 * promise of termination of the wrapped execution, not as a container of a
 * value. Callers MUST NOT rely on the payload carried by the completed stage.
 *
 * <p>Any {@link Throwable} (including {@link Exception} and {@link Error})
 * thrown by the supplied execution results in a stage completed exceptionally
 * with that throwable rather than a gracefully completed stage.
 *
 * <p>{@linkplain org.junit.platform.commons.util.UnrecoverableExceptions
 * Unrecoverable} throwables are rethrown synchronously by
 * {@link #synchronous(ThrowingRunnable)} (since it runs on the calling thread)
 * and are carried by the failed stage of {@link #bridge(ThrowingRunnable)}
 * (since it runs on a pool thread).
 *
 * @since 6.0
 */
@API(status = EXPERIMENTAL, since = "6.0")
public final class AsyncTestExecution {

	private AsyncTestExecution() {
		/* no-op */
	}

	/**
	 * A runnable that may throw any throwable.
	 */
	@FunctionalInterface
	public interface ThrowingRunnable {
		void run() throws Throwable;
	}

	/**
	 * Run the supplied blocking {@code execution} on the common pool and return
	 * a stage that completes when the execution terminates.
	 *
	 * <p>If the execution throws, the returned stage completes exceptionally
	 * with that throwable. On normal return the stage completes successfully
	 * (its payload is irrelevant and must be ignored).
	 *
	 * @param execution the blocking execution to run
	 * @return a completion stage signaling termination of the execution
	 * @throws org.junit.platform.commons.PreconditionViolationException if
	 * {@code execution} is {@code null}
	 */
	public static CompletionStage<?> bridge(ThrowingRunnable execution) {
		Preconditions.notNull(execution, "execution must not be null");
		CompletableFuture<Object> result = new CompletableFuture<>();
		// execute() returns void, so the common pool is used without leaving an
		// ignored Future behind. Any throwable (Exception or Error) produces a
		// stage in a failed state so the throwable is never lost; a normal return
		// produces a successful stage whose payload is a pure signal and MUST be
		// ignored by callers.
		ForkJoinPool.commonPool().execute(() -> {
			try {
				execution.run();
				result.complete(null);
			}
			catch (Throwable throwable) {
				// The stage is completed exceptionally so the throwable is not
				// lost, even for unrecoverable errors (the caller may rethrow
				// them when awaiting the stage).
				result.completeExceptionally(throwable);
			}
		});
		return result;
	}

	/**
	 * Run the supplied {@code execution} synchronously on the calling thread and
	 * return an already-settled stage.
	 *
	 * <p>Unlike {@link #bridge(ThrowingRunnable)} this method does not offload
	 * the execution to a pool; it is intended for default {@code *Async}
	 * implementations that must preserve the calling thread's identity and
	 * ordering guarantees while still exposing a {@link CompletionStage} seam.
	 *
	 * <p>If the execution throws a recoverable throwable, the returned stage is
	 * already completed exceptionally with that throwable. An
	 * {@linkplain org.junit.platform.commons.util.UnrecoverableExceptions
	 * unrecoverable} throwable is rethrown synchronously so it propagates up the
	 * calling stack unchanged. On normal return the stage is already completed
	 * successfully (its payload is irrelevant and must be ignored).
	 *
	 * @param execution the execution to run synchronously
	 * @return an already-settled completion stage signaling termination
	 * @throws org.junit.platform.commons.PreconditionViolationException if
	 * {@code execution} is {@code null}
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static CompletionStage<?> synchronous(ThrowingRunnable execution) {
		Preconditions.notNull(execution, "execution must not be null");
		CompletableFuture<Object> result = new CompletableFuture<>();
		try {
			execution.run();
			result.complete(null);
		}
		catch (Throwable throwable) {
			// Unrecoverable errors must not be trapped in a CompletableFuture's
			// completion machinery; rethrow them synchronously so they propagate
			// up the calling stack unchanged (matching the behavior of the
			// blocking implementation). Recoverable throwables produce a stage in
			// a failed state.
			UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
			result.completeExceptionally(throwable);
		}
		return result;
	}

	/**
	 * Run the supplied {@code execution} synchronously on the calling thread
	 * and return a stage that is already completed with its result.
	 *
	 * <p>Unlike {@link #synchronous(ThrowingRunnable)}, the produced stage
	 * carries the {@code execution}'s result, which is useful for seeding the
	 * reactive world with a value (e.g. the context produced by a node).
	 *
	 * @param execution the execution to run synchronously
	 * @param <T> the result type
	 * @return an already-settled completion stage carrying the result; never
	 * {@code null}
	 * @throws org.junit.platform.commons.PreconditionViolationException if
	 * {@code execution} is {@code null}
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	public static <T extends @Nullable Object> CompletionStage<T> synchronousResult(ThrowingSupplier<T> execution) {
		Preconditions.notNull(execution, "execution must not be null");
		CompletableFuture<T> result = new CompletableFuture<>();
		try {
			result.complete(execution.get());
		}
		catch (Throwable throwable) {
			// Unrecoverable errors must not be trapped in a CompletableFuture's
			// completion machinery; rethrow them synchronously so they propagate
			// up the calling stack unchanged. Recoverable throwables produce a
			// stage in a failed state.
			UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
			result.completeExceptionally(throwable);
		}
		return result;
	}

	/**
	 * A supplier that returns a result and may throw any throwable.
	 *
	 * @param <T> the result type
	 */
	@FunctionalInterface
	public interface ThrowingSupplier<T extends @Nullable Object> {

		@Nullable
		T get() throws Throwable;
	}

}
