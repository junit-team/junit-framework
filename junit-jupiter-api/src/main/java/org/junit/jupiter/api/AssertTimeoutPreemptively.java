/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.timeout.DurationUtils.formatDurationInFractionalMs;
import static org.junit.jupiter.api.timeout.DurationUtils.isPositiveAndRepresentableInNanos;
import static org.junit.jupiter.api.timeout.PreemptiveTimeoutUtils.executeWithPreemptiveTimeout;

import java.time.Duration;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.platform.commons.util.Preconditions;
import org.opentest4j.AssertionFailedError;

/**
 * {@code AssertTimeout} is a collection of utility methods that support asserting
 * the execution of the code under test did not take longer than the timeout duration
 * using a preemptive approach.
 *
 * @since 5.9.1
 */
class AssertTimeoutPreemptively {

	static void assertTimeoutPreemptively(Duration timeout, Executable executable) {
		Preconditions.notNull(timeout, () -> "timeout must not be null");
		Preconditions.condition(isPositiveAndRepresentableInNanos(timeout),
			() -> "timeout must be positive and less than approximately 292 years (2^63 nanoseconds)");
		Preconditions.notNull(executable, () -> "executable must not be null");
		assertTimeoutPreemptively(timeout, executable, (String) null);
	}

	@SuppressWarnings("NullAway")
	static void assertTimeoutPreemptively(Duration timeout, Executable executable, @Nullable String message) {
		assertTimeoutPreemptively(timeout, () -> {
			executable.execute();
			return null;
		}, message);
	}

	@SuppressWarnings("NullAway")
	static void assertTimeoutPreemptively(Duration timeout, Executable executable,
			Supplier<@Nullable String> messageSupplier) {
		assertTimeoutPreemptively(timeout, () -> {
			executable.execute();
			return null;
		}, messageSupplier);
	}

	static <T extends @Nullable Object> T assertTimeoutPreemptively(Duration timeout, ThrowingSupplier<T> supplier) {
		Preconditions.notNull(timeout, () -> "timeout must not be null");
		Preconditions.condition(isPositiveAndRepresentableInNanos(timeout),
			() -> "timeout must be positive and less than approximately 292 years (2^63 nanoseconds)");
		Preconditions.notNull(supplier, () -> "supplier must not be null");
		return executeWithPreemptiveTimeout(timeout, supplier, null, AssertTimeoutPreemptively::createAssertionFailure);
	}

	static <T extends @Nullable Object> T assertTimeoutPreemptively(Duration timeout, ThrowingSupplier<T> supplier,
			@Nullable String message) {
		Preconditions.notNull(timeout, () -> "timeout must not be null");
		Preconditions.condition(isPositiveAndRepresentableInNanos(timeout),
			() -> "timeout must be positive and less than approximately 292 years (2^63 nanoseconds)");
		Preconditions.notNull(supplier, () -> "supplier must not be null");
		return executeWithPreemptiveTimeout(timeout, supplier, message == null ? null : () -> message,
			AssertTimeoutPreemptively::createAssertionFailure);
	}

	static <T extends @Nullable Object> T assertTimeoutPreemptively(Duration timeout, ThrowingSupplier<T> supplier,
			@Nullable Supplier<@Nullable String> messageSupplier) {
		Preconditions.notNull(timeout, () -> "timeout must not be null");
		Preconditions.condition(isPositiveAndRepresentableInNanos(timeout),
			() -> "timeout must be positive and less than approximately 292 years (2^63 nanoseconds)");
		Preconditions.notNull(supplier, () -> "supplier must not be null");
		return executeWithPreemptiveTimeout(timeout, supplier, messageSupplier,
			AssertTimeoutPreemptively::createAssertionFailure);
	}

	private static AssertionFailedError createAssertionFailure(Duration timeout,
			@Nullable Supplier<@Nullable String> messageSupplier, @Nullable Throwable cause, @Nullable Thread thread) {
		return assertionFailure() //
				.message(messageSupplier) //
				.reason("execution timed out after %s".formatted(formatDurationInFractionalMs(timeout))) //
				.cause(cause) //
				.trimStacktrace(Assertions.class) //
				.build();
	}

	private AssertTimeoutPreemptively() {
	}

}
