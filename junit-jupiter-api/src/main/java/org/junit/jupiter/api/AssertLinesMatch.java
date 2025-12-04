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

import static java.lang.String.join;
import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.platform.commons.util.Preconditions.condition;
import static org.junit.platform.commons.util.Preconditions.notNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * {@code AssertLinesMatch} is a collection of utility methods that support asserting
 * lines of {@link String} equality or {@link java.util.regex.Pattern}-match in tests.
 *
 * @since 5.0
 */
class AssertLinesMatch {

	private AssertLinesMatch() {
		/* no-op */
	}

	private static final int MAX_SNIPPET_LENGTH = 21;

	static void assertLinesMatch(List<String> expectedLines, List<String> actualLines) {
		assertLinesMatch(expectedLines, actualLines, (Object) null);
	}

	static void assertLinesMatch(List<String> expectedLines, List<String> actualLines, @Nullable String message) {
		assertLinesMatch(expectedLines, actualLines, (Object) message);
	}

	static void assertLinesMatch(Stream<String> expectedLines, Stream<String> actualLines) {
		assertLinesMatch(expectedLines, actualLines, (Object) null);
	}

	static void assertLinesMatch(Stream<String> expectedLines, Stream<String> actualLines, @Nullable String message) {
		assertLinesMatch(expectedLines, actualLines, (Object) message);
	}

	static void assertLinesMatch(Stream<String> expectedLines, Stream<String> actualLines,
			@Nullable Object messageOrSupplier) {
		notNull(expectedLines, "expectedLines must not be null");
		notNull(actualLines, "actualLines must not be null");

		// trivial case: same stream instance
		if (expectedLines == actualLines) {
			return;
		}

		List<String> expectedListOfStrings = expectedLines.toList();
		List<String> actualListOfStrings = actualLines.toList();
		assertLinesMatch(expectedListOfStrings, actualListOfStrings, messageOrSupplier);
	}

	static void assertLinesMatch(List<String> expectedLines, List<String> actualLines,
			@Nullable Object messageOrSupplier) {
		notNull(expectedLines, "expectedLines must not be null");
		notNull(actualLines, "actualLines must not be null");

		// trivial case: same list instance
		if (expectedLines == actualLines) {
			return;
		}

		new LinesMatcher(expectedLines, actualLines, messageOrSupplier).assertLinesMatch();
	}

	private record LinesMatcher(List<String> expectedLines, List<String> actualLines,
			@Nullable Object messageOrSupplier) {

		void assertLinesMatch() {
			int expectedSize = expectedLines.size();
			int actualSize = actualLines.size();

			// trivial case: when expecting more than actual lines available, something is wrong
			if (expectedSize > actualSize) {
				fail("expected %d lines, but only got %d", expectedSize, actualSize);
			}

			// simple case: both list are equally sized, compare them line-by-line
			if (expectedSize == actualSize) {
				if (IntStream.range(0, expectedSize).allMatch(i -> matches(expectedLines.get(i), actualLines.get(i)))) {
					return;
				}
				// else fall-through to "with fast-forward" matching
			}

			assertLinesMatchWithFastForward();
		}

		void assertLinesMatchWithFastForward() {
			Deque<String> expectedDeque = new ArrayDeque<>(expectedLines);
			Deque<String> actualDeque = new ArrayDeque<>(actualLines);

			main: while (!expectedDeque.isEmpty()) {
				String expectedLine = expectedDeque.pop();
				int expectedLineNumber = expectedLines.size() - expectedDeque.size(); // 1-based line number
				// trivial case: no more actual lines available
				if (actualDeque.isEmpty()) {
					fail("expected line #%d:`%s` not found - actual lines depleted", expectedLineNumber,
						snippet(expectedLine));
				}

				String actualLine = actualDeque.peek();
				// trivial case: take the fast path when they match
				if (matches(expectedLine, actualLine)) {
					actualDeque.pop();
					continue; // main
				}

				// fast-forward marker found in expected line: fast-forward actual line...
				if (isFastForwardLine(expectedLine)) {
					int fastForwardLimit = parseFastForwardLimit(expectedLine);
					int actualRemaining = actualDeque.size();

					// trivial case: fast-forward marker was in last expected line
					if (expectedDeque.isEmpty()) {
						// no limit given or perfect match? we're done.
						if (fastForwardLimit == Integer.MAX_VALUE || fastForwardLimit == actualRemaining) {
							return;
						}
						fail("terminal fast-forward(%d) error: fast-forward(%d) expected", fastForwardLimit,
							actualRemaining);
					}

					// fast-forward limit was given: use it
					if (fastForwardLimit != Integer.MAX_VALUE) {
						if (actualRemaining < fastForwardLimit) {
							fail("fast-forward(%d) error: not enough actual lines remaining (%s)", fastForwardLimit,
								actualRemaining);
						}
						// fast-forward now: actualDeque.pop(fastForwardLimit)
						for (int i = 0; i < fastForwardLimit; i++) {
							actualDeque.pop();
						}
						continue; // main
					}

					// peek next expected line
					expectedLine = expectedDeque.peek();
					// fast-forward "unlimited": until next match
					while (true) {
						if (actualDeque.isEmpty()) {
							fail("fast-forward(∞) didn't find: `%s`", snippet(expectedLine));
						}
						if (matches(expectedLine, actualDeque.peek())) {
							continue main;
						}
						actualDeque.pop();
					}
				}

				int actualLineNumber = actualLines.size() - actualDeque.size() + 1; // 1-based line number
				fail("expected line #%d doesn't match actual line #%d%n" + "\texpected: `%s`%n" + "\t  actual: `%s`",
					expectedLineNumber, actualLineNumber, expectedLine, actualLine);
			}

			// after math
			if (!actualDeque.isEmpty()) {
				fail("more actual lines than expected: %d", actualDeque.size());
			}
		}

		String snippet(String line) {
			if (line.length() <= MAX_SNIPPET_LENGTH) {
				return line;
			}
			return line.substring(0, MAX_SNIPPET_LENGTH - 5) + "[...]";
		}

		void fail(String format, Object... args) {
			String newLine = System.lineSeparator();
			assertionFailure() //
					.message(messageOrSupplier) //
					.reason(format.formatted(args)) //
					.expected(join(newLine, expectedLines)) //
					.actual(join(newLine, actualLines)) //
					.includeValuesInMessage(false) //
					.buildAndThrow();
		}
	}

	static boolean isFastForwardLine(String line) {
		line = line.strip();
		return line.length() >= 4 && line.startsWith(">>") && line.endsWith(">>");
	}

	static int parseFastForwardLimit(String fastForwardLine) {
		fastForwardLine = fastForwardLine.strip();
		String text = fastForwardLine.substring(2, fastForwardLine.length() - 2).strip();
		try {
			int limit = Integer.parseInt(text);
			condition(limit > 0, () -> "fast-forward(%d) limit must be greater than zero".formatted(limit));
			return limit;
		}
		catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	static boolean matches(String expectedLine, String actualLine) {
		notNull(expectedLine, "expected line must not be null");
		notNull(actualLine, "actual line must not be null");
		if (expectedLine.equals(actualLine)) {
			return true;
		}
		try {
			return actualLine.matches(expectedLine);
		}
		catch (PatternSyntaxException ignore) {
			return false;
		}
	}

}
