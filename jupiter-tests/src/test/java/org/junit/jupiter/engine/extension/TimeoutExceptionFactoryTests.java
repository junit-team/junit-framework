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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.engine.extension.TimeoutExceptionFactory.create;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @since 5.9
 */
@DisplayName("TimeoutExceptionFactory")
class TimeoutExceptionFactoryTests {

	private static final TimeoutDuration tenMillisDuration = new TimeoutDuration(10, MILLISECONDS);
	private static final Exception suppressedException = new Exception("Winke!");
	private static final String methodSignature = "test()";

	@Test
	@DisplayName("creates exception with method signature and timeout")
	void createExceptionWithMethodSignatureTimeout() {
		TimeoutException exception = create(methodSignature, tenMillisDuration);

		assertThat(exception) //
				.hasMessage("test() timed out after 10 milliseconds") //
				.hasNoSuppressedExceptions();
	}

	@Test
	@DisplayName("creates exception with method signature, timeout and throwable")
	void createExceptionWithMethodSignatureTimeoutAndThrowable() {
		TimeoutException exception = create(methodSignature, tenMillisDuration, suppressedException);

		assertThat(exception) //
				.hasMessage("test() timed out after 10 milliseconds") //
				.hasSuppressedException(suppressedException);
	}

	@SuppressWarnings({ "DataFlowIssue", "ThrowableNotThrown" })
	@Nested
	@DisplayName("throws exception when")
	class ThrowException {

		@Test
		@DisplayName("method signature is null")
		void methodSignatureIsNull() {
			assertThatThrownBy(() -> create(null, tenMillisDuration, suppressedException)) //
					.hasMessage("method signature must not be null");
		}

		@Test
		@DisplayName("method timeout duration is null")
		void timeoutDurationIsnull() {
			assertThatThrownBy(() -> create(methodSignature, null, suppressedException)) //
					.hasMessage("timeout duration must not be null");
		}
	}

}
