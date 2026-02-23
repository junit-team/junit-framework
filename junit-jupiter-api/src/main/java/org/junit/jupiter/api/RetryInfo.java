/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;

/**
 * {@code RetryInfo} is used to inject information about the current retry
 * attempt of a retry test into {@code @RetryTest}, {@code @BeforeEach},
 * and {@code @AfterEach} methods.
 *
 * <p>If a method parameter is of type {@code RetryInfo}, JUnit will supply
 * an instance of {@code RetryInfo} corresponding to the current retry test
 * as the value for the parameter.
 *
 * <p><strong>WARNING</strong>: {@code RetryInfo} cannot be injected into
 * a {@code @BeforeEach} or {@code @AfterEach} method if the corresponding test
 * method is not a {@code @RetryTest}. Any attempt to do so will result in a
 * {@link org.junit.jupiter.api.extension.ParameterResolutionException
 * ParameterResolutionException}.
 *
 * @since 6.1
 * @see RetryTest
 * @see TestInfo
 */
@API(status = EXPERIMENTAL, since = "6.1")
public interface RetryInfo {

	/**
	 * Get the current attempt number of the corresponding
	 * {@link RetryTest @RetryTest} method.
	 *
	 * <p>The first attempt returns {@code 1}, the first retry returns
	 * {@code 2}, and so on.
	 *
	 * @return the current attempt number (1-based)
	 */
	int getCurrentAttempt();

	/**
	 * Get the maximum number of attempts configured for the corresponding
	 * {@link RetryTest @RetryTest} method.
	 *
	 * @return the maximum number of attempts
	 * @see RetryTest#maxAttempts()
	 */
	int getMaxAttempts();

	/**
	 * Get the number of failed attempts so far for the corresponding
	 * {@link RetryTest @RetryTest} method.
	 *
	 * @return the number of failed attempts
	 */
	int getFailureCount();

	/**
	 * Get the configured delay in milliseconds between retry attempts.
	 *
	 * @return the delay in milliseconds
	 * @see RetryTest#delayMs()
	 */
	long getDelayMs();

}
