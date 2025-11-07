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

public interface TestSuiteTestsAssert {

	/**
	 * Asserts that there were exactly {@code expected} number of started tests.
	 *
	 * @param expected the expected number of started tests
	 * @return a {@code TestSuiteTestsAssert} for further assertions.
	 */
	TestSuiteTestsAssert hasNumberOfStartedTests(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of failed tests.
	 *
	 * @param expected the expected number of failed tests
	 * @return a {@code TestSuiteTestsFailureAssert} for further assertions.
	 */
	TestSuiteTestsFailureAssert hasNumberOfFailedTests(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of aborted tests.
	 *
	 * @param expected the expected number of aborted tests
	 * @return a {@code TestSuiteTestsAssert} for further assertions.
	 */
	TestSuiteTestsAssert hasNumberOfAbortedTests(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of succeeded tests.
	 *
	 * @param expected the expected number of succeeded tests
	 * @return a {@code TestSuiteTestsAssert} for further assertions.
	 */
	TestSuiteTestsAssert hasNumberOfSucceededTests(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of skipped tests.
	 *
	 * @param expected the expected number of skipped tests
	 * @return a {@code TestSuiteTestsAssert} for further assertions.
	 */
	TestSuiteTestsAssert hasNumberOfSkippedTests(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of dynamically registered tests.
	 *
	 * @param expected the expected number of dynamically registered tests
	 * @return a {@code TestSuiteTestsAssert} for further assertions.
	 */
	TestSuiteTestsAssert hasNumberOfDynamicallyRegisteredTests(int expected);

	interface TestSuiteTestsFailureAssert extends TestSuiteTestsAssert, TestSuiteFailureAssert {
	}

}
