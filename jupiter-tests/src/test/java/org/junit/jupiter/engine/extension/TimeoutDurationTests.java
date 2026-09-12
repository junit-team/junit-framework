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
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
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
				"timeout duration must be a positive number: 0");
			assertPreconditionViolationFor(() -> new TimeoutDuration(-1, SECONDS)).withMessage(
				"timeout duration must be a positive number: -1");
		}

		@Test
		@SuppressWarnings("DataFlowIssue")
		void nonNullUnit() {
			assertPreconditionViolationNotNullFor("timeout unit", () -> new TimeoutDuration(1, null));
		}

		@Test
		void representableInNanos() {
			var maxNanoRepresentableDays = Duration.ofNanos(Long.MAX_VALUE).toDays();
			assertPreconditionViolationFor(() -> new TimeoutDuration(maxNanoRepresentableDays + 1, DAYS)).withMessage(
				"timeout duration must be less than approximately 292 years (2^63 nanoseconds): 106752 days");
		}
	}

}
