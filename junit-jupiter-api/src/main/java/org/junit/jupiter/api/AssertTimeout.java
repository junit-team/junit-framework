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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.timeout.TimeoutUtils.isRepresentableInNanos;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;

import java.time.Duration;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code AssertTimeout} is a collection of utility methods that support asserting
 * the execution of the code under test did not take longer than the timeout duration.
 *
 * @since 5.0
 */
class AssertTimeout {

	private AssertTimeout() {
		/* no-op */
	}

	static void assertTimeout(Duration timeout, Executable executable) {
		assertTimeout(timeout, executable, (String) null);
	}

	static void assertTimeout(Duration timeout, Executable executable, @Nullable String message) {
		AssertTimeout.<@Nullable Object> assertTimeout(timeout, () -> {
			executable.execute();
			return null;
		}, message);
	}

	static void assertTimeout(Duration timeout, Executable executable, Supplier<@Nullable String> messageSupplier) {
		AssertTimeout.<@Nullable Object> assertTimeout(timeout, () -> {
			executable.execute();
			return null;
		}, messageSupplier);
	}

	static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier) {
		return assertTimeout(timeout, supplier, (Object) null);
	}

	static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier,
			@Nullable String message) {
		return assertTimeout(timeout, supplier, (Object) message);
	}

	static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier,
			Supplier<@Nullable String> messageSupplier) {
		return assertTimeout(timeout, supplier, (Object) messageSupplier);
	}

	private static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier,
			@Nullable Object messageOrSupplier) {
		Preconditions.notNull(timeout, () -> "timeout must not be null");
		Preconditions.condition(!timeout.isNegative() && !timeout.isZero(), () -> "timeout must be positive");
		Preconditions.condition(isRepresentableInNanos(timeout),
			() -> "timeout must be less than approximately 292 years (2^63 nanoseconds)");

		long start = System.nanoTime();
		T result;
		try {
			result = supplier.get();
		}
		catch (Throwable ex) {
			throw throwAsUncheckedException(ex);
		}
		// Creates a false positive if the tests runs for more than 292 years.
		var timeElapsed = Duration.ofNanos(System.nanoTime() - start);
		if (timeElapsed.compareTo(timeout) > 0) {
			assertionFailure() //
					.message(messageOrSupplier) //
					.reason(createExecutionExceededTimeoutMessage(timeout, timeElapsed)) //
					.trimStacktrace(Assertions.class) //
					.buildAndThrow();
		}
		return result;
	}

	private static String createExecutionExceededTimeoutMessage(Duration timeout, Duration timeElapsed) {
		var significantNanoFraction = timeout.toNanos() - MILLISECONDS.toNanos(timeout.toMillis()) != 0;
		var timeoutExceeded = timeElapsed.minus(timeout);
		boolean timeoutExceededOnlyByNanoSeconds = timeoutExceeded.toMillis() == 0;
		return "execution exceeded timeout of %s by %s" //
				.formatted(formatDuration(timeout, significantNanoFraction), //
					formatDuration(timeoutExceeded, significantNanoFraction || timeoutExceededOnlyByNanoSeconds));
	}

	private static String formatDuration(Duration duration, boolean includeNanoSeconds) {
		long milliseconds = duration.toMillis();
		if (!includeNanoSeconds) {
			return "%d ms".formatted(milliseconds);
		}
		long nanoFraction = duration.toNanosPart() - MILLISECONDS.toNanos(milliseconds);
		return "%d.%06d ms".formatted(milliseconds, nanoFraction);
	}

}
