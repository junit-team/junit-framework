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

import static org.junit.jupiter.api.AssertionTestUtils.assertMessageContains;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertNotSameAssertionsTests {

	@Test
	void assertNotSameWithDifferentObjects() {
		assertNotSame(new Object(), new Object());
		assertNotSame(new Object(), new Object(), "message");
		assertNotSame(new Object(), new Object(), () -> "message");
	}

	@Test
	void assertNotSameWithDifferentObjectsAndMessageSupplier() {
		assertNotSame(new Object(), new Object(), () -> "should not fail");
	}

	@Test
	void assertNotSameWithObjectVsNull() {
		assertNotSame(new Object(), null);
	}

	@Test
	void assertNotSameWithNullVsObject() {
		assertNotSame(null, new Object());
	}

	@Test
	void assertNotSameWithTwoNulls() {
		try {
			assertNotSame(null, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: not same but was: <null>");
		}
	}

	@Test
	void assertNotSameWithSameObjectAndMessage() {
		try {
			Object foo = new Object();
			assertNotSame(foo, foo, "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "test");
			assertMessageContains(ex, "expected: not same but was: <java.lang.Object@");
		}
	}

	@Test
	void assertNotSameWithSameObjectAndMessageSupplier() {
		try {
			Object foo = new Object();
			assertNotSame(foo, foo, () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "test");
			assertMessageContains(ex, "expected: not same but was: <java.lang.Object@");
		}
	}

}
