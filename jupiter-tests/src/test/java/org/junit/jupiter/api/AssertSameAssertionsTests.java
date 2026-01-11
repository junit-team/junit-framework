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
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageContains;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageMatches;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertSameAssertionsTests {

	@Test
	void assertSameWithTwoNulls() {
		assertSame(null, null);
		assertSame(null, null, () -> "should not fail");
	}

	@Test
	void assertSameWithSameObject() {
		Object foo = new Object();
		assertSame(foo, foo);
		assertSame(foo, foo, "message");
		assertSame(foo, foo, () -> "message");
	}

	@Test
	void assertSameWithObjectVsNull() {
		Object expected = new Object();
		try {
			assertSame(expected, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageContains(ex, "expected: <java.lang.Object@");
			assertMessageContains(ex, "but was: <null>");
			assertExpectedAndActualValues(ex, expected, null);
		}
	}

	@Test
	void assertSameWithNullVsObject() {
		Object actual = new Object();
		try {
			assertSame(null, actual);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageContains(ex, "expected: <null>");
			assertMessageContains(ex, "but was: <java.lang.Object@");
			assertExpectedAndActualValues(ex, null, actual);
		}
	}

	@Test
	void assertSameWithDifferentObjects() {
		Object expected = new Object();
		Object actual = new Object();
		try {
			assertSame(expected, actual);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageContains(ex, "expected: <java.lang.Object@");
			assertMessageContains(ex, "but was: <java.lang.Object@");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

	@Test
	void assertSameWithEqualPrimitivesAutoboxedToDifferentWrappers() {
		try {
			int i = 999;
			assertSame(i, i);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageMatches(ex,
				"expected: java\\.lang\\.Integer@.+?<999> but was: java\\.lang\\.Integer@.+?<999>");
			assertExpectedAndActualValues(ex, 999, 999);
		}
	}

	@Test
	void assertSameWithEquivalentStringsAndMessageSupplier() {
		String expected = new String("foo");
		String actual = new String("foo");
		try {
			assertSame(expected, actual, () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "test");
			assertMessageContains(ex, "expected: java.lang.String@");
			assertMessageContains(ex, "but was: java.lang.String@");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

}
