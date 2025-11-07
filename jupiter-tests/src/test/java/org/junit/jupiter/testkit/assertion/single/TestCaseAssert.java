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

/**
 * Assertions for asserting the state of single tests/containers.
 */
public interface TestCaseAssert {

	/**
	 * Asserts that there was exactly one started test.
	 *
	 * @return a {@link org.junit.jupiter.testkit.assertion.single.TestCaseStartedAssert} for further assertions
	 */
	TestCaseStartedAssert hasSingleStartedTest();

	/**
	 * Asserts that there was exactly one failed test.
	 *
	 * @return a {@link org.junit.jupiter.testkit.assertion.single.TestCaseFailureAssert} for further assertions
	 */
	TestCaseFailureAssert hasSingleFailedTest();

	/**
	 * Asserts that there was exactly one aborted test.
	 */
	void hasSingleAbortedTest();

	/**
	 * Asserts that there was exactly one successful test.
	 */
	void hasSingleSucceededTest();

	/**
	 * Asserts that there was exactly one skipped test.
	 */
	void hasSingleSkippedTest();

	/**
	 * Asserts that there was exactly one dynamically registered test.
	 *
	 * @return a {@link org.junit.jupiter.testkit.assertion.single.TestCaseStartedAssert} for further assertions
	 */
	TestCaseStartedAssert hasSingleDynamicallyRegisteredTest();

	/**
	 * Asserts that there was exactly one started container.
	 *
	 * @return a {@link org.junit.jupiter.testkit.assertion.single.TestCaseStartedAssert} for further assertions
	 */
	TestCaseStartedAssert hasSingleStartedContainer();

	/**
	 * Asserts that there was exactly one failed container.
	 *
	 * @return a {@link org.junit.jupiter.testkit.assertion.single.TestCaseFailureAssert} for further assertions
	 */
	TestCaseFailureAssert hasSingleFailedContainer();

	/**
	 * Asserts that there was exactly one aborted container.
	 */
	void hasSingleAbortedContainer();

	/**
	 * Asserts that there was exactly one succeeded container.
	 */
	void hasSingleSucceededContainer();

	/**
	 * Asserts that there was exactly one skipped container.
	 */
	void hasSingleSkippedContainer();

	/**
	 * Asserts that there was exactly one aborted container.
	 *
	 * @return a {@link org.junit.jupiter.testkit.assertion.single.TestCaseStartedAssert} for further assertions
	 */
	TestCaseStartedAssert hasSingleDynamicallyRegisteredContainer();

}
