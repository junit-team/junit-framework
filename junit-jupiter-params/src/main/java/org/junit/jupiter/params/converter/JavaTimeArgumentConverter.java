/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalQuery;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * @since 5.0
 */
class JavaTimeArgumentConverter extends AnnotationBasedArgumentConverter<JavaTimeConversionPattern> {

	private static final Map<Class<?>, TemporalQuery<?>> TEMPORAL_QUERIES;
	static {
		Map<Class<?>, TemporalQuery<?>> queries = new LinkedHashMap<>();
		queries.put(ChronoLocalDate.class, ChronoLocalDate::from);
		queries.put(ChronoLocalDateTime.class, ChronoLocalDateTime::from);
		queries.put(ChronoZonedDateTime.class, ChronoZonedDateTime::from);
		queries.put(LocalDate.class, LocalDate::from);
		queries.put(LocalDateTime.class, LocalDateTime::from);
		queries.put(LocalTime.class, LocalTime::from);
		queries.put(OffsetDateTime.class, OffsetDateTime::from);
		queries.put(OffsetTime.class, OffsetTime::from);
		queries.put(Year.class, Year::from);
		queries.put(YearMonth.class, YearMonth::from);
		queries.put(ZonedDateTime.class, ZonedDateTime::from);
		TEMPORAL_QUERIES = Collections.unmodifiableMap(queries);
	}

	@Override
	protected @Nullable Object convert(@Nullable Object input, Class<?> targetClass,
			JavaTimeConversionPattern annotation) {

		if (input == null) {
			if (annotation.nullable()) {
				return null;
			}
			throw new ArgumentConversionException(
				"Cannot convert null to " + targetClass.getName() + "; consider setting 'nullable = true'");
		}
		TemporalQuery<?> temporalQuery = TEMPORAL_QUERIES.get(targetClass);
		if (temporalQuery == null) {
			throw new ArgumentConversionException("Cannot convert to " + targetClass.getName() + ": " + input);
		}
		String pattern = annotation.value();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		return formatter.parse(input.toString(), temporalQuery);
	}

}
