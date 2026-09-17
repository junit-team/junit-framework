/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Timeout;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.5
 */
record TimeoutDuration(long value, TimeUnit unit) {

	static TimeoutDuration from(Timeout timeout) {
		return new TimeoutDuration(timeout.value(), timeout.unit());
	}

	TimeoutDuration {
		Preconditions.notNull(unit, "timeout unit must not be null");
		long maxRepresentableValue = maxRepresentableValueFor(unit);
		Preconditions.condition(value > 0 && value <= maxRepresentableValue, //
			() -> "timeout duration must be a positive number less than approximately %s (2^63 nanoseconds): %s" //
					.formatted(formatTimeoutDuration(maxRepresentableValue, unit), formatTimeoutDuration(value, unit)));
	}

	private static long maxRepresentableValueFor(TimeUnit unit) {
		return unit.convert(Long.MAX_VALUE, NANOSECONDS);
	}

	@Override
	public String toString() {
		return formatTimeoutDuration(value, unit);
	}

	private static String formatTimeoutDuration(long value, TimeUnit unit) {
		String label = unit.name().toLowerCase(Locale.ROOT);
		if (value == 1 && label.endsWith("s")) {
			label = label.substring(0, label.length() - 1);
		}
		return value + " " + label;
	}

	public Duration toDuration() {
		return Duration.of(value, toChronoUnit());
	}

	private ChronoUnit toChronoUnit() {
		return switch (unit) {
			case NANOSECONDS -> ChronoUnit.NANOS;
			case MICROSECONDS -> ChronoUnit.MICROS;
			case MILLISECONDS -> ChronoUnit.MILLIS;
			case SECONDS -> ChronoUnit.SECONDS;
			case MINUTES -> ChronoUnit.MINUTES;
			case HOURS -> ChronoUnit.HOURS;
			case DAYS -> ChronoUnit.DAYS;
			default -> throw new JUnitException("Could not map TimeUnit " + unit + " to ChronoUnit");
		};
	}
}
