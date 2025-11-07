/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit.assertion.suite;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.assertj.core.api.ListAssert;

interface TestSuiteFailureAssert {

	/**
	 * Asserts that all thrown exceptions are of a certain type.
	 *
	 * @param exceptionType the exception type you want to check
	 * @return a {@link ListAssert} for asserting exception messages
	 */
	/*
	 * Note: We avoid a varargs-variant of this method to prevent heap-pollution warnings.
	 * If you need a method with n exception types, create n-1 `withExceptionInstancesOf`
	 * (note the plural) overloads with [2, n] parameters.
	 */
	ListAssert<String> withExceptionInstancesOf(Class<? extends Throwable> exceptionType);

	/**
	 * Asserts that all failed tests failed because of a Throwable.
	 *
	 * @return a {@link ListAssert} for asserting exception messages
	 */
	ListAssert<String> withExceptions();

	/**
	 * Asserts that the thrown exceptions fulfill the condition of the given predicate.
	 *
	 * @param predicate the condition the exceptions must fulfill
	 */
	void assertingExceptions(Predicate<List<Throwable>> predicate);

	/**
	 * Applies the supplied consumer to the exceptions thrown by the tests/containers.
	 *
	 * @param testFunction a consumer, for writing more flexible tests
	 */
	void andThenCheckExceptions(Consumer<List<Throwable>> testFunction);

}
