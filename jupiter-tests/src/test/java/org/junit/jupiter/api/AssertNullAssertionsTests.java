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
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageMatches;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Supplier;

import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertNullAssertionsTests {

	@Test
	void assertNullWithNull() {
		assertNull(null);
		assertNull(null, "message");
		assertNull(null, () -> "message");
	}

	@Test
	void assertNullWithNullAndMessageSupplier() {
		assertNull(null, () -> "test");
	}

	@Test
	@SuppressWarnings("unused")
	void assertNullWithNonNullObject() {
		try {
			assertNull("foo");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <null> but was: <foo>");
			assertExpectedAndActualValues(ex, null, "foo");
		}
	}

	@Test
	void assertNullWithNonNullObjectWithNullStringReturnedFromToString() {
		assertNullWithNonNullObjectWithNullStringReturnedFromToString(null);
	}

	@Test
	void assertNullWithNonNullObjectWithNullStringReturnedFromToStringAndMessageSupplier() {
		assertNullWithNonNullObjectWithNullStringReturnedFromToString(() -> "boom");
	}

	@SuppressWarnings("unused")
	private void assertNullWithNonNullObjectWithNullStringReturnedFromToString(
			@Nullable Supplier<@Nullable String> messageSupplier) {
		String actual = "null";
		try {
			if (messageSupplier == null) {
				assertNull(actual);
			}
			else {
				assertNull(actual, messageSupplier);
			}
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			// Should look something like:
			// expected: <null> but was: java.lang.String@264b3504<null>
			String prefix = (messageSupplier != null ? messageSupplier.get() + " ==> " : "");
			assertMessageMatches(ex, prefix + "expected: <null> but was: java\\.lang\\.String@.+<null>");
			assertExpectedAndActualValues(ex, null, actual);
		}
	}

	@Test
	void assertNullWithNonNullObjectWithNullReferenceReturnedFromToString() {
		assertNullWithNonNullObjectWithNullReferenceReturnedFromToString(null);
	}

	@Test
	void assertNullWithNonNullObjectWithNullReferenceReturnedFromToStringAndMessageSupplier() {
		assertNullWithNonNullObjectWithNullReferenceReturnedFromToString(() -> "boom");
	}

	@SuppressWarnings("unused")
	private void assertNullWithNonNullObjectWithNullReferenceReturnedFromToString(
			@Nullable Supplier<@Nullable String> messageSupplier) {
		Object actual = new NullToString();
		try {
			if (messageSupplier == null) {
				assertNull(actual);
			}
			else {
				assertNull(actual, messageSupplier);
			}
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			// Should look something like:
			// expected: <null> but was: org.junit.jupiter.api.AssertNullAssertionsTests$NullToString@4e7912d8<null>
			String prefix = (messageSupplier != null ? messageSupplier.get() + " ==> " : "");
			assertMessageMatches(ex, prefix
					+ "expected: <null> but was: org\\.junit\\.jupiter\\.api\\.AssertNullAssertionsTests\\$NullToString@.+<null>");
			assertExpectedAndActualValues(ex, null, actual);
		}
	}

	@Test
	@SuppressWarnings("unused")
	void assertNullWithNonNullObjectAndMessage() {
		try {
			assertNull("foo", "a message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "a message ==> expected: <null> but was: <foo>");
			assertExpectedAndActualValues(ex, null, "foo");
		}
	}

	@Test
	@SuppressWarnings("unused")
	void assertNullWithNonNullObjectAndMessageSupplier() {
		try {
			assertNull("foo", () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "test ==> expected: <null> but was: <foo>");
			assertExpectedAndActualValues(ex, null, "foo");
		}
	}

	@NullUnmarked
	private static class NullToString {

		@Override
		public String toString() {
			return null;
		}
	}

}
