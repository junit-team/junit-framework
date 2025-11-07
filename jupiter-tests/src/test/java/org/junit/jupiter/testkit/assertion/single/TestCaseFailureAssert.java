/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit.assertion.single;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.assertj.core.api.AbstractThrowableAssert;

/**
 * Used to assert a single failed container or test.
 */
public interface TestCaseFailureAssert {

	/**
	 * Asserts that the test/container failed because of a specific type of exception.
	 *
	 * @param exceptionType the expected type of the thrown exception
	 * @return an {@link AbstractThrowableAssert} for further assertions
	 */
	<T extends Throwable> AbstractThrowableAssert<?, T> withExceptionInstanceOf(Class<T> exceptionType);

	/**
	 * Asserts that the test/container failed because an exception was thrown.
	 *
	 * @return an {@link AbstractThrowableAssert} for further assertions
	 */
	AbstractThrowableAssert<?, ? extends Throwable> withException();

	/**
	 * Asserts that the test/container threw an exception that fulfills the supplied predicate.
	 *
	 * @param predicate the condition the thrown exception must fulfill
	 */
	void withExceptionFulfilling(Predicate<Throwable> predicate);

	/**
	 * Applies the supplied consumer to the exception thrown by the test/container.
	 *
	 * @param testFunction a consumer, for writing more flexible tests
	 */
	void andThenCheckException(Consumer<Throwable> testFunction);

}
