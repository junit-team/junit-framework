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

import static org.junit.jupiter.api.AssertionTestUtils.assertExpectedAndActualValues;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertTrueAssertionsTests {

	@Test
	void assertTrueWithBooleanTrue() {
		assertTrue(true);
		assertTrue(true, "test");
		assertTrue(true, () -> "test");
	}

	@Test
	void assertTrueWithBooleanSupplierTrue() {
		assertTrue(() -> true);
		assertTrue(() -> true, "test");
		assertTrue(() -> true, () -> "test");
	}

	@Test
	void assertTrueWithBooleanTrueAndMessageSupplier() {
		assertTrue(true, () -> "test");
	}

	@Test
	void assertTrueWithBooleanSupplierTrueAndMessageSupplier() {
		assertTrue(() -> true, () -> "test");
	}

	@Test
	void assertTrueWithBooleanFalseAndDefaultMessageWithExpectedAndActualValues() {
		try {
			assertTrue(false);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <true> but was: <false>");
			assertExpectedAndActualValues(ex, true, false);
		}
	}

	@Test
	void assertTrueWithBooleanFalseAndString() {
		try {
			assertTrue(false, "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <true> but was: <false>");
			assertExpectedAndActualValues(ex, true, false);
		}
	}

	@Test
	void assertTrueWithBooleanFalseAndMessageSupplier() {
		try {
			assertTrue(false, () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <true> but was: <false>");
			assertExpectedAndActualValues(ex, true, false);
		}
	}

	@Test
	void assertTrueWithBooleanSupplierFalseAndString() {
		try {
			assertTrue(() -> false, "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <true> but was: <false>");
			assertExpectedAndActualValues(ex, true, false);
		}
	}

	@Test
	void assertTrueWithBooleanSupplierFalseAndMessageSupplier() {
		try {
			assertTrue(() -> false, () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <true> but was: <false>");
			assertExpectedAndActualValues(ex, true, false);
		}
	}

}
