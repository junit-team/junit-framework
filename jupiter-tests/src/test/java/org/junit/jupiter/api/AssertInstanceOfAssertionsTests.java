/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions#assertInstanceOf(Class, Object)}.
 *
 * @since 5.8
 */
class AssertInstanceOfAssertionsTests {

	@Test
	void assertInstanceOfFailsNullValue() {
		assertInstanceOfFails(String.class, null, "null value");
	}

	@Test
	void assertInstanceOfFailsWrongTypeValue() {
		assertInstanceOfFails(String.class, 1, "type");
	}

	@Test
	void assertInstanceOfFailsWrongExceptionValue() {
		assertInstanceOfFails(RuntimeException.class, new IOException(), "type");
	}

	@Test
	void assertInstanceOfFailsSuperTypeExceptionValue() {
		assertInstanceOfFails(IllegalArgumentException.class, new RuntimeException(), "type");
	}

	private static class BaseClass {
	}

	private static class SubClass extends BaseClass {

	}

	@Test
	void assertInstanceOfFailsSuperTypeValue() {
		assertInstanceOfFails(SubClass.class, new BaseClass(), "type");
	}

	@Test
	void assertInstanceOfSucceedsSameTypeValue() {
		assertInstanceOfSucceeds(String.class, "indeed a String");
		assertInstanceOfSucceeds(BaseClass.class, new BaseClass());
		assertInstanceOfSucceeds(SubClass.class, new SubClass());
	}

	@Test
	void assertInstanceOfSucceedsExpectSuperClassOfValue() {
		assertInstanceOfSucceeds(CharSequence.class, "indeed a CharSequence");
		assertInstanceOfSucceeds(BaseClass.class, new SubClass());
	}

	@Test
	void assertInstanceOfSucceedsSameTypeExceptionValue() {
		assertInstanceOfSucceeds(UnsupportedOperationException.class, new UnsupportedOperationException());
	}

	@Test
	void assertInstanceOfSucceedsExpectSuperClassOfExceptionValue() {
		assertInstanceOfSucceeds(RuntimeException.class, new IllegalArgumentException("is a RuntimeException"));
	}

	private <T> void assertInstanceOfSucceeds(Class<T> expectedType, Object actualValue) {
		T res = assertInstanceOf(expectedType, actualValue);
		assertSame(res, actualValue);
		res = assertInstanceOf(expectedType, actualValue, "extra");
		assertSame(res, actualValue);
		res = assertInstanceOf(expectedType, actualValue, () -> "extra");
		assertSame(res, actualValue);
	}

	private void assertInstanceOfFails(Class<?> expectedType, @Nullable Object actualValue, String unexpectedSort) {
		String valueType = actualValue == null ? "null" : actualValue.getClass().getCanonicalName();
		String expectedMessage = "Unexpected %s, expected: <%s> but was: <%s>".formatted(unexpectedSort,
			expectedType.getCanonicalName(), valueType);
		Throwable expectedCause = actualValue instanceof Throwable throwable ? throwable : null;

		assertThrowsWithMessage(expectedMessage, expectedCause, () -> assertInstanceOf(expectedType, actualValue));
		assertThrowsWithMessage("extra ==> " + expectedMessage, expectedCause,
			() -> assertInstanceOf(expectedType, actualValue, "extra"));
		assertThrowsWithMessage("extra ==> " + expectedMessage, expectedCause,
			() -> assertInstanceOf(expectedType, actualValue, () -> "extra"));
	}

	private void assertThrowsWithMessage(String expectedMessage, @Nullable Throwable expectedCause,
			Executable executable) {
		Throwable throwable = assertThrows(AssertionFailedError.class, executable);
		assertEquals(expectedMessage, throwable.getMessage());
		assertSame(expectedCause, throwable.getCause());
	}
}
