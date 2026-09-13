/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;

final class DurationUtils {

	private DurationUtils() {
		/* no-op */
	}

	static String formatDurationInFractionalMs(Duration duration) {
		return formatDurationInMs(duration, hasSignificantNanoFraction(duration));
	}

	static String formatDurationInMs(Duration duration, boolean includeNanoSecondFraction) {
		long milliseconds = duration.toMillis();
		if (!includeNanoSecondFraction) {
			return "%d ms".formatted(milliseconds);
		}
		long nanoFraction = duration.toNanos() - MILLISECONDS.toNanos(milliseconds);
		return "%d.%06d ms".formatted(milliseconds, nanoFraction);
	}

	static boolean hasSignificantNanoFraction(Duration timeout) {
		return nanoFraction(timeout) != 0;
	}

	private static long nanoFraction(Duration timeout) {
		return timeout.toNanos() - MILLISECONDS.toNanos(timeout.toMillis());
	}

}
