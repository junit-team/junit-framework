/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.timeout;

import static java.util.Objects.requireNonNullElse;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;

import java.io.Serial;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.platform.commons.JUnitException;

/**
 * Internal utilities for executing code with a preemptive timeout.
 *
 * @since 6.0
 */
@API(status = INTERNAL, since = "6.0")
public class PreemptiveTimeoutUtils {

	/**
	 * <em>Assert</em> that execution of the supplied {@code supplier}
	 * completes before the given {@code timeout} is exceeded.
	 *
	 * <p>See the {@linkplain Assertions Preemptive Timeouts} section of the
	 * class-level Javadoc for further details.
	 *
	 * <p>If the assertion passes then the {@code supplier}'s result is returned.
	 *
	 * <p>In the case the assertion does not pass, the supplied
	 * {@link TimeoutFailureFactory} is invoked to create an exception which is
	 * then thrown.
	 *
	 * <p>If necessary, the failure message will be retrieved lazily from the
	 * supplied {@code messageSupplier}.
	 */
	public static <T extends @Nullable Object, E extends Throwable> T executeWithPreemptiveTimeout(Duration timeout,
			ThrowingSupplier<T> supplier, @Nullable Supplier<@Nullable String> messageSupplier,
			TimeoutFailureFactory<E> failureFactory) throws E {

		AtomicReference<Thread> threadReference = new AtomicReference<>();
		ExecutorService executorService = Executors.newSingleThreadExecutor(new TimeoutThreadFactory());

		try {
			Future<T> future = submitTask(supplier, threadReference, executorService);
			return resolveFutureAndHandleException(future, timeout, messageSupplier, threadReference::get,
				failureFactory);
		}
		finally {
			executorService.shutdownNow();
		}
	}

	private static <T extends @Nullable Object> Future<T> submitTask(ThrowingSupplier<T> supplier,
			AtomicReference<Thread> threadReference, ExecutorService executorService) {
		return executorService.submit(() -> {
			try {
				threadReference.set(Thread.currentThread());
				return supplier.get();
			}
			catch (Throwable throwable) {
				throw throwAsUncheckedException(throwable);
			}
		});
	}

	private static <T extends @Nullable Object, E extends Throwable> T resolveFutureAndHandleException(Future<T> future,
			Duration timeout, @Nullable Supplier<@Nullable String> messageSupplier,
			Supplier<@Nullable Thread> threadSupplier, TimeoutFailureFactory<E> failureFactory)
			throws E, RuntimeException {
		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException ex) {
			Thread thread = threadSupplier.get();
			ExecutionTimeoutException cause = null;
			if (thread != null) {
				cause = new ExecutionTimeoutException("Execution timed out in thread " + thread.getName());
				cause.setStackTrace(thread.getStackTrace());
			}
			throw failureFactory.createTimeoutFailure(timeout, messageSupplier, cause, thread);
		}
		catch (ExecutionException ex) {
			throw throwAsUncheckedException(requireNonNullElse(ex.getCause(), ex));
		}
		catch (Throwable ex) {
			throw throwAsUncheckedException(ex);
		}
	}

	private PreemptiveTimeoutUtils() {
	}

	/**
	 * Factory for timeout failures.
	 *
	 * @param <T> The type of error or exception created
	 * @since 5.9.1
	 * @see PreemptiveTimeoutUtils#executeWithPreemptiveTimeout(Duration, ThrowingSupplier, Supplier, TimeoutFailureFactory)
	 */
	public interface TimeoutFailureFactory<T extends Throwable> {

		/**
		 * Create a failure for the given timeout, message, and cause.
		 *
		 * @return timeout failure; never {@code null}
		 */
		T createTimeoutFailure(Duration timeout, @Nullable Supplier<@Nullable String> messageSupplier,
				@Nullable Throwable cause, @Nullable Thread testThread);
	}

	private static class ExecutionTimeoutException extends JUnitException {

		@Serial
		private static final long serialVersionUID = 1L;

		ExecutionTimeoutException(String message) {
			super(message);
		}

	}

	/**
	 * The thread factory used for preemptive timeout.
	 *
	 * <p>The factory creates threads with meaningful names, helpful for debugging
	 * purposes.
	 */
	private static class TimeoutThreadFactory implements ThreadFactory {

		private static final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "junit-timeout-thread-" + threadNumber.getAndIncrement());
		}

	}

}
