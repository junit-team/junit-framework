/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

/**
 * {@code Assertions} is a collection of selected assertion utility methods
 * from JUnit Jupiter for use within the JUnit Platform Test Kit.
 *
 * @since 1.4
 */
class Assertions {

	@FunctionalInterface
	interface Executable {
		void execute() throws Throwable;
	}

	static void assertAll(String heading, Stream<Executable> executables) {
		Preconditions.notNull(executables, "executables stream must not be null");

		List<Throwable> failures = executables //
				.map(executable -> {
					Preconditions.notNull(executable, "individual executables must not be null");
					try {
						executable.execute();
						return null;
					}
					catch (Throwable t) {
						UnrecoverableExceptions.rethrowIfUnrecoverable(t);
						return t;
					}
				}) //
				.filter(Objects::nonNull) //
				.toList();

		if (!failures.isEmpty()) {
			MultipleFailuresError multipleFailuresError = new MultipleFailuresError(heading, failures);
			failures.forEach(multipleFailuresError::addSuppressed);
			throw multipleFailuresError;
		}
	}

	static void assertEquals(long expected, long actual, String message) {
		if (expected != actual) {
			failNotEqual(expected, actual, message);
		}
	}

	private static void failNotEqual(long expected, long actual, String message) {
		fail(format(expected, actual, message), expected, actual);
	}

	private static void fail(String message, Object expected, Object actual) {
		throw new AssertionFailedError(message, expected, actual);
	}

	private static String format(long expected, long actual, String message) {
		return buildPrefix(message) + formatValues(expected, actual);
	}

	private static String buildPrefix(String message) {
		return (StringUtils.isNotBlank(message) ? message + " ==> " : "");
	}

	private static String formatValues(long expected, long actual) {
		return "expected: <%d> but was: <%d>".formatted(expected, actual);
	}

	private Assertions() {
	}

}
