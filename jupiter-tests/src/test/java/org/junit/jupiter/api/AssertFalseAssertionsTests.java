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
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertFalseAssertionsTests {

	@Test
	void assertFalseWithBooleanFalse() {
		assertFalse(false);
		assertFalse(false, "test");
		assertFalse(false, () -> "test");
	}

	@Test
	void assertFalseWithBooleanSupplierFalse() {
		assertFalse(() -> false);
		assertFalse(() -> false, "test");
		assertFalse(() -> false, () -> "test");
	}

	@Test
	void assertFalseWithBooleanFalseAndMessageSupplier() {
		assertFalse(false, () -> "test");
	}

	@Test
	void assertFalseWithBooleanSupplierFalseAndMessageSupplier() {
		assertFalse(() -> false, () -> "test");
	}

	@Test
	void assertFalseWithBooleanTrueAndDefaultMessageWithExpectedAndActualValues() {
		try {
			assertFalse(true);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <false> but was: <true>");
			assertExpectedAndActualValues(ex, false, true);
		}
	}

	@Test
	void assertFalseWithBooleanTrueAndString() {
		try {
			assertFalse(true, "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <false> but was: <true>");
			assertExpectedAndActualValues(ex, false, true);
		}
	}

	@Test
	void assertFalseWithBooleanSupplierTrueAndString() {
		try {
			assertFalse(() -> true, "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <false> but was: <true>");
			assertExpectedAndActualValues(ex, false, true);
		}
	}

	@Test
	void assertFalseWithBooleanTrueAndMessageSupplier() {
		try {
			assertFalse(true, () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <false> but was: <true>");
			assertExpectedAndActualValues(ex, false, true);
		}
	}

	@Test
	void assertFalseWithBooleanSupplierTrueAndMessageSupplier() {
		try {
			assertFalse(() -> true, () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <false> but was: <true>");
			assertExpectedAndActualValues(ex, false, true);
		}
	}

}
