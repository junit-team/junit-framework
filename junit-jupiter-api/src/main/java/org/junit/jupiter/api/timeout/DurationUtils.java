/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.timeout;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.time.Duration;

import org.apiguardian.api.API;

/**
 * Internal utilities for working with durations.
 *
 * @since 6.0
 */
@API(status = INTERNAL, since = "6.2")
public final class DurationUtils {

	private static final Duration MAX_NANO_DURATION = Duration.ofNanos(Long.MAX_VALUE);

	private DurationUtils() {
		/* no-op */
	}

	public static boolean isPositiveAndRepresentableInNanos(Duration duration) {
		return isPositive(duration) && duration.compareTo(MAX_NANO_DURATION) <= 0;
	}

	private static boolean isPositive(Duration timeout) {
		return !timeout.isNegative() && !timeout.isZero();
	}

	public static String formatDurationInFractionalMs(Duration duration) {
		return formatDurationInMs(duration, hasSignificantNanoFraction(duration));
	}

	public static String formatDurationInMs(Duration duration, boolean includeNanoSecondFraction) {
		long milliseconds = duration.toMillis();
		if (!includeNanoSecondFraction) {
			return "%d ms".formatted(milliseconds);
		}
		long nanoFraction = duration.toNanos() - MILLISECONDS.toNanos(milliseconds);
		return "%d.%06d ms".formatted(milliseconds, nanoFraction);
	}

	public static boolean hasSignificantNanoFraction(Duration timeout) {
		return nanoFraction(timeout) != 0;
	}

	private static long nanoFraction(Duration timeout) {
		return timeout.toNanos() - MILLISECONDS.toNanos(timeout.toMillis());
	}
}
