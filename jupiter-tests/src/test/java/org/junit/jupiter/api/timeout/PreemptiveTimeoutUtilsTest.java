/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.timeout;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.junit.jupiter.api.timeout.PreemptiveTimeoutUtils.executeWithPreemptiveTimeout;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.AssertionFailedError;

class PreemptiveTimeoutUtilsTest {

	private static final Duration PREEMPTIVE_TIMEOUT = ofMillis(WINDOWS.isCurrentOs() ? 1000 : 100);
	private static final PreemptiveTimeoutUtils.TimeoutFailureFactory<TimeoutException> TIMEOUT_EXCEPTION_FACTORY = (__,
			___, ____, _____) -> new TimeoutException();

	@Test
	void executeWithPreemptiveTimeoutThrowingTimeoutExceptionWithMessageForSupplierThatCompletesAfterTheTimeout() {
		assertThrows(TimeoutException.class, () -> executeWithPreemptiveTimeout(PREEMPTIVE_TIMEOUT, () -> {
			waitForInterrupt();
			return "Tempus Fugit";
		}, () -> "Tempus Fugit", TIMEOUT_EXCEPTION_FACTORY));
	}

	@Test
	void executeWithPreemptiveTimeoutThrowingTimeoutExceptionWithMessageForSupplierThatThrowsAnAssertionFailedError() {
		AssertionFailedError exception = assertThrows(AssertionFailedError.class,
			() -> executeWithPreemptiveTimeout(ofMillis(500), () -> fail("enigma"), () -> "Tempus Fugit",
				TIMEOUT_EXCEPTION_FACTORY));
		assertThat(exception).hasMessage("enigma");
	}

	@Test
	void executeWithPreemptiveTimeoutThrowingTimeoutExceptionWithMessageForSupplierThatThrowsAnException() {
		RuntimeException exception = assertThrows(RuntimeException.class,
			() -> executeWithPreemptiveTimeout(ofMillis(500),
				() -> ExceptionUtils.throwAsUncheckedException(new RuntimeException(":(")), () -> "Tempus Fugit",
				TIMEOUT_EXCEPTION_FACTORY));
		assertThat(exception).hasMessage(":(");
	}

	@Test
	void executeWithPreemptiveTimeoutThrowingTimeoutExceptionWithMessageForSupplierThatCompletesBeforeTimeout()
			throws Exception {
		var result = executeWithPreemptiveTimeout(PREEMPTIVE_TIMEOUT, () -> "Tempus Fugit", () -> "Tempus Fugit",
			TIMEOUT_EXCEPTION_FACTORY);

		assertThat(result).isEqualTo("Tempus Fugit");
	}

	private void waitForInterrupt() {
		try {
			assertFalse(Thread.interrupted(), "Already interrupted");
			new CountDownLatch(1).await();
		}
		catch (InterruptedException ignore) {
			// ignore
		}
	}

	@Nested
	class Preconditions {

		@Test
		void positiveDuration() {
			assertPreconditionViolationFor( //
				() -> PreemptiveTimeoutUtils.executeWithPreemptiveTimeout(Duration.ofNanos(-1), //
					() -> fail("enigma"), //
					() -> "Tempus Fugit", TIMEOUT_EXCEPTION_FACTORY)) //
							.withMessage("timeout must be positive");

			assertPreconditionViolationFor( //
				() -> PreemptiveTimeoutUtils.executeWithPreemptiveTimeout(Duration.ofNanos(0), //
					() -> fail("enigma"), //
					() -> "Tempus Fugit", TIMEOUT_EXCEPTION_FACTORY)) //
							.withMessage("timeout must be positive");

		}

		@Test
		void timeoutRepresentableInNanos() {
			assertPreconditionViolationFor( //
				() -> PreemptiveTimeoutUtils.executeWithPreemptiveTimeout(Duration.ofNanos(Long.MAX_VALUE).plusNanos(1),
					() -> fail("enigma"), //
					() -> "Tempus Fugit", TIMEOUT_EXCEPTION_FACTORY)) //
							.withMessage("timeout must be less than approximately 292 years (2^63 nanoseconds)");
		}

	}

}
