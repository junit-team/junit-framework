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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertArrayEqualsAssertionsTests {

	@Test
	void assertArrayEqualsWithNulls() {
		assertArrayEquals(null, (boolean[]) null);
		assertArrayEquals(null, (char[]) null);
		assertArrayEquals(null, (byte[]) null);
		assertArrayEquals(null, (int[]) null);
		assertArrayEquals(null, (long[]) null);
		assertArrayEquals(null, (float[]) null);
		assertArrayEquals(null, (double[]) null);
		assertArrayEquals(null, (Object[]) null);
	}

	@Test
	void assertArrayEqualsBooleanArrays() {
		boolean[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new boolean[] {}, new boolean[] {});
		assertArrayEquals(new boolean[] {}, new boolean[] {}, "message");
		assertArrayEquals(new boolean[] {}, new boolean[] {}, () -> "message");
		assertArrayEquals(new boolean[] { true }, new boolean[] { true });
		assertArrayEquals(new boolean[] { false, true, false, false }, new boolean[] { false, true, false, false });
	}

	@Test
	void assertArrayEqualsBooleanArrayVsNull() {
		try {
			assertArrayEquals(null, new boolean[] { true, false });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new boolean[] { true, false }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsBooleanArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new boolean[] { true, false }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new boolean[] { true, false }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsBooleanArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new boolean[] { true, false }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new boolean[] { true, false }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsBooleanArraysOfDifferentLength() {
		try {
			assertArrayEquals(new boolean[] { true, false }, new boolean[] { true, false, true });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsBooleanArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new boolean[] { true, false, false }, new boolean[] { true }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <1>");
		}
	}

	@Test
	void assertArrayEqualsBooleanArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new boolean[] { true }, new boolean[] { true, false }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <1> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsDifferentBooleanArrays() {
		try {
			assertArrayEquals(new boolean[] { true, false, false }, new boolean[] { true, false, true });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [2], expected: <false> but was: <true>");
		}
	}

	@Test
	void assertArrayEqualsDifferentBooleanArraysAndMessage() {
		try {
			assertArrayEquals(new boolean[] { true, true }, new boolean[] { false, true }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [0], expected: <true> but was: <false>");
		}
	}

	@Test
	void assertArrayEqualsDifferentBooleanArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new boolean[] { false, false, false }, new boolean[] { false, true, true },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <false> but was: <true>");
		}
	}

	@Test
	void assertArrayEqualsCharArrays() {
		char[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new char[] {}, new char[] {});
		assertArrayEquals(new char[] {}, new char[] {}, "message");
		assertArrayEquals(new char[] {}, new char[] {}, () -> "message");
		assertArrayEquals(new char[] { 'a' }, new char[] { 'a' });
		assertArrayEquals(new char[] { 'j', 'u', 'n', 'i', 't' }, new char[] { 'j', 'u', 'n', 'i', 't' });
	}

	@Test
	void assertArrayEqualsCharArrayVsNull() {
		try {
			assertArrayEquals(null, new char[] { 'a', 'z' });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new char[] { 'a', 'z' }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsCharArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new char[] { 'a', 'b', 'z' }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new char[] { 'a', 'b', 'z' }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsCharArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new char[] { 'z', 'x', 'y' }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new char[] { 'z', 'x', 'y' }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsCharArraysOfDifferentLength() {
		try {
			assertArrayEquals(new char[] { 'q', 'w', 'e' }, new char[] { 'q', 'w' });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <3> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsCharArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new char[] { 'a', 's', 'd' }, new char[] { 'd' }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <1>");
		}
	}

	@Test
	void assertArrayEqualsCharArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new char[] { 'q' }, new char[] { 't', 'u' }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <1> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsDifferentCharArrays() {
		try {
			assertArrayEquals(new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'a' });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [2], expected: <c> but was: <a>");
		}
	}

	@Test
	void assertArrayEqualsDifferentCharArraysAndMessage() {
		try {
			assertArrayEquals(new char[] { 'z', 'x', 'c', 'v' }, new char[] { 'x', 'x', 'c', 'v' }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [0], expected: <z> but was: <x>");
		}
	}

	@Test
	void assertArrayEqualsDifferentCharArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new char[] { 'r', 't', 'y' }, new char[] { 'r', 'y', 'u' }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <t> but was: <y>");
		}
	}

	@Test
	void assertArrayEqualsByteArrays() {
		byte[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new byte[] {}, new byte[] {});
		assertArrayEquals(new byte[] {}, new byte[] {}, "message");
		assertArrayEquals(new byte[] {}, new byte[] {}, () -> "message");
		assertArrayEquals(new byte[] { 42 }, new byte[] { 42 });
		assertArrayEquals(new byte[] { 1, 2, 3, 42 }, new byte[] { 1, 2, 3, 42 });
	}

	@Test
	void assertArrayEqualsByteArrayVsNull() {
		try {
			assertArrayEquals(null, new byte[] { 7, 8, 9 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new byte[] { 7, 8, 9 }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsByteArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new byte[] { 9, 8, 7 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new byte[] { 9, 8, 7 }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsByteArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new byte[] { 10, 20, 30 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new byte[] { 10, 20, 30 }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsByteArraysOfDifferentLength() {
		try {
			assertArrayEquals(new byte[] { 1, 2, 100 }, new byte[] { 1, 2, 100, 101 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <3> but was: <4>");
		}
	}

	@Test
	void assertArrayEqualsByteArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new byte[] { 1, 2 }, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <2> but was: <9>");
		}
	}

	@Test
	void assertArrayEqualsByteArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new byte[] { 88, 99 }, new byte[] { 99, 88, 77 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsDifferentByteArrays() {
		try {
			assertArrayEquals(new byte[] { 12, 13, 12, 13 }, new byte[] { 12, 13, 12, 14 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [3], expected: <13> but was: <14>");
		}
	}

	@Test
	void assertArrayEqualsDifferentByteArraysAndMessage() {
		try {
			assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, new byte[] { 1, 2, 3, 5, 5 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [3], expected: <4> but was: <5>");
		}
	}

	@Test
	void assertArrayEqualsDifferentByteArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new byte[] { 127, 126, -128, +127 }, new byte[] { 127, 126, -128, -127 },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [3], expected: <127> but was: <-127>");
		}
	}

	@Test
	void assertArrayEqualsShortArrays() {
		short[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new short[] {}, new short[] {});
		assertArrayEquals(new short[] {}, new short[] {}, "message");
		assertArrayEquals(new short[] {}, new short[] {}, () -> "message");
		assertArrayEquals(new short[] { 999 }, new short[] { 999 });
		assertArrayEquals(new short[] { 111, 222, 333, 444, 999 }, new short[] { 111, 222, 333, 444, 999 });
	}

	@Test
	void assertArrayEqualsShortArrayVsNull() {
		try {
			assertArrayEquals(null, new short[] { 5, 10, 12 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new short[] { 5, 10, 12 }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsShortArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new short[] { 128, 129, 130 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new short[] { -129, -130, -131 }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsShortArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new short[] { 1, 2, 3, 4 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new short[] { -2000, 1, 2, 3, 4 }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsShortArraysOfDifferentLength() {
		try {
			assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, new short[] { 1, 2, 3 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <6> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsShortArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new short[] { 1, 2, 3, 10_000 }, new short[] { 10_000, 1, 2, 3, 4, 5, 6, 7 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <4> but was: <8>");
		}
	}

	@Test
	void assertArrayEqualsShortArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new short[] { 150, 151 }, new short[] { 150, 151, 152 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsDifferentShortArrays() {
		try {
			assertArrayEquals(new short[] { 10, 100, 1000, 10000 }, new short[] { 1, 10, 100, 1000 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [0], expected: <10> but was: <1>");
		}
	}

	@Test
	void assertArrayEqualsDifferentShortArraysAndMessage() {
		try {
			assertArrayEquals(new short[] { 1, 2, 100, -200 }, new short[] { 1, 2, 100, -500 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [3], expected: <-200> but was: <-500>");
		}
	}

	@Test
	void assertArrayEqualsDifferentShortArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new short[] { 1000, 2000, +3000, 42 }, new short[] { 1000, 2000, -3000, 42 },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [2], expected: <3000> but was: <-3000>");
		}
	}

	@Test
	void assertArrayEqualsIntArrays() {
		int[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new int[] {}, new int[] {});
		assertArrayEquals(new int[] {}, new int[] {}, "message");
		assertArrayEquals(new int[] {}, new int[] {}, () -> "message");
		assertArrayEquals(new int[] { Integer.MAX_VALUE }, new int[] { Integer.MAX_VALUE });
		assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 99_999 }, new int[] { 1, 2, 3, 4, 5, 99_999 });
	}

	@Test
	void assertArrayEqualsIntArrayVsNull() {
		try {
			assertArrayEquals(null, new int[] { Integer.MIN_VALUE, 2, 10 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new int[] { Integer.MIN_VALUE, 2, 10 }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsIntArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new int[] { 99_999, 88_888, 1 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new int[] { 99_999, 77_7777, 2 }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsIntArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new int[] { 1, 10, 100, 1000, 10000, 100000 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new int[] { 100000, 10000, 1000, 100, 10, 1 }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsIntArraysOfDifferentLength() {
		try {
			assertArrayEquals(new int[] { 1, 2, 3, Integer.MIN_VALUE, 4 }, new int[] { 1, Integer.MAX_VALUE, 2 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <5> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsIntArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new int[] { 100_000, 200_000, 1, 2 }, new int[] { 1, 2, 3 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <4> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsIntArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE }, new int[] { 1, 2, 3 },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsDifferentIntArrays() {
		try {
			assertArrayEquals(new int[] { Integer.MIN_VALUE, 1, 2, 10 }, new int[] { Integer.MIN_VALUE, 1, 10, 10 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [2], expected: <2> but was: <10>");
		}
	}

	@Test
	void assertArrayEqualsDifferentIntArraysAndMessage() {
		try {
			assertArrayEquals(new int[] { 9, 10, 100, 100_000, 7 }, new int[] { 9, 10, 100, 100_000, 200_000 },
				"message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [4], expected: <7> but was: <200000>");
		}
	}

	@Test
	void assertArrayEqualsDifferentIntArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new int[] { 1, Integer.MIN_VALUE, 2 }, new int[] { 1, Integer.MAX_VALUE, 2 },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex,
				"array contents differ at index [1], expected: <-2147483648> but was: <2147483647>");
		}
	}

	@Test
	void assertArrayEqualsLongArrays() {
		long[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new long[] {}, new long[] {});
		assertArrayEquals(new long[] {}, new long[] {}, "message");
		assertArrayEquals(new long[] {}, new long[] {}, () -> "message");
		assertArrayEquals(new long[] { Long.MAX_VALUE }, new long[] { Long.MAX_VALUE });
		assertArrayEquals(new long[] { Long.MIN_VALUE, 10, 20, 30 }, new long[] { Long.MIN_VALUE, 10, 20, 30 });
	}

	@Test
	void assertArrayEqualsLongArrayVsNull() {
		try {
			assertArrayEquals(null, new long[] { Long.MAX_VALUE, 2, 10 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new long[] { Long.MAX_VALUE, 2, 10 }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsLongArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new long[] { 42, 4242, 424242, 4242424242L }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new long[] { 4242424242L, 424242, 4242, 42 }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsLongArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new long[] { 12345678910L, 10, 9, 8 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new long[] { 8, 9, 10, 12345678910L }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsLongArraysOfDifferentLength() {
		try {
			assertArrayEquals(new long[] { 1, 2, 3, Long.MIN_VALUE, 4 }, new long[] { 1, Long.MAX_VALUE, 2 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <5> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsLongArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new long[] { 100_000L, 200_000L, 1L, 2L }, new long[] { 1L, 2L, 3L }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <4> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsLongArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new long[] { Long.MAX_VALUE, Long.MIN_VALUE }, new long[] { 1L, 2L, 42L },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsDifferentLongArrays() {
		try {
			assertArrayEquals(new long[] { Long.MIN_VALUE, 17, 18L, 19 }, new long[] { Long.MIN_VALUE, 17, 18, 20 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [3], expected: <19> but was: <20>");
		}
	}

	@Test
	void assertArrayEqualsDifferentLongArraysAndMessage() {
		try {
			assertArrayEquals(new long[] { 6, 5, 4, 3, 2, Long.MIN_VALUE }, new long[] { 6, 5, 4, 3, 2, 1 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex,
				"array contents differ at index [5], expected: <-9223372036854775808> but was: <1>");
		}
	}

	@Test
	void assertArrayEqualsDifferentLongArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new long[] { 42, -9999L, 2 }, new long[] { 42L, +9999L, 2L }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <-9999> but was: <9999>");
		}
	}

	@Test
	void assertArrayEqualsFloatArrays() {
		float[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new float[] {}, new float[] {});
		assertArrayEquals(new float[] {}, new float[] {}, "message");
		assertArrayEquals(new float[] {}, new float[] {}, () -> "message");
		assertArrayEquals(new float[] { Float.MAX_VALUE }, new float[] { Float.MAX_VALUE });
		assertArrayEquals(new float[] { Float.MIN_VALUE, 5F, 5.5F, 1.00F },
			new float[] { Float.MIN_VALUE, 5F, 5.5F, 1.00F });

		assertArrayEquals(new float[] { Float.NaN }, new float[] { Float.NaN });
		assertArrayEquals(new float[] { 10.18F, Float.NaN, 42.9F }, new float[] { 10.18F, Float.NaN, 42.9F });
	}

	@Test
	void assertArrayEqualsFloatArrayVsNull() {
		try {
			assertArrayEquals(null, new float[] { Float.MAX_VALUE, 4.2F, 9.0F });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new float[] { Float.MIN_VALUE, 2.3F, 10.10F }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsFloatArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new float[] { 42.42F, 42.4242F, 19.20F }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new float[] { 11.101F, 12.101F, 99.9F }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsFloatArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new float[] { 5F, 6F, 7.77F, 8.88F }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new float[] { 1F, 1.1F, 1.11F, 1.111F }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsFloatArraysOfDifferentLength() {
		try {
			assertArrayEquals(new float[] { Float.MIN_VALUE, 1F, 2F, 3F }, new float[] { Float.MAX_VALUE, 7.1F });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <4> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsFloatArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new float[] { 19.1F, 12.77F, 18.F }, new float[] { .9F, .8F, 5.123F, .10F }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <4>");
		}
	}

	@Test
	void assertArrayEqualsFloatArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new float[] { 1.1F, 1.2F, 1.3F }, new float[] { 1F, 2F, 3F, 4F, 5F, 6F },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <6>");
		}
	}

	@Test
	void assertArrayEqualsDifferentFloatArrays() {
		try {
			assertArrayEquals(new float[] { 5.5F, 6.5F, 7.5F, 8.5F }, new float[] { 5.5F, 6.5F, 7.4F, 8.5F });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [2], expected: <7.5> but was: <7.4>");
		}

		try {
			assertArrayEquals(new float[] { 1.0F, 2.0F, 3.0F, Float.NaN }, new float[] { 1.0F, 2.0F, 3.0F, 4.0F });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [3], expected: <NaN> but was: <4.0>");
		}
	}

	@Test
	void assertArrayEqualsDifferentFloatArraysAndMessage() {
		try {
			assertArrayEquals(new float[] { 1.9F, 0.5F, 0.4F, 0.3F }, new float[] { 1.9F, 0.5F, 0.4F, -0.333F },
				"message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [3], expected: <0.3> but was: <-0.333>");
		}
	}

	@Test
	void assertArrayEqualsDifferentFloatArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new float[] { 0.3F, 0.9F, 8F }, new float[] { 0.3F, Float.MIN_VALUE, 8F },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <0.9> but was: <1.4E-45>");
		}
	}

	@Test
	void assertArrayEqualsDeltaFloatArrays() {
		float[] array = {};
		assertArrayEquals(array, array, 0.001F);
		assertArrayEquals(new float[] {}, new float[] {}, 0.001F);
		assertArrayEquals(new float[] {}, new float[] {}, 0.001F, "message");
		assertArrayEquals(new float[] {}, new float[] {}, 0.001F, () -> "message");
		assertArrayEquals(new float[] { Float.MAX_VALUE }, new float[] { Float.MAX_VALUE }, 0.0001F);
		assertArrayEquals(new float[] { Float.MIN_VALUE, 2.111F, 2.521F, 1.01F },
			new float[] { Float.MIN_VALUE, 2.119F, 2.523F, 1.01001F }, 0.01F);

		assertArrayEquals(new float[] { Float.NaN }, new float[] { Float.NaN }, 0.1F);
		assertArrayEquals(new float[] { 10.18F, Float.NaN, 42.9F }, new float[] { 10.98F, Float.NaN, 43.9F }, 1F);
	}

	@Test
	void assertArrayEqualsDeltaFloatArraysThrowsForIllegalDelta() {
		try {
			assertArrayEquals(new float[] {}, new float[] {}, -0.5F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <-0.5>");
		}
		try {
			assertArrayEquals(new float[] {}, new float[] {}, Float.NaN);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <NaN>");
		}

		try {
			assertArrayEquals(new float[] { 12.9F, 7F, 13F }, new float[] { 12.9F, 7F, 13F }, -0.5F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <-0.5>");
		}
		try {
			assertArrayEquals(new float[] { 1.11F, 1.11F, 9F }, new float[] { 1.11F, 1.11F, 9F, 10F }, Float.NaN);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <NaN>");
		}
	}

	@Test
	void assertArrayEqualsDeltaFloatArrayVsNull() {
		try {
			assertArrayEquals(null, new float[] { Float.MAX_VALUE, 4.2F, 9.0F }, 0.001F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new float[] { Float.MIN_VALUE, 2.3F, 10.10F }, null, 0.01F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDeltaFloatArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new float[] { 42.42F, 42.4242F, 19.20F }, 0.0001F, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new float[] { 11.101F, 12.101F, 99.9F }, null, 0.01F, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDeltaFloatArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new float[] { 5F, 6F, 7.77F, 8.88F }, 0.1F, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new float[] { 1F, 1.1F, 1.11F, 1.111F }, null, 0.1F, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDeltaFloatArraysOfDifferentLength() {
		try {
			assertArrayEquals(new float[] { Float.MIN_VALUE, 1F, 2F, 3F }, new float[] { Float.MAX_VALUE, 7.1F }, 0.1F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <4> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsDeltaFloatArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new float[] { 19.1F, 12.77F }, new float[] { .9F, .8F, 5.123F }, 0.1F, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsDeltaFloatArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new float[] { 1.1F, 1.2F, 1.3F }, new float[] { 1F, 2F, 3F, 4F }, 0.1F, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <4>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDifferentFloatArrays() {
		try {
			assertArrayEquals(new float[] { 5.6F, 3.2F, 9.1F, 0.5F }, new float[] { 5.55F, 3.3F, 9.201F, 0.51F }, 0.1F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [2], expected: <9.1> but was: <9.201>");
		}

		try {
			assertArrayEquals(new float[] { 1.0F, 2.0F, 3.0F, Float.NaN }, new float[] { 1.5F, 1.5F, 2.9F, 4.0F },
				0.5F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [3], expected: <NaN> but was: <4.0>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDifferentFloatArraysAndMessage() {
		try {
			assertArrayEquals(new float[] { 1.91F, 0.5F, .4F, 0.3F }, new float[] { 2F, 0.509F, .499F, -0.333F }, 0.1F,
				"message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [3], expected: <0.3> but was: <-0.333>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDifferentFloatArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new float[] { 0.3F, 0.9F, 8F }, new float[] { 0.6F, Float.MIN_VALUE, 8.4F }, 0.5F,
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <0.9> but was: <1.4E-45>");
		}
	}

	@Test
	void assertArrayEqualsDoubleArrays() {
		double[] array = {};
		assertArrayEquals(array, array);
		assertArrayEquals(new double[] {}, new double[] {});
		assertArrayEquals(new double[] {}, new double[] {}, "message");
		assertArrayEquals(new double[] {}, new double[] {}, () -> "message");
		assertArrayEquals(new double[] { Double.MAX_VALUE }, new double[] { Double.MAX_VALUE });
		assertArrayEquals(new double[] { Double.MIN_VALUE, 2.1, 5.5, 1.0 },
			new double[] { Double.MIN_VALUE, 2.1, 5.5, 1.0 });

		assertArrayEquals(new double[] { Double.NaN }, new double[] { Double.NaN });
		assertArrayEquals(new double[] { 1.2, 10.8, Double.NaN, 42.9 }, new double[] { 1.2, 10.8, Double.NaN, 42.9 });
	}

	@Test
	void assertArrayEqualsDoubleArrayVsNull() {
		try {
			assertArrayEquals(null, new double[] { Double.MAX_VALUE, 17.4, 98.7654321 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new double[] { Double.MIN_VALUE, 93.0, 92.000001 }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDoubleArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new double[] { 33.3, 34.9, 20.1, 11.0011 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new double[] { 44.4, 20.19, 11.3, 0.11 }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDoubleArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new double[] { 1.2, 1.3, 1.4, 2.2002 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new double[] { 13.13, 43.33, 100 }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDoubleArraysOfDifferentLength() {
		try {
			assertArrayEquals(new double[] { Double.MIN_VALUE, 1.0, 2.0, 3.0 },
				new double[] { Double.MAX_VALUE, 1.1, 1.0 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <4> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsDoubleArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new double[] { 11.1, 99.1, 2 }, new double[] { .9, .1, .0, .1, .3 }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <5>");
		}
	}

	@Test
	void assertArrayEqualsDoubleArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new double[] { 1.15D, 2.2, 2.3 }, new double[] { 1.15D, 1.15D }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsDifferentDoubleArrays() {
		try {
			assertArrayEquals(new double[] { 1.17, 1.19, 1.21, 5 }, new double[] { 1.17, 1.00019, 1.21, 5 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [1], expected: <1.19> but was: <1.00019>");
		}

		try {
			assertArrayEquals(new double[] { 0.1, 0.2, 0.3, 0.4, 0.5 },
				new double[] { 0.1, 0.2, 0.3, 0.4, Double.NaN });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [4], expected: <0.5> but was: <NaN>");
		}
	}

	@Test
	void assertArrayEqualsDifferentDoubleArraysAndMessage() {
		try {
			assertArrayEquals(new double[] { 1.01, 9.031, .123, 4.23 }, new double[] { 1.01, 9.099, .123, 4.23 },
				"message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <9.031> but was: <9.099>");
		}
	}

	@Test
	void assertArrayEqualsDifferentDoubleArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new double[] { 0.7, .1, 8 }, new double[] { 0.7, Double.MIN_VALUE, 8 }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <0.1> but was: <4.9E-324>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDoubleArrays() {
		double[] array = {};
		assertArrayEquals(array, array, 0.5);
		assertArrayEquals(new double[] {}, new double[] {}, 0.5);
		assertArrayEquals(new double[] {}, new double[] {}, 0.5, "message");
		assertArrayEquals(new double[] {}, new double[] {}, 0.5, () -> "message");
		assertArrayEquals(new double[] { Double.MAX_VALUE, 0.1 }, new double[] { Double.MAX_VALUE, 0.2 }, 0.2);
		assertArrayEquals(new double[] { Double.MIN_VALUE, 3.1, 1.3, 2.7 },
			new double[] { Double.MIN_VALUE, 3.4, 1.7, 2.4 }, 0.5);

		assertArrayEquals(new double[] { Double.NaN }, new double[] { Double.NaN }, 0.01);
		assertArrayEquals(new double[] { 1.2, 1.8, Double.NaN, 4.9 }, new double[] { 1.25, 1.7, Double.NaN, 4.8 }, 0.2);
	}

	@Test
	void assertArrayEqualsDeltaDoubleArraysThrowsForIllegalDelta() {
		try {
			assertArrayEquals(new double[] {}, new double[] {}, -0.5F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <-0.5>");
		}
		try {
			assertArrayEquals(new double[] {}, new double[] {}, Float.NaN);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <NaN>");
		}

		try {
			assertArrayEquals(new double[] { 1.2, 1.3, 10 }, new double[] { 1.2, 1.3, 10 }, -0.5F);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <-0.5>");
		}
		try {
			assertArrayEquals(new double[] { 0.1, 10 }, new double[] { 0.1, 10, 11 }, Float.NaN);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "positive delta expected but was: <NaN>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDoubleArrayVsNull() {
		try {
			assertArrayEquals(null, new double[] { Double.MAX_VALUE, 11.1, 12.12 }, 0.5);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new double[] { Double.MIN_VALUE, 90, 91.9 }, null, 0.1);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDoubleArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new double[] { 33.3, 34.9, 20.1, 11.0011 }, 0.1, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new double[] { 44.4, 20.19, 11.3, 0.11 }, null, 0.5, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDoubleArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new double[] { 1.2, 1.3, 1.4, 2.2002 }, 1, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new double[] { 13.13, 43.33, 100 }, null, 1.5, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDoubleArraysOfDifferentLength() {
		try {
			assertArrayEquals(new double[] { Double.MIN_VALUE, 2.0, 3.0, 4.0 },
				new double[] { Double.MAX_VALUE, 2.1, 3.1 }, 0.001);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <4> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDoubleArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new double[] { 1.1, 99.1, 3.1 }, new double[] { .9, .1, .0, .1, .3 }, 0.1, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <5>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDoubleArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new double[] { 1.77D, 2.1, 3 }, new double[] { 8.8, 0.11 }, 1, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDifferentDoubleArrays() {
		try {
			assertArrayEquals(new double[] { 1.12, 2.92, 1.201 }, new double[] { 1.1201, 2.94, 1.201 }, 0.01);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [1], expected: <2.92> but was: <2.94>");
		}

		try {
			assertArrayEquals(new double[] { 0.6, 0.12, 19.9, 5.5 }, new double[] { 1.0, 0.42, 20, Double.NaN }, 0.5);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [3], expected: <5.5> but was: <NaN>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDifferentDoubleArraysAndMessage() {
		try {
			assertArrayEquals(new double[] { 1.01, 9.031, .123, 4.23 }, new double[] { 1.1, 9.231, .13, 4.3 }, 0.1,
				"message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <9.031> but was: <9.231>");
		}
	}

	@Test
	void assertArrayEqualsDeltaDifferentDoubleArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new double[] { 0.7, 0.3001, 8 }, new double[] { 0.7, 0.4002, 8 }, 0.1, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [1], expected: <0.3001> but was: <0.4002>");
		}
	}

	@Test
	void assertArrayEqualsObjectArrays() {
		Object[] array = { "a", 'b', 1, 2 };
		assertArrayEquals(array, array);

		assertArrayEquals(new Object[] {}, new Object[] {});
		assertArrayEquals(new Object[] {}, new Object[] {}, "message");
		assertArrayEquals(new Object[] {}, new Object[] {}, () -> "message");
		assertArrayEquals(new Object[] { "abc" }, new Object[] { "abc" });
		assertArrayEquals(new Object[] { "abc", 1, 2L, 3D }, new Object[] { "abc", 1, 2L, 3D });

		assertArrayEquals(new Object[] { new Object[] { new Object[] {} } },
			new Object[] { new Object[] { new Object[] {} } });

		assertArrayEquals(
			new Object[] { null, new Object[] { null, new Object[] { null, null } }, null, new Object[] { null } },
			new Object[] { null, new Object[] { null, new Object[] { null, null } }, null, new Object[] { null } });

		assertArrayEquals(new Object[] { "a", new Object[] { new Object[] { "b", new Object[] { "c", "d" } } }, "e" },
			new Object[] { "a", new Object[] { new Object[] { "b", new Object[] { "c", "d" } } }, "e" });

		assertArrayEquals(
			new Object[] { new Object[] { 1 }, new Object[] { 2 },
					new Object[] { new Object[] { 3, new Object[] { 4 } } } },
			new Object[] { new Object[] { 1 }, new Object[] { 2 },
					new Object[] { new Object[] { 3, new Object[] { 4 } } } });

		assertArrayEquals(
			new Object[] { new Object[] {
					new Object[] { new Object[] { new Object[] { new Object[] { new Object[] { "abc" } } } } } } },
			new Object[] { new Object[] {
					new Object[] { new Object[] { new Object[] { new Object[] { new Object[] { "abc" } } } } } } });

		assertArrayEquals(
			new Object[] { null, new Object[] { null, Double.NaN, new Object[] { Float.NaN, null, new Object[] {} } } },
			new Object[] { null,
					new Object[] { null, Double.NaN, new Object[] { Float.NaN, null, new Object[] {} } } });

		assertArrayEquals(
			new Object[] { new String("a"), Integer.valueOf(1), new Object[] { Double.parseDouble("1.1"), "b" } },
			new Object[] { new String("a"), Integer.valueOf(1), new Object[] { Double.parseDouble("1.1"), "b" } });

		assertArrayEquals(
			new Object[] { 1, 2,
					new Object[] { 3, new int[] { 4, 5 }, new long[] { 6 },
							new Object[] { new Object[] { new int[] { 7 } } } },
					new int[] { 8 }, new Object[] { new long[] { 9 } } },
			new Object[] { 1, 2,
					new Object[] { 3, new int[] { 4, 5 }, new long[] { 6 },
							new Object[] { new Object[] { new int[] { 7 } } } },
					new int[] { 8 }, new Object[] { new long[] { 9 } } });

		assertArrayEquals(
			new Object[] { "a", new byte[] { 42 }, new short[] { 42 }, new char[] { 'b', 'c' }, new float[] { 42.23f },
					new double[] { 42.23 }, new boolean[] { true },
					new Object[] { new Object[] { new String[] { "ef" }, new Object[] { new String[] { "ghi" } } } } },
			new Object[] { "a", new byte[] { 42 }, new short[] { 42 }, new char[] { 'b', 'c' }, new float[] { 42.23f },
					new double[] { 42.23 }, new boolean[] { true },
					new Object[] { new Object[] { new String[] { "ef" }, new Object[] { new String[] { "ghi" } } } } });
	}

	@Test
	void assertArrayEqualsObjectArrayVsNull() {
		try {
			assertArrayEquals(null, new Object[] { "a", "b", 1, new Object() });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new Object[] { 'a', 1, new Object(), 10L }, null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsNestedObjectArrayVsNull() {
		try {
			assertArrayEquals(//
				new Object[] { new Object[] {}, 1, "2", new Object[] { '3', new Object[] { null } } }, //
				new Object[] { new Object[] {}, 1, "2", new Object[] { '3', new Object[] { new Object[] { "4" } } } });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected array was <null> at index [3][1][0]");
		}

		try {
			assertArrayEquals(
				new Object[] { 1, 2, new Object[] { 3, new Object[] { "4", new Object[] { 5, new Object[] { 6 } } } },
						"7" },
				new Object[] { 1, 2, new Object[] { 3, new Object[] { "4", new Object[] { 5, null } } }, "7" });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "actual array was <null> at index [2][1][1][1]");
		}
	}

	@Test
	void assertArrayEqualsObjectArrayVsNullAndMessage() {
		try {
			assertArrayEquals(null, new Object[] { 'a', "b", 10, 20D }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new Object[] { "hello", 42 }, null, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsNestedObjectArrayVsNullAndMessage() {
		try {
			assertArrayEquals(new Object[] { 1, new Object[] { 2, 3, new Object[] { 4, 5, new Object[] { null } } } },
				new Object[] { 1, new Object[] { 2, 3, new Object[] { 4, 5, new Object[] { new Object[] { 6 } } } } },
				"message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null> at index [1][2][2][0]");
		}

		try {
			assertArrayEquals(
				new Object[] { 1, new Object[] { 2, new Object[] { 3, new Object[] { new Object[] { 4 } } } } },
				new Object[] { 1, new Object[] { 2, new Object[] { 3, new Object[] { null } } } }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null> at index [1][1][1][0]");
		}
	}

	@Test
	void assertArrayEqualsObjectArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(null, new Object[] { 42, "42", new float[] { 42F }, 42D }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null>");
		}

		try {
			assertArrayEquals(new Object[] { new Object[] { "a" }, new Object() }, null, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null>");
		}
	}

	@Test
	void assertArrayEqualsNestedObjectArrayVsNullAndMessageSupplier() {
		try {
			assertArrayEquals(new Object[] { "1", "2", "3", new Object[] { "4", new Object[] { null } } },
				new Object[] { "1", "2", "3", new Object[] { "4", new Object[] { new int[] { 5 } } } },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected array was <null> at index [3][1][0]");
		}

		try {
			assertArrayEquals(
				new Object[] { 1, 2, new Object[] { "3", new Object[] { '4', new Object[] { 5, 6, new long[] {} } } } },
				new Object[] { 1, 2, new Object[] { "3", new Object[] { '4', new Object[] { 5, 6, null } } } },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "actual array was <null> at index [2][1][1][2]");
		}
	}

	@Test
	void assertArrayEqualsObjectArraysOfDifferentLength() {
		try {
			assertArrayEquals(new Object[] { 'a', "b", 'c' }, new Object[] { 'a', "b", 'c', 1 });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ, expected: <3> but was: <4>");
		}
	}

	@Test
	void assertArrayEqualsNestedObjectArraysOfDifferentLength() {
		try {
			assertArrayEquals(
				new Object[] { "a", new Object[] { "b", new Object[] { "c", "d", new Object[] { "e", 1, 2, 3 } } } },
				new Object[] { "a",
						new Object[] { "b", new Object[] { "c", "d", new Object[] { "e", 1, 2, 3, 4, 5 } } } });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ at index [1][1][2], expected: <4> but was: <6>");
		}

		try {
			assertArrayEquals(
				new Object[] { new Object[] {
						new Object[] { new Object[] { new Object[] { new Object[] { new char[] { 'a' } } } } } } },
				new Object[] { new Object[] { new Object[] {
						new Object[] { new Object[] { new Object[] { new char[] { 'a', 'b' } } } } } } });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array lengths differ at index [0][0][0][0][0][0], expected: <1> but was: <2>");
		}
	}

	@Test
	void assertArrayEqualsObjectArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(new Object[] { 'a', 1 }, new Object[] { 'a', 1, new Object() }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <2> but was: <3>");
		}
	}

	@Test
	void assertArrayEqualsNestedObjectArraysOfDifferentLengthAndMessage() {
		try {
			assertArrayEquals(//
				new Object[] { 'a', 1, new Object[] { 2, 3 } }, //
				new Object[] { 'a', 1, new Object[] { 2, 3, 4, 5 } }, //
				"message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ at index [2], expected: <2> but was: <4>");
		}
	}

	@Test
	void assertArrayEqualsObjectArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(new Object[] { "a", "b", "c" }, new Object[] { "a", "b", "c", "d", "e", "f" },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ, expected: <3> but was: <6>");
		}
	}

	@Test
	void assertArrayEqualsNestedObjectArraysOfDifferentLengthAndMessageSupplier() {
		try {
			assertArrayEquals(//
				new Object[] { "a", new Object[] { 1, 2, 3, new double[] { 4.0, 5.1, 6.1 }, 7 } }, //
				new Object[] { "a", new Object[] { 1, 2, 3, new double[] { 4.0, 5.1, 6.1, 7.0 }, 8 } }, //
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array lengths differ at index [1][3], expected: <3> but was: <4>");
		}
	}

	@Test
	void assertArrayEqualsDifferentObjectArrays() {
		try {
			assertArrayEquals(new Object[] { 1L, "2", '3', 4, 5D }, new Object[] { 1L, "2", '9', 4, 5D });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [2], expected: <3> but was: <9>");
		}

		try {
			assertArrayEquals(new Object[] { "a", 10, 11, 12, Double.NaN }, new Object[] { "a", 10, 11, 12, 13.55D });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [4], expected: <NaN> but was: <13.55>");
		}
	}

	@Test
	void assertArrayEqualsDifferentNestedObjectArrays() {
		try {
			assertArrayEquals(
				new Object[] { 1, 2, new Object[] { 3, new Object[] { 4, new boolean[] { false, true } } } },
				new Object[] { 1, 2, new Object[] { 3, new Object[] { 4, new boolean[] { true, false } } } });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [2][1][1][0], expected: <false> but was: <true>");
		}

		try {
			assertArrayEquals(new Object[] { 1, 2, 3, new Object[] { new Object[] { 4, new Object[] { 5 } } } },
				new Object[] { 1, 2, 3, new Object[] { new Object[] { 4, new Object[] { new Object[] {} } } } });
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "array contents differ at index [3][0][1][0], expected: <5> but was: <[]>");
		}
	}

	@Test
	void assertArrayEqualsDifferentObjectArraysAndMessage() {
		try {
			assertArrayEquals(new Object[] { 1.1D, 2L, "3" }, new Object[] { 1D, 2L, "3" }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [0], expected: <1.1> but was: <1.0>");
		}
	}

	@Test
	void assertArrayEqualsDifferentNestedObjectArraysAndMessage() {
		try {
			assertArrayEquals(new Object[] { 9, 8, '6', new Object[] { 5, 4, "3", new Object[] { "2", '1' } } },
				new Object[] { 9, 8, '6', new Object[] { 5, 4, "3", new Object[] { "99", '1' } } }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageContains(ex, "array contents differ at index [3][3][0], expected: <2> but was: <99>");
		}

		try {
			assertArrayEquals(new Object[] { 9, 8, '6', new Object[] { 5, 4, "3", new String[] { "2", "1" } } },
				new Object[] { 9, 8, '6', new Object[] { 5, 4, "3", new String[] { "99", "1" } } }, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageContains(ex, "array contents differ at index [3][3][0], expected: <2> but was: <99>");
		}
	}

	@Test
	void assertArrayEqualsDifferentObjectArraysAndMessageSupplier() {
		try {
			assertArrayEquals(new Object[] { "one", 1L, Double.MIN_VALUE, "abc" },
				new Object[] { "one", 1L, 42.42, "abc" }, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [2], expected: <4.9E-324> but was: <42.42>");
		}
	}

	@Test
	void assertArrayEqualsDifferentNestedObjectArraysAndMessageSupplier() {
		try {
			assertArrayEquals(
				new Object[] { "one", 1L, new Object[] { "a", 'b', new Object[] { 1, new Object[] { 2, 3 } } }, "abc" },
				new Object[] { "one", 1L, new Object[] { "a", 'b', new Object[] { 1, new Object[] { 2, 4 } } }, "abc" },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [2][2][1][1], expected: <3> but was: <4>");
		}

		try {
			assertArrayEquals(
				new Object[] { "j", new String[] { "a" }, new int[] { 42 }, "ab", new Object[] { 1, new int[] { 3 } } },
				new Object[] { "j", new String[] { "a" }, new int[] { 42 }, "ab", new Object[] { 1, new int[] { 5 } } },
				() -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "array contents differ at index [4][1][0], expected: <3> but was: <5>");
		}
	}

}
