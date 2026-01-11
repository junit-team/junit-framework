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

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.UNICODE_CASE;

import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 5.5
 */
class TimeoutDurationParser {

	private static final Pattern PATTERN = Pattern.compile("([1-9]\\d*) ?((?:[nμm]?s)|m|h|d)?",
		CASE_INSENSITIVE | UNICODE_CASE);

	private static final Map<String, TimeUnit> UNITS_BY_ABBREVIATION = Map.of( //
		"ns", NANOSECONDS, //
		"μs", MICROSECONDS, //
		"ms", MILLISECONDS, //
		"s", SECONDS, //
		"m", MINUTES, //
		"h", HOURS, //
		"d", DAYS //
	);

	TimeoutDuration parse(CharSequence text) throws DateTimeParseException {
		Matcher matcher = PATTERN.matcher(text);
		if (matcher.matches()) {
			long value = Long.parseLong(matcher.group(1));
			String unitAbbreviation = matcher.group(2);
			TimeUnit unit = unitAbbreviation == null ? SECONDS
					: requireNonNull(UNITS_BY_ABBREVIATION.get(unitAbbreviation.toLowerCase(Locale.ENGLISH)));
			return new TimeoutDuration(value, unit);
		}
		throw new DateTimeParseException("Timeout duration is not in the expected format (<number> [ns|μs|ms|s|m|h|d])",
			text, 0);
	}

}
