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

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @since 5.0
 */
class JavaTimeArgumentConverterTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToChronoLocalDate() {
		assertThat(convert("01.02.2017", "dd.MM.yyyy", ChronoLocalDate.class)) //
				.isEqualTo(LocalDate.of(2017, 2, 1));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToChronoLocalDateTime() {
		assertThat(convert("01.02.2017 12:34:56.789", "dd.MM.yyyy HH:mm:ss.SSS", ChronoLocalDateTime.class)) //
				.isEqualTo(LocalDateTime.of(2017, 2, 1, 12, 34, 56, 789_000_000));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToChronoZonedDateTime() {
		assertThat(convert("01.02.2017 12:34:56.789 Z", "dd.MM.yyyy HH:mm:ss.SSS X", ChronoZonedDateTime.class)) //
				.isEqualTo(ZonedDateTime.of(2017, 2, 1, 12, 34, 56, 789_000_000, UTC));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToLocalDate() {
		assertThat(convert("01.02.2017", "dd.MM.yyyy", LocalDate.class)) //
				.isEqualTo(LocalDate.of(2017, 2, 1));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToLocalDateTime() {
		assertThat(convert("01.02.2017 12:34:56.789", "dd.MM.yyyy HH:mm:ss.SSS", LocalDateTime.class)) //
				.isEqualTo(LocalDateTime.of(2017, 2, 1, 12, 34, 56, 789_000_000));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToLocalTime() {
		assertThat(convert("12:34:56.789", "HH:mm:ss.SSS", LocalTime.class)) //
				.isEqualTo(LocalTime.of(12, 34, 56, 789_000_000));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToOffsetDateTime() {
		assertThat(convert("01.02.2017 12:34:56.789 +02", "dd.MM.yyyy HH:mm:ss.SSS X", OffsetDateTime.class)) //
				.isEqualTo(OffsetDateTime.of(2017, 2, 1, 12, 34, 56, 789_000_000, ZoneOffset.ofHours(2)));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToOffsetTime() {
		assertThat(convert("12:34:56.789 -02", "HH:mm:ss.SSS X", OffsetTime.class)) //
				.isEqualTo(OffsetTime.of(12, 34, 56, 789_000_000, ZoneOffset.ofHours(-2)));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToYear() {
		assertThat(convert("2017", "yyyy", Year.class)) //
				.isEqualTo(Year.of(2017));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToYearMonth() {
		assertThat(convert("03/2017", "MM/yyyy", YearMonth.class)) //
				.isEqualTo(YearMonth.of(2017, 3));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsStringToZonedDateTime() {
		assertThat(convert("01.02.2017 12:34:56.789 Europe/Berlin", "dd.MM.yyyy HH:mm:ss.SSS VV", ZonedDateTime.class)) //
				.isEqualTo(ZonedDateTime.of(2017, 2, 1, 12, 34, 56, 789_000_000, ZoneId.of("Europe/Berlin")));
	}

	@Test
	void throwsExceptionOnInvalidTargetType() {
		var exception = assertThrows(ArgumentConversionException.class, () -> convert("2017", "yyyy", Integer.class));

		assertThat(exception).hasMessage("Cannot convert to java.lang.Integer: 2017");
	}

	/**
	 * @since 5.12
	 */
	@Test
	void throwsExceptionOnNullParameterWithoutNullable() {
		var exception = assertThrows(ArgumentConversionException.class,
			() -> convert(null, "dd.MM.yyyy", LocalDate.class));

		assertThat(exception).hasMessage(
			"Cannot convert null to java.time.LocalDate; consider setting 'nullable = true'");
	}

	/**
	 * @since 5.12
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void convertsNullableParameter() {
		assertThat(convert(null, "dd.MM.yyyy", true, LocalDate.class)).isNull();
	}

	private @Nullable Object convert(@Nullable Object input, String pattern, Class<?> targetClass) {
		return convert(input, pattern, false, targetClass);
	}

	private @Nullable Object convert(@Nullable Object input, String pattern, boolean nullable, Class<?> targetClass) {
		var converter = new JavaTimeArgumentConverter();
		var annotation = mock(JavaTimeConversionPattern.class);
		when(annotation.value()).thenReturn(pattern);
		when(annotation.nullable()).thenReturn(nullable);

		return converter.convert(input, targetClass, annotation);
	}

}
