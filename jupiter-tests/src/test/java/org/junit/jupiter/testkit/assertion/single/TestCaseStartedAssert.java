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
 * This interface contains methods for asserting a single test or container
 * which was already asserted as "started".
 */
public interface TestCaseStartedAssert {

	/**
	 * Asserts that the test/container has succeeded.
	 */
	void whichSucceeded();

	/**
	 * Asserts that the test/container was aborted.
	 *
	 * @return a {@link TestCaseAbortedAssert} for further assertions.
	 */
	TestCaseAbortedAssert whichAborted();

	/**
	 * Asserts that the test/container has failed.
	 *
	 * @return a {@link TestCaseFailureAssert} for further assertions.
	 */
	TestCaseFailureAssert whichFailed();

}
