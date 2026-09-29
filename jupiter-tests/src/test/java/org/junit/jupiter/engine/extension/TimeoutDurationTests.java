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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @since 5.5
 */
class TimeoutDurationTests {

	@Test
	void formatsDurationNicely() {
		assertThat(new TimeoutDuration(1, SECONDS)).hasToString("1 second");
		assertThat(new TimeoutDuration(2, SECONDS)).hasToString("2 seconds");
	}

	@Test
	void fulfillsEqualsAndHashCodeContract() {
		var oneSecond = new TimeoutDuration(1, SECONDS);

		assertThat(oneSecond) //
				.isEqualTo(oneSecond) //
				.isEqualTo(new TimeoutDuration(1, SECONDS)) //
				.hasSameHashCodeAs(new TimeoutDuration(1, SECONDS)) //
				.isNotEqualTo("foo") //
				.isNotEqualTo(new TimeoutDuration(2, SECONDS)) //
				.isNotEqualTo(new TimeoutDuration(1, MINUTES));
	}

	@Nested
	class Preconditions {

		@Test
		void positiveDuration() {
			assertPreconditionViolationFor(() -> new TimeoutDuration(0, SECONDS)).withMessage(
				"timeout duration must be a positive number less than approximately 9223372036 seconds (2^63 nanoseconds): 0 seconds");
			assertPreconditionViolationFor(() -> new TimeoutDuration(-1, SECONDS)).withMessage(
				"timeout duration must be a positive number less than approximately 9223372036 seconds (2^63 nanoseconds): -1 seconds");
		}

		@Test
		@SuppressWarnings("DataFlowIssue")
		void nonNullUnit() {
			assertPreconditionViolationNotNullFor("timeout unit", () -> new TimeoutDuration(1, null));
		}

		@Test
		void representableInNanos() {
			var maxRepresentableDuration = Duration.ofNanos(Long.MAX_VALUE);

			var maxNanoRepresentableDays = maxRepresentableDuration.toDays();
			assertPreconditionViolationFor(() -> new TimeoutDuration(maxNanoRepresentableDays + 1, DAYS)) //
					.withMessage(
						"timeout duration must be a positive number less than approximately 106751 days (2^63 nanoseconds): 106752 days");

			var maxNanoRepresentableHours = maxRepresentableDuration.toHours();
			assertPreconditionViolationFor(() -> new TimeoutDuration(maxNanoRepresentableHours + 1, HOURS)) //
					.withMessage(
						"timeout duration must be a positive number less than approximately 2562047 hours (2^63 nanoseconds): 2562048 hours");

			var maxNanoRepresentableMinutes = maxRepresentableDuration.toMinutes();
			assertPreconditionViolationFor(() -> new TimeoutDuration(maxNanoRepresentableMinutes + 1, MINUTES)) //
					.withMessage(
						"timeout duration must be a positive number less than approximately 153722867 minutes (2^63 nanoseconds): 153722868 minutes");

			var maxNanoRepresentableSeconds = maxRepresentableDuration.toSeconds();
			assertPreconditionViolationFor(() -> new TimeoutDuration(maxNanoRepresentableSeconds + 1, SECONDS)) //
					.withMessage(
						"timeout duration must be a positive number less than approximately 9223372036 seconds (2^63 nanoseconds): 9223372037 seconds");

			var maxNanoRepresentableMillis = maxRepresentableDuration.toMillis();
			assertPreconditionViolationFor(() -> new TimeoutDuration(maxNanoRepresentableMillis + 1, MILLISECONDS)) //
					.withMessage(
						"timeout duration must be a positive number less than approximately 9223372036854 milliseconds (2^63 nanoseconds): 9223372036855 milliseconds");

			var maxNanoRepresentableNanos = maxRepresentableDuration.toNanos();
			assertDoesNotThrow(() -> new TimeoutDuration(maxNanoRepresentableNanos, NANOSECONDS));

		}
	}

}
