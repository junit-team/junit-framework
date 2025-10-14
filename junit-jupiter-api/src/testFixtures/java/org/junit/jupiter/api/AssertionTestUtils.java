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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;
import org.opentest4j.ValueWrapper;

public class AssertionTestUtils {

	private AssertionTestUtils() {
		/* no-op */
	}

	public static void expectAssertionFailedError() {
		throw new AssertionError("Should have thrown an " + AssertionFailedError.class.getName());
	}

	public static void assertEmptyMessage(Throwable ex) throws AssertionError {
		if (!(ex.getMessage() == null || ex.getMessage().isEmpty())) {
			throw new AssertionError("Exception message should be empty, but was [" + ex.getMessage() + "].");
		}
	}

	public static void assertMessageEquals(Throwable ex, String msg) throws AssertionError {
		if (!msg.equals(ex.getMessage())) {
			throw new AssertionError("Exception message should be [" + msg + "], but was [" + ex.getMessage() + "].");
		}
	}

	public static void assertMessageMatches(Throwable ex, String regex) throws AssertionError {
		if (ex.getMessage() == null || !ex.getMessage().matches(regex)) {
			throw new AssertionError("Exception message should match regular expression [" + regex + "], but was ["
					+ ex.getMessage() + "].");
		}
	}

	public static void assertMessageStartsWith(@Nullable Throwable ex, String msg) throws AssertionError {
		if (ex == null) {
			throw new AssertionError("Cause should not have been null");
		}
		if (ex.getMessage() == null || !ex.getMessage().startsWith(msg)) {
			throw new AssertionError(
				"Exception message should start with [" + msg + "], but was [" + ex.getMessage() + "].");
		}
	}

	public static void assertMessageEndsWith(Throwable ex, String msg) throws AssertionError {
		if (ex.getMessage() == null || !ex.getMessage().endsWith(msg)) {
			throw new AssertionError(
				"Exception message should end with [" + msg + "], but was [" + ex.getMessage() + "].");
		}
	}

	public static void assertMessageContains(@Nullable Throwable ex, String msg) throws AssertionError {
		if (ex == null) {
			throw new AssertionError("Cause should not have been null");
		}
		if (ex.getMessage() == null || !ex.getMessage().contains(msg)) {
			throw new AssertionError(
				"Exception message should contain [" + msg + "], but was [" + ex.getMessage() + "].");
		}
	}

	public static void assertExpectedAndActualValues(AssertionFailedError ex, @Nullable Object expected,
			@Nullable Object actual) throws AssertionError {
		if (!wrapsEqualValue(ex.getExpected(), expected)) {
			throw new AssertionError("Expected value in AssertionFailedError should equal ["
					+ ValueWrapper.create(expected) + "], but was [" + ex.getExpected() + "].");
		}
		if (!wrapsEqualValue(ex.getActual(), actual)) {
			throw new AssertionError("Actual value in AssertionFailedError should equal [" + ValueWrapper.create(actual)
					+ "], but was [" + ex.getActual() + "].");
		}
	}

	public static boolean wrapsEqualValue(ValueWrapper wrapper, @Nullable Object value) {
		if (value == null || value instanceof Serializable) {
			return Objects.equals(value, wrapper.getValue());
		}
		return wrapper.getIdentityHashCode() == System.identityHashCode(value)
				&& Objects.equals(wrapper.getStringRepresentation(), String.valueOf(value))
				&& Objects.equals(wrapper.getType(), value.getClass());
	}

	public static void recurseIndefinitely() {
		// simulate infinite recursion
		throw new StackOverflowError();
	}

	public static void runOutOfMemory() {
		// simulate running out of memory
		throw new OutOfMemoryError("boom");
	}

	@SafeVarargs
	public static void assertExpectedExceptionTypes(MultipleFailuresError multipleFailuresError,
			Class<? extends Throwable>... exceptionTypes) {

		assertNotNull(multipleFailuresError, "MultipleFailuresError");
		List<Throwable> failures = multipleFailuresError.getFailures();
		assertEquals(exceptionTypes.length, failures.size(), "number of failures");

		// Verify that exceptions are also present as suppressed exceptions.
		// https://github.com/junit-team/junit-framework/issues/1602
		Throwable[] suppressed = multipleFailuresError.getSuppressed();
		assertEquals(exceptionTypes.length, suppressed.length, "number of suppressed exceptions");

		for (int i = 0; i < exceptionTypes.length; i++) {
			assertEquals(exceptionTypes[i], failures.get(i).getClass(), "exception type [" + i + "]");
			assertEquals(exceptionTypes[i], suppressed[i].getClass(), "suppressed exception type [" + i + "]");
		}
	}
}
