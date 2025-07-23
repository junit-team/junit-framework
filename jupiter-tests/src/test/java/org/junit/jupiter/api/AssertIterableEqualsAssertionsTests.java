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

import static org.junit.jupiter.api.AssertionTestUtils.assertMessageContains;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEndsWith;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.IterableFactory.listOf;
import static org.junit.jupiter.api.IterableFactory.setOf;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertIterableEqualsAssertionsTests {

	@Test
	void assertIterableEqualsEqualToSelf() {
		List<Object> list = listOf("a", 'b', 1, 2);
		assertIterableEquals(list, list);
		assertIterableEquals(list, list, "message");
		assertIterableEquals(list, list, () -> "message");

		Set<Object> set = setOf("a", 'b', 1, 2);
		assertIterableEquals(set, set);
	}

	@Test
	void assertIterableEqualsEqualObjectsOfSameType() {
		assertIterableEquals(listOf(), listOf());
		assertIterableEquals(listOf("abc"), listOf("abc"));
		assertIterableEquals(listOf("abc", 1, 2L, 3D), listOf("abc", 1, 2L, 3D));
		assertIterableEquals(setOf(), setOf());
		assertIterableEquals(setOf("abc"), setOf("abc"));
		assertIterableEquals(setOf("abc", 1, 2L, 3D), setOf("abc", 1, 2L, 3D));
	}

	@Test
	void assertIterableEqualsNestedIterables() {
		assertIterableEquals(listOf(listOf(listOf())), listOf(listOf(listOf())));
		assertIterableEquals(setOf(setOf(setOf())), setOf(setOf(setOf())));
	}

	@Test
	void assertIterableEqualsNestedIterablesWithNull() {
		assertIterableEquals(listOf(null, listOf(null, listOf(null, null)), null, listOf((List<Object>) null)),
			listOf(null, listOf(null, listOf(null, null)), null, listOf((List<Object>) null)));
		assertIterableEquals(setOf(null, setOf(null, setOf(null, null)), null, setOf((Set<Object>) null)),
			setOf(null, setOf(null, setOf(null, null)), null, setOf((Set<Object>) null)));
	}

	@Test
	void assertIterableEqualsNestedIterablesWithStrings() {
		assertIterableEquals(listOf("a", listOf(listOf("b", listOf("c", "d"))), "e"),
			listOf("a", listOf(listOf("b", listOf("c", "d"))), "e"));
		assertIterableEquals(setOf("a", setOf(setOf("b", setOf("c", "d"))), "e"),
			setOf("a", setOf(setOf("b", setOf("c", "d"))), "e"));
	}

	@Test
	void assertIterableEqualsNestedIterablesWithIntegers() {
		assertIterableEquals(listOf(listOf(1), listOf(2), listOf(listOf(3, listOf(4)))),
			listOf(listOf(1), listOf(2), listOf(listOf(3, listOf(4)))));
		assertIterableEquals(setOf(setOf(1), setOf(2), setOf(setOf(3, setOf(4)))),
			setOf(setOf(1), setOf(2), setOf(setOf(3, setOf(4)))));
		assertIterableEquals(listOf(listOf(1), listOf(listOf(1))), setOf(setOf(1), setOf(setOf(1))));
	}

	@Test
	void assertIterableEqualsNestedIterablesWithDeeplyNestedObject() {
		assertIterableEquals(listOf(listOf(listOf(listOf(listOf(listOf(listOf("abc"))))))),
			listOf(listOf(listOf(listOf(listOf(listOf(listOf("abc"))))))));
		assertIterableEquals(setOf(setOf(setOf(setOf(setOf(setOf(setOf("abc"))))))),
			setOf(setOf(setOf(setOf(setOf(setOf(setOf("abc"))))))));
	}

	@Test
	void assertIterableEqualsNestedIterablesWithNaN() {
		assertIterableEquals(listOf(null, listOf(null, Double.NaN, listOf(Float.NaN, null, listOf()))),
			listOf(null, listOf(null, Double.NaN, listOf(Float.NaN, null, listOf()))));
		assertIterableEquals(setOf(null, setOf(null, Double.NaN, setOf(Float.NaN, null, setOf()))),
			setOf(null, setOf(null, Double.NaN, setOf(Float.NaN, null, setOf()))));
	}

	@Test
	void assertIterableEqualsNestedIterablesWithObjectsOfDifferentTypes() {
		assertIterableEquals(listOf(new String("a"), Integer.valueOf(1), listOf(Double.parseDouble("1.1"), "b")),
			listOf(new String("a"), Integer.valueOf(1), listOf(Double.parseDouble("1.1"), "b")));
		assertIterableEquals(setOf(new String("a"), Integer.valueOf(1), setOf(Double.parseDouble("1.1"), "b")),
			setOf(new String("a"), Integer.valueOf(1), setOf(Double.parseDouble("1.1"), "b")));
	}

	@Test
	void assertIterableEqualsNestedIterablesOfMixedSubtypes() {
		assertIterableEquals(
			listOf(1, 2, listOf(3, setOf(4, 5), setOf(6L), listOf(listOf(setOf(7)))), setOf(8), listOf(setOf(9L))),
			listOf(1, 2, listOf(3, setOf(4, 5), setOf(6L), listOf(listOf(setOf(7)))), setOf(8), listOf(setOf(9L))));

		assertIterableEquals(
			listOf("a", setOf('b', 'c'), setOf((int) 'd'), listOf(listOf(listOf("ef"), listOf(listOf("ghi"))))),
			setOf("a", listOf('b', 'c'), listOf((int) 'd'), setOf(setOf(setOf("ef"), setOf(setOf("ghi"))))));
	}

	@Test
	void assertIterableEqualsIterableVsNull() {
		try {
			assertIterableEquals(null, listOf("a", "b", 1, listOf()));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected iterable was <null>");
		}

		try {
			assertIterableEquals(listOf('a', 1, new Object(), 10L), null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual iterable was <null>");
		}
	}

	@Test
	void assertIterableEqualsNestedIterableVsNull() {
		try {
			assertIterableEquals(listOf(listOf(), 1, "2", setOf('3', listOf((List<Object>) null))),
				listOf(listOf(), 1, "2", setOf('3', listOf(listOf("4")))));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected iterable was <null> at index [3][1][0]");
		}

		try {
			assertIterableEquals(setOf(1, 2, listOf(3, listOf("4", setOf(5, setOf(6)))), "7"),
				setOf(1, 2, listOf(3, listOf("4", setOf(5, null))), "7"));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual iterable was <null> at index [2][1][1][1]");
		}
	}

	@Test
	void assertIterableEqualsIterableVsNullAndMessage() {
		try {
			assertIterableEquals(null, listOf('a', "b", 10, 20D), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected iterable was <null>");
		}

		try {
			assertIterableEquals(listOf("hello", 42), null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual iterable was <null>");
		}
	}

	@Test
	void assertIterableEqualsNestedIterableVsNullAndMessage() {
		try {
			assertIterableEquals(listOf(1, listOf(2, 3, listOf(4, 5, listOf((List<Object>) null)))),
				listOf(1, listOf(2, 3, listOf(4, 5, listOf(listOf(6))))), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected iterable was <null> at index [1][2][2][0]");
		}

		try {
			assertIterableEquals(listOf(1, listOf(2, listOf(3, listOf(listOf(4))))),
				listOf(1, listOf(2, listOf(3, listOf((List<Object>) null)))), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual iterable was <null> at index [1][1][1][0]");
		}
	}

	@Test
	void assertIterableEqualsIterableVsNullAndMessageSupplier() {
		try {
			assertIterableEquals(null, setOf(42, "42", listOf(42F), 42D), () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected iterable was <null>");
		}

		try {
			assertIterableEquals(listOf(listOf("a"), listOf()), null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual iterable was <null>");
		}
	}

	@Test
	void assertIterableEqualsNestedIterableVsNullAndMessageSupplier() {
		try {
			assertIterableEquals(listOf("1", "2", "3", listOf("4", listOf((List<Object>) null))),
				listOf("1", "2", "3", listOf("4", listOf(listOf(5)))), () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected iterable was <null> at index [3][1][0]");
		}

		try {
			assertIterableEquals(setOf(1, 2, setOf("3", setOf('4', setOf(5, 6, setOf())))),
				setOf(1, 2, setOf("3", setOf('4', setOf(5, 6, null)))), () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual iterable was <null> at index [2][1][1][2]");
		}
	}

	@Test
	void assertIterableEqualsIterablesOfDifferentLength() {
		try {
			assertIterableEquals(listOf('a', "b", 'c'), listOf('a', "b", 'c', 1));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "iterable lengths differ, expected: <3> but was: <4>");
		}
	}

	@Test
	void assertIterableEqualsNestedIterablesOfDifferentLength() {
		try {
			assertIterableEquals(listOf("a", setOf("b", listOf("c", "d", setOf("e", 1, 2, 3)))),
				listOf("a", setOf("b", listOf("c", "d", setOf("e", 1, 2, 3, 4, 5)))));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "iterable lengths differ at index [1][1][2], expected: <4> but was: <6>");
		}

		try {
			assertIterableEquals(listOf(listOf(listOf(listOf(listOf(listOf(listOf('a'))))))),
				listOf(listOf(listOf(listOf(listOf(listOf(listOf('a', 'b'))))))));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "iterable lengths differ at index [0][0][0][0][0][0], expected: <1> but was: <2>");
		}
	}

	@Test
	void assertIterableEqualsIterablesOfDifferentLengthAndMessage() {
		try {
			assertIterableEquals(setOf('a', 1), setOf('a', 1, new Object()), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertIterableEqualsNestedIterablesOfDifferentLengthAndMessage() {
		try {
			assertIterableEquals(listOf('a', 1, listOf(2, 3)), listOf('a', 1, listOf(2, 3, 4, 5)), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable lengths differ at index [2], expected: <2> but was: <4>");
		}
	}

	@Test
	void assertIterableEqualsIterablesOfDifferentLengthAndMessageSupplier() {
		try {
			assertIterableEquals(setOf("a", "b", "c"), setOf("a", "b", "c", "d", "e", "f"), () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable lengths differ, expected: <3> but was: <6>");
		}
	}

	@Test
	void assertIterableEqualsNestedIterablesOfDifferentLengthAndMessageSupplier() {
		try {
			assertIterableEquals(listOf("a", setOf(1, 2, 3, listOf(4.0, 5.1, 6.1), 7)),
				listOf("a", setOf(1, 2, 3, listOf(4.0, 5.1, 6.1, 7.0), 8)), () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable lengths differ at index [1][3], expected: <3> but was: <4>");
		}
	}

	@Test
	void assertIterableEqualsDifferentIterables() {
		try {
			assertIterableEquals(listOf(1L, "2", '3', 4, 5D), listOf(1L, "2", '9', 4, 5D));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "iterable contents differ at index [2], expected: <3> but was: <9>");
		}

		try {
			assertIterableEquals(listOf("a", 10, 11, 12, Double.NaN), listOf("a", 10, 11, 12, 13.55D));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "iterable contents differ at index [4], expected: <NaN> but was: <13.55>");
		}
	}

	@Test
	void assertIterableEqualsDifferentNestedIterables() {
		try {
			assertIterableEquals(listOf(1, 2, listOf(3, listOf(4, listOf(false, true)))),
				listOf(1, 2, listOf(3, listOf(4, listOf(true, false)))));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex,
				"iterable contents differ at index [2][1][1][0], expected: <false> but was: <true>");
		}

		List<Object> differentElement = listOf();
		try {
			assertIterableEquals(listOf(1, 2, 3, listOf(listOf(4, listOf(5)))),
				listOf(1, 2, 3, listOf(listOf(4, listOf(differentElement)))));
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex,
				"iterable contents differ at index [3][0][1][0], expected: <5> but was: <" + differentElement + ">");
		}
	}

	@Test
	void assertIterableEqualsDifferentIterablesAndMessage() {
		try {
			assertIterableEquals(listOf(1.1D, 2L, "3"), listOf(1D, 2L, "3"), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable contents differ at index [0], expected: <1.1> but was: <1.0>");
		}
	}

	@Test
	void assertIterableEqualsDifferentNestedIterablesAndMessage() {
		try {
			assertIterableEquals(listOf(9, 8, '6', listOf(5, 4, "3", listOf("2", '1'))),
				listOf(9, 8, '6', listOf(5, 4, "3", listOf("99", '1'))), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageContains(ex, "iterable contents differ at index [3][3][0], expected: <2> but was: <99>");
		}

		try {
			assertIterableEquals(listOf(9, 8, '6', listOf(5, 4, "3", listOf("2", "1"))),
				listOf(9, 8, '6', listOf(5, 4, "3", listOf("99", "1"))), "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageContains(ex, "iterable contents differ at index [3][3][0], expected: <2> but was: <99>");
		}
	}

	@Test
	void assertIterableEqualsDifferentIterablesAndMessageSupplier() {
		try {
			assertIterableEquals(setOf("one", 1L, Double.MIN_VALUE, "abc"), setOf("one", 1L, 42.42, "abc"),
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable contents differ at index [2], expected: <4.9E-324> but was: <42.42>");
		}
	}

	@Test
	void assertIterableEqualsDifferentNestedIterablesAndMessageSupplier() {
		try {
			assertIterableEquals(setOf("one", 1L, setOf("a", 'b', setOf(1, setOf(2, 3))), "abc"),
				setOf("one", 1L, setOf("a", 'b', setOf(1, setOf(2, 4))), "abc"), () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable contents differ at index [2][2][1][1], expected: <3> but was: <4>");
		}

		try {
			assertIterableEquals(listOf("j", listOf("a"), setOf(42), "ab", setOf(1, listOf(3))),
				listOf("j", listOf("a"), setOf(42), "ab", setOf(1, listOf(5))), () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "iterable contents differ at index [4][1][0], expected: <3> but was: <5>");
		}
	}

	@Test
	// https://github.com/junit-team/junit-framework/issues/2157
	void assertIterableEqualsWithListOfPath() {
		var expected = listOf(Path.of("1"));
		var actual = listOf(Path.of("1"));
		assertDoesNotThrow(() -> assertIterableEquals(expected, actual));
	}

	@Test
	void assertIterableEqualsThrowsStackOverflowErrorForInterlockedRecursiveStructures() {
		var expected = new ArrayList<>();
		var actual = new ArrayList<>();
		actual.add(expected);
		expected.add(actual);
		assertThrows(StackOverflowError.class, () -> assertIterableEquals(expected, actual));
	}

	@Test
	// https://github.com/junit-team/junit-framework/issues/2915
	void assertIterableEqualsWithDifferentListOfPath() {
		try {
			var expected = listOf(Path.of("1").resolve("2"));
			var actual = listOf(Path.of("1").resolve("3"));
			assertIterableEquals(expected, actual);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "iterable contents differ at index [0][1], expected: <2> but was: <3>");
		}
	}

}
