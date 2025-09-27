/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import static java.util.Map.entry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Function;

import org.junit.platform.commons.util.Preconditions;

class StringToJavaTimeConverter extends StringToTargetTypeConverter<Object> {

	private static final Map<Class<?>, Function<String, ?>> CONVERTERS = Map.ofEntries( //
		entry(Duration.class, Duration::parse), //
		entry(Instant.class, Instant::parse), //
		entry(LocalDate.class, LocalDate::parse), //
		entry(LocalDateTime.class, LocalDateTime::parse), //
		entry(LocalTime.class, LocalTime::parse), //
		entry(MonthDay.class, MonthDay::parse), //
		entry(OffsetDateTime.class, OffsetDateTime::parse), //
		entry(OffsetTime.class, OffsetTime::parse), //
		entry(Period.class, Period::parse), //
		entry(Year.class, Year::parse), //
		entry(YearMonth.class, YearMonth::parse), //
		entry(ZonedDateTime.class, ZonedDateTime::parse), //
		entry(ZoneId.class, ZoneId::of), //
		entry(ZoneOffset.class, ZoneOffset::of) //
	);

	@Override
	boolean canConvert(Class<?> targetType) {
		return CONVERTERS.containsKey(targetType);
	}

	@Override
	Object convert(String source, Class<?> targetType) throws ConversionException {
		Function<String, ?> converter = Preconditions.notNull(CONVERTERS.get(targetType),
			() -> "No registered converter for %s".formatted(targetType.getName()));
		return converter.apply(source);
	}

}
