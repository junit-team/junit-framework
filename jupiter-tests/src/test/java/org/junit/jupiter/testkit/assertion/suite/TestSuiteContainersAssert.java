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

public interface TestSuiteContainersAssert {

	/**
	 * Asserts that there were exactly {@code expected} number of started containers.
	 *
	 * @param expected the expected number of started containers
	 * @return a {@code TestSuiteContainersAssert} for further assertions.
	 */
	TestSuiteContainersAssert hasNumberOfStartedContainers(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of failed containers.
	 *
	 * @param expected the expected number of failed containers
	 * @return a {@code TestSuiteContainersFailureAssert} for further assertions.
	 */
	TestSuiteContainersFailureAssert hasNumberOfFailedContainers(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of aborted containers.
	 *
	 * @param expected the expected number of aborted containers
	 * @return a {@code TestSuiteContainersAssert} for further assertions.
	 */
	TestSuiteContainersAssert hasNumberOfAbortedContainers(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of succeeded containers.
	 *
	 * @param expected the expected number of succeeded containers
	 * @return a {@code TestSuiteContainersAssert} for further assertions.
	 */
	TestSuiteContainersAssert hasNumberOfSucceededContainers(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of skipped containers.
	 *
	 * @param expected the expected number of skipped containers
	 * @return a {@code TestSuiteContainersAssert} for further assertions.
	 */
	TestSuiteContainersAssert hasNumberOfSkippedContainers(int expected);

	/**
	 * Asserts that there were exactly {@code expected} number of dynamically registered containers.
	 *
	 * @param expected the expected number of dynamically registered containers
	 * @return a {@code TestSuiteContainersAssert} for further assertions.
	 */
	TestSuiteContainersAssert hasNumberOfDynamicallyRegisteredContainers(int expected);

	interface TestSuiteContainersFailureAssert extends TestSuiteContainersAssert, TestSuiteFailureAssert {
	}

}
