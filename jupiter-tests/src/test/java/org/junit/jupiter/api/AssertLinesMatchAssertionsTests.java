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

import static org.junit.jupiter.api.AssertLinesMatch.isFastForwardLine;
import static org.junit.jupiter.api.AssertLinesMatch.parseFastForwardLimit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertLinesMatchAssertionsTests {

	@Test
	void assertLinesMatchEmptyLists() {
		assertLinesMatch(Collections.emptyList(), new ArrayList<>());
	}

	@Test
	void assertLinesMatchSameListInstance() {
		List<String> list = List.of("first line", "second line", "third line", "last line");
		assertLinesMatch(list, list);
	}

	@Test
	void assertLinesMatchPlainEqualLists() {
		List<String> expected = List.of("first line", "second line", "third line", "last line");
		List<String> actual = List.of("first line", "second line", "third line", "last line");
		assertLinesMatch(expected, actual);
	}

	@Test
	void assertLinesMatchUsingRegexPatterns() {
		List<String> expected = List.of("^first.+line", "second\\s*line", "th.rd l.ne", "last line$");
		List<String> actual = List.of("first line", "second line", "third line", "last line");
		assertLinesMatch(expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerAtEndOfExpectedLines() {
		List<String> expected = List.of("first line", ">> ignore all following lines >>");
		List<String> actual = List.of("first line", "I", "II", "III", "IV", "V", "VI", "last line");
		assertLinesMatch(expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarker() {
		List<String> expected = List.of("first line", ">> skip lines until next matches >>", "V", "last line");
		List<String> actual = List.of("first line", "I", "II", "III", "IV", "V", "last line");
		assertLinesMatch(expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerWithLimit1() {
		List<String> expected = List.of("first line", ">> 1 >>", "last line");
		List<String> actual = List.of("first line", "skipped", "last line");
		assertLinesMatch(expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerWithLimit3() {
		List<String> expected = List.of(">> 3 >>");
		List<String> actual = List.of("first line", "skipped", "last line");
		assertLinesMatch(expected, actual);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes", "DataFlowIssue" })
	void assertLinesMatchWithNullFails() {
		assertPreconditionViolationFor(() -> assertLinesMatch(null, (List) null));
		assertPreconditionViolationFor(() -> assertLinesMatch(null, Collections.emptyList()));
		assertPreconditionViolationFor(() -> assertLinesMatch(Collections.emptyList(), null));
	}

	@Test
	void assertLinesMatchWithNullElementsFails() {
		var list = List.of("1", "2", "3");
		var withNullElement = Arrays.asList("1", null, "3"); // List.of() doesn't permit null values.
		assertDoesNotThrow(() -> assertLinesMatch(withNullElement, withNullElement));
		assertPreconditionViolationNotNullFor("expected line", () -> assertLinesMatch(withNullElement, list));
		assertPreconditionViolationNotNullFor("actual line", () -> assertLinesMatch(list, withNullElement));
	}

	private void assertError(AssertionFailedError error, String expectedMessage, List<String> expectedLines,
			List<String> actualLines) {
		assertEquals(expectedMessage, error.getMessage());
		assertEquals(String.join(System.lineSeparator(), expectedLines), error.getExpected().getStringRepresentation());
		assertEquals(String.join(System.lineSeparator(), actualLines), error.getActual().getStringRepresentation());
	}

	@Test
	void assertLinesMatchMoreExpectedThanActualAvailableFails() {
		var expected = List.of("first line", "second line", "third line");
		var actual = List.of("first line", "third line");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		assertError(error, "expected 3 lines, but only got 2", expected, actual);
	}

	@Test
	void assertLinesMatchFailsWithDescriptiveErrorMessage() {
		var expected = List.of("first line", "second line", "third line");
		var actual = List.of("first line", "sec0nd line", "third line");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		var expectedMessage = String.join(System.lineSeparator(), List.of( //
			"expected line #2 doesn't match actual line #2", //
			"\texpected: `second line`", //
			"\t  actual: `sec0nd line`"));
		assertError(error, expectedMessage, expected, actual);
	}

	@Test
	void assertLinesMatchMoreActualLinesThenExpectedFails() {
		var expected = List.of("first line", "second line", "third line");
		var actual = List.of("first line", "second line", "third line", "last line");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		assertError(error, "more actual lines than expected: 1", expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerWithTooLowLimitFails() {
		var expected = List.of("first line", ">> 1 >>");
		var actual = List.of("first line", "skipped", "last line");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		assertError(error, "terminal fast-forward(1) error: fast-forward(2) expected", expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerWithTooHighLimitFails() {
		var expected = List.of("first line", ">> 100 >>");
		var actual = List.of("first line", "skipped", "last line");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		assertError(error, "terminal fast-forward(100) error: fast-forward(2) expected", expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerWithTooHighLimitAndFollowingLineFails() {
		/*
		 * It is important here that the line counts are expected <= actual, that the
		 * fast-forward exceeds the available actual lines and that it is not a
		 * terminal fast-forward.
		 */
		var expected = List.of("first line", ">> 3 >>", "not present");
		var actual = List.of("first line", "first skipped", "second skipped");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		assertError(error, "fast-forward(3) error: not enough actual lines remaining (2)", expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerWithoutMatchingNextLineFails() {
		var expected = List.of("first line", ">> fails, because next line is >>", "not present");
		var actual = List.of("first line", "skipped", "last line");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		assertError(error, "fast-forward(∞) didn't find: `not present`", expected, actual);
	}

	@Test
	void assertLinesMatchUsingFastForwardMarkerWithExtraExpectLineFails() {
		var expected = List.of("first line", ">> fails, because final line is missing >>", "last line", "not present");
		var actual = List.of("first line", "first skipped", "second skipped", "last line");
		var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual));
		assertError(error, "expected line #4:`not present` not found - actual lines depleted", expected, actual);
	}

	@Test
	void assertLinesMatchIsFastForwardLine() {
		assertAll("valid fast-forward lines", //
			() -> assertTrue(isFastForwardLine(">>>>")), () -> assertTrue(isFastForwardLine(">> >>")),
			() -> assertTrue(isFastForwardLine(">> stacktrace >>")),
			() -> assertTrue(isFastForwardLine(">> single line, non Integer.parse()-able comment >>")),
			() -> assertTrue(isFastForwardLine(">>9>>")), () -> assertTrue(isFastForwardLine(">> 9 >>")),
			() -> assertTrue(isFastForwardLine(">> -9 >>")), () -> assertTrue(isFastForwardLine(" >> 9 >> ")),
			() -> assertTrue(isFastForwardLine("  >> 9 >>  ")));
	}

	@Test
	void assertLinesMatchParseFastForwardLimit() {
		assertAll("valid fast-forward limits", //
			() -> assertEquals(Integer.MAX_VALUE, parseFastForwardLimit(">>>>")),
			() -> assertEquals(Integer.MAX_VALUE, parseFastForwardLimit(">> >>")),
			() -> assertEquals(Integer.MAX_VALUE, parseFastForwardLimit(">> stacktrace >>")),
			() -> assertEquals(Integer.MAX_VALUE, parseFastForwardLimit(">> non Integer.parse()-able comment >>")),
			() -> assertEquals(9, parseFastForwardLimit(">>9>>")),
			() -> assertEquals(9, parseFastForwardLimit(">> 9 >>")),
			() -> assertEquals(9, parseFastForwardLimit(" >> 9 >> ")),
			() -> assertEquals(9, parseFastForwardLimit("  >> 9 >>  ")));
		assertPreconditionViolationFor(() -> parseFastForwardLimit(">>0>>"))//
				.withMessage("fast-forward(0) limit must be greater than zero");
		assertPreconditionViolationFor(() -> parseFastForwardLimit(">>-1>>"))//
				.withMessage("fast-forward(-1) limit must be greater than zero");
		assertPreconditionViolationFor(() -> parseFastForwardLimit(">>-2147483648>>"))//
				.withMessage("fast-forward(-2147483648) limit must be greater than zero");
	}

	@Test
	void assertLinesMatchMatches() {
		Random random = new Random();
		assertAll("do match", //
			() -> assertTrue(
				AssertLinesMatch.matches("duration: [\\d]+ ms", "duration: " + random.nextInt(1000) + " ms")),
			() -> assertTrue(AssertLinesMatch.matches("123", "123")),
			() -> assertTrue(AssertLinesMatch.matches(".*", "123")),
			() -> assertTrue(AssertLinesMatch.matches("\\d+", "123")));
		assertAll("don't match", //
			() -> assertFalse(
				AssertLinesMatch.matches("duration: [\\d]+ ms", "duration: " + random.nextGaussian() + " ms")),
			() -> assertFalse(AssertLinesMatch.matches("12", "123")),
			() -> assertFalse(AssertLinesMatch.matches("..+", "1")),
			() -> assertFalse(AssertLinesMatch.matches("\\d\\d+", "1")));
	}

	@Test
	void largeListsThatDoNotMatchAreTruncated() {
		var expected = IntStream.range(1, 999).boxed().map(Object::toString).toList();
		var actual = IntStream.range(0, 1000).boxed().map(Object::toString).toList();
		var error = assertThrows(AssertionFailedError.class,
			() -> assertLinesMatch(expected, actual, "custom message"));
		var expectedMessage = String.join(System.lineSeparator(), List.of( //
			"custom message ==> expected line #1 doesn't match actual line #1", //
			"\texpected: `1`", //
			"\t  actual: `0`"));
		assertError(error, expectedMessage, expected, actual);
	}

	/**
	 * @since 5.5
	 */
	@Nested
	class WithCustomFailureMessage {
		@Test
		void simpleStringMessage() {
			String message = "XXX";
			var expected = List.of("a", "b", "c");
			var actual = List.of("a", "d", "c");
			var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual, message));
			var expectedMessage = String.join(System.lineSeparator(), List.of( //
				message + " ==> expected line #2 doesn't match actual line #2", //
				"\texpected: `b`", //
				"\t  actual: `d`"));
			assertError(error, expectedMessage, expected, actual);
		}

		@Test
		void stringSupplierWithMultiLineMessage() {
			var message = "XXX\nYYY";
			Supplier<@Nullable String> supplier = () -> message;
			var expected = List.of("a", "b", "c");
			var actual = List.of("a", "d", "c");
			var error = assertThrows(AssertionFailedError.class, () -> assertLinesMatch(expected, actual, supplier));
			var expectedMessage = String.join(System.lineSeparator(), List.of( //
				message + " ==> expected line #2 doesn't match actual line #2", //
				"\texpected: `b`", //
				"\t  actual: `d`"));
			assertError(error, expectedMessage, expected, actual);
		}
	}

	@Nested
	class WithStreamsOfStrings {
		@Test
		void assertLinesMatchEmptyStreams() {
			assertLinesMatch(Stream.empty(), Stream.empty());
		}

		@Test
		void assertLinesMatchSameListInstance() {
			Stream<String> stream = Stream.of("first line", "second line", "third line", "last line");
			assertLinesMatch(stream, stream);
		}

		@Test
		void assertLinesMatchPlainEqualLists() {
			var expected = """
					first line
					second line
					third line
					last line
					""";
			var actual = """
					first line
					second line
					third line
					last line
					""";
			assertLinesMatch(expected.lines(), actual.lines());
		}

		@Test
		void assertLinesMatchUsingRegexPatterns() {
			var expected = """
					^first.+line
					second\\s*line
					th.rd l.ne
					last line$
					""";
			var actual = """
					first line
					second line
					third line
					last line
					""";
			assertLinesMatch(expected.lines(), actual.lines());
		}
	}

}
