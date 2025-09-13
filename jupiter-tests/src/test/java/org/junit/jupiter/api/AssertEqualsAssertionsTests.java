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

import static org.junit.jupiter.api.AssertionTestUtils.assertExpectedAndActualValues;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageContains;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEndsWith;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertEqualsAssertionsTests {

	@Test
	void assertEqualsByte() {
		byte expected = 1;
		byte actual = 1;
		assertEquals(expected, actual);
		assertEquals(expected, actual, "message");
		assertEquals(expected, actual, () -> "message");
	}

	@Test
	void assertEqualsByteWithUnequalValues() {
		byte expected = 1;
		byte actual = 2;
		try {
			assertEquals(expected, actual);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

	@Test
	void assertEqualsStringWithUnequalSpaceTabs() {
		String expected = "a			c";
		String actual = "a	c";
		try {
			assertEquals(expected, actual, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageContains(ex, "expected: <a			c> but was: <a	c>");
			assertMessageContains(ex, "diff: a[\\t\\t\\t]c");
		}
	}

	@Test
	void assertEqualsByteWithUnequalValuesAndMessage() {
		byte expected = 1;
		byte actual = 2;
		try {
			assertEquals(expected, actual, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

	@Test
	void assertEqualsByteWithUnequalValuesAndMessageSupplier() {
		byte expected = 1;
		byte actual = 2;
		try {
			assertEquals(expected, actual, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

	@Test
	void assertEqualsShort() {
		short expected = 1;
		short actual = 1;
		assertEquals(expected, actual);
		assertEquals(expected, actual, "message");
		assertEquals(expected, actual, () -> "message");
	}

	@Test
	void assertEqualsShortWithUnequalValues() {
		short expected = 1;
		short actual = 2;
		try {
			assertEquals(expected, actual);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

	@Test
	void assertEqualsShortWithUnequalValuesAndMessage() {
		short expected = 1;
		short actual = 2;
		try {
			assertEquals(expected, actual, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

	@Test
	void assertEqualsShortWithUnequalValuesAndMessageSupplier() {
		short expected = 1;
		short actual = 2;
		try {
			assertEquals(expected, actual, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, expected, actual);
		}
	}

	@Test
	void assertEqualsInt() {
		assertEquals(1, 1);
		assertEquals(1, 1, "message");
		assertEquals(1, 1, () -> "message");
	}

	@Test
	void assertEqualsIntWithUnequalValues() {
		try {
			assertEquals(1, 2);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, 1, 2);
		}
	}

	@Test
	void assertEqualsIntWithUnequalValuesAndMessage() {
		try {
			assertEquals(1, 2, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, 1, 2);
		}
	}

	@Test
	void assertEqualsIntWithUnequalValuesAndMessageSupplier() {
		try {
			assertEquals(1, 2, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, 1, 2);
		}
	}

	@Test
	void assertEqualsLong() {
		assertEquals(1L, 1L);
		assertEquals(1L, 1L, "message");
		assertEquals(1L, 1L, () -> "message");
	}

	@Test
	void assertEqualsLongWithUnequalValues() {
		try {
			assertEquals(1L, 2L);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, 1L, 2L);
		}
	}

	@Test
	void assertEqualsLongWithUnequalValuesAndMessage() {
		try {
			assertEquals(1L, 2L, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, 1L, 2L);
		}
	}

	@Test
	void assertEqualsLongWithUnequalValuesAndMessageSupplier() {
		try {
			assertEquals(1L, 2L, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1> but was: <2>");
			assertExpectedAndActualValues(ex, 1L, 2L);
		}
	}

	@Test
	void assertEqualsChar() {
		assertEquals('a', 'a');
		assertEquals('a', 'a', "message");
		assertEquals('a', 'a', () -> "message");
	}

	@Test
	void assertEqualsCharWithUnequalValues() {
		try {
			assertEquals('a', 'b');
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <a> but was: <b>");
			assertExpectedAndActualValues(ex, 'a', 'b');
		}
	}

	@Test
	void assertEqualsCharWithUnequalValuesAndMessage() {
		try {
			assertEquals('a', 'b', "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <a> but was: <b>");
			assertExpectedAndActualValues(ex, 'a', 'b');
		}
	}

	@Test
	void assertEqualsCharWithUnequalValuesAndMessageSupplier() {
		try {
			assertEquals('a', 'b', () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <a> but was: <b>");
			assertExpectedAndActualValues(ex, 'a', 'b');
		}
	}

	@Test
	void assertEqualsFloat() {
		assertEquals(1.0f, 1.0f);
		assertEquals(1.0f, 1.0f, "message");
		assertEquals(1.0f, 1.0f, () -> "message");
		assertEquals(Float.NaN, Float.NaN);
		assertEquals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
		assertEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		assertEquals(Float.MIN_VALUE, Float.MIN_VALUE);
		assertEquals(Float.MAX_VALUE, Float.MAX_VALUE);
		assertEquals(Float.MIN_NORMAL, Float.MIN_NORMAL);
		assertEquals(Double.NaN, Float.NaN);
	}

	@Test
	void assertEqualsFloatWithUnequalValues() {
		try {
			assertEquals(1.0f, 1.1f);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <1.0> but was: <1.1>");
			assertExpectedAndActualValues(ex, 1.0f, 1.1f);
		}
	}

	@Test
	void assertEqualsFloatWithUnequalValuesAndMessage() {
		try {
			assertEquals(1.0f, 1.1f, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1.0> but was: <1.1>");
			assertExpectedAndActualValues(ex, 1.0f, 1.1f);
		}
	}

	@Test
	void assertEqualsFloatWithUnequalValuesAndMessageSupplier() {
		try {
			assertEquals(1.0f, 1.1f, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1.0> but was: <1.1>");
			assertExpectedAndActualValues(ex, 1.0f, 1.1f);
		}
	}

	@Test
	void assertEqualsFloatWithDelta() {
		assertEquals(0.0f, 0.0f, 0.1f);
		assertEquals(0.0f, 0.0f, 0.1f, "message");
		assertEquals(0.0f, 0.0f, 0.1f, () -> "message");
		assertEquals(0.56f, 0.6f, 0.05f);
		assertEquals(0.01f, 0.011f, 0.002f);
		assertEquals(Float.NaN, Float.NaN, 0.5f);
		assertEquals(0.1f, 0.1f, 0.0f);
	}

	@Test
	void assertEqualsFloatWithIllegalDelta() {
		AssertionFailedError e1 = assertThrows(AssertionFailedError.class, () -> assertEquals(0.1f, 0.2f, -0.9f));
		assertMessageEndsWith(e1, "positive delta expected but was: <-0.9>");

		AssertionFailedError e2 = assertThrows(AssertionFailedError.class, () -> assertEquals(.0f, .0f, -10.5f));
		assertMessageEndsWith(e2, "positive delta expected but was: <-10.5>");

		AssertionFailedError e3 = assertThrows(AssertionFailedError.class, () -> assertEquals(4.5f, 4.6f, Float.NaN));
		assertMessageEndsWith(e3, "positive delta expected but was: <NaN>");
	}

	@Test
	void assertEqualsFloatWithDeltaWithUnequalValues() {
		AssertionFailedError e1 = assertThrows(AssertionFailedError.class, () -> assertEquals(0.5f, 0.2f, 0.2f));
		assertMessageEndsWith(e1, "expected: <0.5> but was: <0.2>");

		AssertionFailedError e2 = assertThrows(AssertionFailedError.class, () -> assertEquals(0.1f, 0.2f, 0.000001f));
		assertMessageEndsWith(e2, "expected: <0.1> but was: <0.2>");

		AssertionFailedError e3 = assertThrows(AssertionFailedError.class, () -> assertEquals(100.0f, 50.0f, 10.0f));
		assertMessageEndsWith(e3, "expected: <100.0> but was: <50.0>");

		AssertionFailedError e4 = assertThrows(AssertionFailedError.class, () -> assertEquals(-3.5f, -3.3f, 0.01f));
		assertMessageEndsWith(e4, "expected: <-3.5> but was: <-3.3>");

		AssertionFailedError e5 = assertThrows(AssertionFailedError.class, () -> assertEquals(+0.0f, -0.001f, .00001f));
		assertMessageEndsWith(e5, "expected: <0.0> but was: <-0.001>");
	}

	@Test
	void assertEqualsFloatWithDeltaWithUnequalValuesAndMessage() {
		Executable assertion = () -> assertEquals(0.5f, 0.45f, 0.03f, "message");

		AssertionFailedError e = assertThrows(AssertionFailedError.class, assertion);

		assertMessageStartsWith(e, "message");
		assertMessageEndsWith(e, "expected: <0.5> but was: <0.45>");
		assertExpectedAndActualValues(e, 0.5f, 0.45f);
	}

	@Test
	void assertEqualsFloatWithDeltaWithUnequalValuesAndMessageSupplier() {
		Executable assertion = () -> assertEquals(0.5f, 0.45f, 0.03f, () -> "message");

		AssertionFailedError e = assertThrows(AssertionFailedError.class, assertion);

		assertMessageStartsWith(e, "message");
		assertMessageEndsWith(e, "expected: <0.5> but was: <0.45>");
		assertExpectedAndActualValues(e, 0.5f, 0.45f);
	}

	@Test
	void assertEqualsDouble() {
		assertEquals(1.0d, 1.0d);
		assertEquals(1.0d, 1.0d, "message");
		assertEquals(1.0d, 1.0d, () -> "message");
		assertEquals(Double.NaN, Double.NaN);
		assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
		assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		assertEquals(Double.MIN_VALUE, Double.MIN_VALUE);
		assertEquals(Double.MAX_VALUE, Double.MAX_VALUE);
		assertEquals(Double.MIN_NORMAL, Double.MIN_NORMAL);
	}

	@Test
	void assertEqualsDoubleWithUnequalValues() {
		try {
			assertEquals(1.0d, 1.1d);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <1.0> but was: <1.1>");
			assertExpectedAndActualValues(ex, 1.0d, 1.1d);
		}
	}

	@Test
	void assertEqualsDoubleWithUnequalValuesAndMessage() {
		try {
			assertEquals(1.0d, 1.1d, "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1.0> but was: <1.1>");
			assertExpectedAndActualValues(ex, 1.0d, 1.1d);
		}
	}

	@Test
	void assertEqualsDoubleWithUnequalValuesAndMessageSupplier() {
		try {
			assertEquals(1.0d, 1.1d, () -> "message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "message");
			assertMessageEndsWith(ex, "expected: <1.0> but was: <1.1>");
			assertExpectedAndActualValues(ex, 1.0d, 1.1d);
		}
	}

	@Test
	void assertEqualsDoubleWithDelta() {
		assertEquals(0.0d, 0.0d, 0.1d);
		assertEquals(0.0d, 0.0d, 0.1d, "message");
		assertEquals(0.0d, 0.0d, 0.1d, () -> "message");
		assertEquals(0.42d, 0.24d, 0.19d);
		assertEquals(0.02d, 0.011d, 0.01d);
		assertEquals(Double.NaN, Double.NaN, 0.2d);
		assertEquals(0.001d, 0.001d, 0.0d);
	}

	@Test
	void assertEqualsDoubleWithIllegalDelta() {
		AssertionFailedError e1 = assertThrows(AssertionFailedError.class, () -> assertEquals(1.1d, 1.11d, -0.5d));
		assertMessageEndsWith(e1, "positive delta expected but was: <-0.5>");

		AssertionFailedError e2 = assertThrows(AssertionFailedError.class, () -> assertEquals(.55d, .56d, -10.5d));
		assertMessageEndsWith(e2, "positive delta expected but was: <-10.5>");

		AssertionFailedError e3 = assertThrows(AssertionFailedError.class, () -> assertEquals(1.1d, 1.1d, Double.NaN));
		assertMessageEndsWith(e3, "positive delta expected but was: <NaN>");
	}

	@Test
	void assertEqualsDoubleWithDeltaWithUnequalValues() {
		AssertionFailedError e1 = assertThrows(AssertionFailedError.class, () -> assertEquals(9.9d, 9.7d, 0.1d));
		assertMessageEndsWith(e1, "expected: <9.9> but was: <9.7>");
		assertExpectedAndActualValues(e1, 9.9d, 9.7d);

		AssertionFailedError e2 = assertThrows(AssertionFailedError.class, () -> assertEquals(0.1d, 0.05d, 0.001d));
		assertMessageEndsWith(e2, "expected: <0.1> but was: <0.05>");
		assertExpectedAndActualValues(e2, 0.1d, 0.05d);

		AssertionFailedError e3 = assertThrows(AssertionFailedError.class, () -> assertEquals(17.11d, 15.11d, 1.1d));
		assertMessageEndsWith(e3, "expected: <17.11> but was: <15.11>");
		assertExpectedAndActualValues(e3, 17.11d, 15.11d);

		AssertionFailedError e4 = assertThrows(AssertionFailedError.class, () -> assertEquals(-7.2d, -5.9d, 1.1d));
		assertMessageEndsWith(e4, "expected: <-7.2> but was: <-5.9>");
		assertExpectedAndActualValues(e4, -7.2d, -5.9d);

		AssertionFailedError e5 = assertThrows(AssertionFailedError.class, () -> assertEquals(+0.0d, -0.001d, .00001d));
		assertMessageEndsWith(e5, "expected: <0.0> but was: <-0.001>");
		assertExpectedAndActualValues(e5, +0.0d, -0.001d);
	}

	@Test
	void assertEqualsDoubleWithDeltaWithUnequalValuesAndMessage() {
		Executable assertion = () -> assertEquals(42.42d, 42.4d, 0.001d, "message");

		AssertionFailedError e = assertThrows(AssertionFailedError.class, assertion);

		assertMessageStartsWith(e, "message");
		assertMessageEndsWith(e, "expected: <42.42> but was: <42.4>");
		assertExpectedAndActualValues(e, 42.42d, 42.4d);
	}

	@Test
	void assertEqualsDoubleWithDeltaWithUnequalValuesAndMessageSupplier() {
		Executable assertion = () -> assertEquals(0.9d, 10.12d, 5.001d, () -> "message");

		AssertionFailedError e = assertThrows(AssertionFailedError.class, assertion);

		assertMessageStartsWith(e, "message");
		assertMessageEndsWith(e, "expected: <0.9> but was: <10.12>");
		assertExpectedAndActualValues(e, 0.9d, 10.12d);
	}

	@Test
	void assertEqualsWithNullReferences() {
		Object null1 = null;
		Object null2 = null;

		assertEquals(null1, null);
		assertEquals(null, null2);
		assertEquals(null1, null2);
	}

	@Test
	void assertEqualsWithSameObject() {
		Object foo = new Object();
		assertEquals(foo, foo);
		assertEquals(foo, foo, "message");
		assertEquals(foo, foo, () -> "message");
	}

	@Test
	void assertEqualsWithEquivalentStrings() {
		assertEquals(new String("foo"), new String("foo"));
	}

	@Test
	void assertEqualsWithNullVsObject() {
		try {
			assertEquals(null, "foo");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <null> but was: <foo>");
			assertExpectedAndActualValues(ex, null, "foo");
		}
	}

	@Test
	void assertEqualsWithObjectVsNull() {
		try {
			assertEquals("foo", null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "expected: <foo> but was: <null>");
			assertExpectedAndActualValues(ex, "foo", null);
		}
	}

	@Test
	void assertEqualsWithObjectWithNullStringReturnedFromToStringVsNull() {
		try {
			assertEquals("null", null);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "expected: java.lang.String@");
			assertMessageEndsWith(ex, "<null> but was: <null>");
			assertExpectedAndActualValues(ex, "null", null);
		}
	}

	@Test
	void assertEqualsWithNullVsObjectWithNullStringReturnedFromToString() {
		try {
			assertEquals(null, "null");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "expected: <null> but was: java.lang.String@");
			assertMessageEndsWith(ex, "<null>");
			assertExpectedAndActualValues(ex, null, "null");
		}
	}

	@Test
	void assertEqualsWithNullVsObjectAndMessageSupplier() {
		try {
			assertEquals(null, "foo", () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "test");
			assertMessageEndsWith(ex, "expected: <null> but was: <foo>");
			assertExpectedAndActualValues(ex, null, "foo");
		}
	}

	@Test
	void assertEqualsWithObjectVsNullAndMessageSupplier() {
		try {
			assertEquals("foo", null, () -> "test");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "test");
			assertMessageEndsWith(ex, "expected: <foo> but was: <null>");
			assertExpectedAndActualValues(ex, "foo", null);
		}
	}

	@Test
	void assertEqualsInvokesEqualsMethodForIdenticalObjects() {
		Object obj = new EqualsThrowsException();
		assertThrows(NumberFormatException.class, () -> assertEquals(obj, obj));
	}

	@Test
	void assertEqualsWithUnequalObjectWhoseToStringImplementationThrowsAnException() {
		try {
			assertEquals(new ToStringThrowsException(), "foo");
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "expected: <" + ToStringThrowsException.class.getName() + "@");
			assertMessageEndsWith(ex, "but was: <foo>");
		}
	}

	// -------------------------------------------------------------------------

	@Nested
	class MixedBoxedAndUnboxedPrimitivesTests {

		@Test
		void bytes() {
			byte primitive = (byte) 42;
			Byte wrapper = Byte.valueOf("42");

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void shorts() {
			short primitive = (short) 42;
			Short wrapper = Short.valueOf("42");

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void integers() {
			int primitive = 42;
			Integer wrapper = Integer.valueOf("42");

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void longs() {
			long primitive = 42L;
			Long wrapper = Long.valueOf("42");

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void floats() {
			float primitive = 42.0f;
			Float wrapper = Float.valueOf("42.0");

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, 0.0f);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, 0.0f, "message");
			assertEquals(primitive, wrapper, () -> "message");
			assertEquals(primitive, wrapper, 0.0f, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, 0.0f);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, 0.0f, "message");
			assertEquals(wrapper, primitive, () -> "message");
			assertEquals(wrapper, primitive, 0.0f, () -> "message");
		}

		@Test
		void doubles() {
			double primitive = 42.0d;
			Double wrapper = Double.valueOf("42.0");

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, 0.0d);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, 0.0d, "message");
			assertEquals(primitive, wrapper, () -> "message");
			assertEquals(primitive, wrapper, 0.0d, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, 0.0d);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, 0.0d, "message");
			assertEquals(wrapper, primitive, () -> "message");
			assertEquals(wrapper, primitive, 0.0d, () -> "message");
		}

		@Test
		void booleans() {
			boolean primitive = true;
			Boolean wrapper = Boolean.valueOf("true");

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void chars() {
			char primitive = 'a';
			Character wrapper = Character.valueOf('a');

			assertEquals(primitive, wrapper);
			assertEquals(primitive, wrapper, "message");
			assertEquals(primitive, wrapper, () -> "message");

			assertEquals(wrapper, primitive);
			assertEquals(wrapper, primitive, "message");
			assertEquals(wrapper, primitive, () -> "message");
		}

	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("overrides")
	private static class EqualsThrowsException {

		@Override
		public boolean equals(Object obj) {
			throw new NumberFormatException();
		}
	}

	private static class ToStringThrowsException {

		@Override
		public String toString() {
			throw new NumberFormatException();
		}
	}

}
