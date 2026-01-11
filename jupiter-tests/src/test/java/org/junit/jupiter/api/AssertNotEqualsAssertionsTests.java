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

import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEndsWith;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertNotEqualsAssertionsTests {

	@Nested
	class AssertNotEqualsByte {

		@Test
		void assertNotEqualsByte() {
			byte unexpected = 1;
			byte actual = 2;
			assertNotEquals(unexpected, actual);
			assertNotEquals(unexpected, actual, "message");
			assertNotEquals(unexpected, actual, () -> "message");
		}

		@Test
		void withEqualValues() {
			byte unexpected = 1;
			byte actual = 1;
			try {
				assertNotEquals(unexpected, actual);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			byte unexpected = 1;
			byte actual = 1;
			try {
				assertNotEquals(unexpected, actual, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			byte unexpected = 1;
			byte actual = 1;
			try {
				assertNotEquals(unexpected, actual, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

	}

	@Nested
	class AssertNotEqualsShort {

		@Test
		void assertNotEqualsShort() {
			short unexpected = 1;
			short actual = 2;
			assertNotEquals(unexpected, actual);
			assertNotEquals(unexpected, actual, "message");
			assertNotEquals(unexpected, actual, () -> "message");
		}

		@Test
		void withEqualValues() {
			short unexpected = 1;
			short actual = 1;
			try {
				assertNotEquals(unexpected, actual);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			short unexpected = 1;
			short actual = 1;
			try {
				assertNotEquals(unexpected, actual, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			short unexpected = 1;
			short actual = 1;
			try {
				assertNotEquals(unexpected, actual, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

	}

	@Nested
	class AssertNotEqualsChar {

		@Test
		void assertNotEqualsChar() {
			char unexpected = 'a';
			char actual = 'b';
			assertNotEquals(unexpected, actual);
			assertNotEquals(unexpected, actual, "message");
			assertNotEquals(unexpected, actual, () -> "message");
		}

		@Test
		void withEqualValues() {
			char unexpected = 'a';
			char actual = 'a';
			try {
				assertNotEquals(unexpected, actual);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <a>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			char unexpected = 'a';
			char actual = 'a';
			try {
				assertNotEquals(unexpected, actual, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <a>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			char unexpected = 'a';
			char actual = 'a';
			try {
				assertNotEquals(unexpected, actual, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <a>");
			}
		}

	}

	@Nested
	class AssertNotEqualsInt {

		@Test
		void assertNotEqualsInt() {
			int unexpected = 1;
			int actual = 2;
			assertNotEquals(unexpected, actual);
			assertNotEquals(unexpected, actual, "message");
			assertNotEquals(unexpected, actual, () -> "message");
		}

		@Test
		void withEqualValues() {
			int unexpected = 1;
			int actual = 1;
			try {
				assertNotEquals(unexpected, actual);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			int unexpected = 1;
			int actual = 1;
			try {
				assertNotEquals(unexpected, actual, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			int unexpected = 1;
			int actual = 1;
			try {
				assertNotEquals(unexpected, actual, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

	}

	@Nested
	class AssertNotEqualsLong {

		@Test
		void assertNotEqualsLong() {
			long unexpected = 1L;
			long actual = 2L;
			assertNotEquals(unexpected, actual);
			assertNotEquals(unexpected, actual, "message");
			assertNotEquals(unexpected, actual, () -> "message");
		}

		@Test
		void withEqualValues() {
			long unexpected = 1L;
			long actual = 1L;
			try {
				assertNotEquals(unexpected, actual);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			long unexpected = 1L;
			long actual = 1L;
			try {
				assertNotEquals(unexpected, actual, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			long unexpected = 1L;
			long actual = 1L;
			try {
				assertNotEquals(unexpected, actual, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1>");
			}
		}

	}

	@Nested
	class AssertNotEqualsFloatWithoutDelta {

		@Test
		void assertNotEqualsFloat() {
			float unexpected = 1.0f;
			float actual = 2.0f;
			assertNotEquals(unexpected, actual);
			assertNotEquals(unexpected, actual, "message");
			assertNotEquals(unexpected, actual, () -> "message");
		}

		@Test
		void assertNotEqualsForTwoNaNFloat() {
			try {
				assertNotEquals(Float.NaN, Float.NaN);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <NaN>");
			}
		}

		@Test
		void assertNotEqualsForPositiveInfinityFloat() {
			try {
				assertNotEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <Infinity>");
			}
		}

		@Test
		void assertNotEqualsForNegativeInfinityFloat() {
			try {
				assertNotEquals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <-Infinity>");
			}
		}

		@Test
		void withEqualValues() {
			float unexpected = 1.0f;
			float actual = 1.0f;
			try {
				assertNotEquals(unexpected, actual);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1.0>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			float unexpected = 1.0f;
			float actual = 1.0f;
			try {
				assertNotEquals(unexpected, actual, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.0>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			float unexpected = 1.0f;
			float actual = 1.0f;
			try {
				assertNotEquals(unexpected, actual, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.0>");
			}
		}

	}

	@Nested
	class AssertNotEqualsFloatWithDelta {

		@Test
		void assertNotEqualsFloat() {
			assertNotEquals(1.0f, 1.5f, 0.4f);
			assertNotEquals(1.0f, 1.5f, 0.4f, "message");
			assertNotEquals(1.0f, 1.5f, 0.4f, () -> "message");
		}

		@Test
		void withEqualValues() {
			float unexpected = 1.0f;
			float actual = 1.5f;
			float delta = 0.5f;
			try {
				assertNotEquals(unexpected, actual, delta);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1.5>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			float unexpected = 1.0f;
			float actual = 1.5f;
			float delta = 0.5f;
			try {
				assertNotEquals(unexpected, actual, delta, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.5>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			float unexpected = 1.0f;
			float actual = 1.5f;
			float delta = 0.5f;
			try {
				assertNotEquals(unexpected, actual, delta, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.5>");
			}
		}

	}

	@Nested
	class AssertNotEqualsDoubleWithoutDelta {

		@Test
		void assertNotEqualsDouble() {
			double unexpected = 1.0d;
			double actual = 2.0d;
			assertNotEquals(unexpected, actual);
			assertNotEquals(unexpected, actual, "message");
			assertNotEquals(unexpected, actual, () -> "message");
		}

		@Test
		void assertNotEqualsForTwoNaNDouble() {
			try {
				assertNotEquals(Double.NaN, Double.NaN);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <NaN>");
			}
		}

		@Test
		void withEqualValues() {
			double unexpected = 1.0d;
			double actual = 1.0d;
			try {
				assertNotEquals(unexpected, actual);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1.0>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			double unexpected = 1.0d;
			double actual = 1.0d;
			try {
				assertNotEquals(unexpected, actual, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.0>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			double unexpected = 1.0d;
			double actual = 1.0d;
			try {
				assertNotEquals(unexpected, actual, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.0>");
			}
		}

	}

	@Nested
	class AssertNotEqualsDoubleWithDelta {

		@Test
		void assertNotEqualsDouble() {
			assertNotEquals(1.0d, 1.5d, 0.4d);
			assertNotEquals(1.0d, 1.5d, 0.4d, "message");
			assertNotEquals(1.0d, 1.5d, 0.4d, () -> "message");
		}

		@Test
		void withEqualValues() {
			double unexpected = 1.0d;
			double actual = 1.5d;
			double delta = 0.5d;
			try {
				assertNotEquals(unexpected, actual, delta);
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageEquals(ex, "expected: not equal but was: <1.5>");
			}
		}

		@Test
		void withEqualValuesWithMessage() {
			double unexpected = 1.0d;
			double actual = 1.5d;
			double delta = 0.5d;
			try {
				assertNotEquals(unexpected, actual, delta, "custom message");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.5>");
			}
		}

		@Test
		void withEqualValuesWithMessageSupplier() {
			double unexpected = 1.0d;
			double actual = 1.5d;
			double delta = 0.5d;
			try {
				assertNotEquals(unexpected, actual, delta, () -> "custom message from supplier");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "custom message from supplier");
				assertMessageEndsWith(ex, "expected: not equal but was: <1.5>");
			}
		}

	}

	@Nested
	class AssertNotEqualsObject {

		@Test
		void assertNotEqualsWithNullVsObject() {
			assertNotEquals(null, "foo");
		}

		@Test
		void assertNotEqualsWithObjectVsNull() {
			assertNotEquals("foo", null);
		}

		@Test
		void assertNotEqualsWithDifferentObjects() {
			assertNotEquals(new Object(), new Object());
			assertNotEquals(new Object(), new Object(), "message");
			assertNotEquals(new Object(), new Object(), () -> "message");
		}

		@Test
		void assertNotEqualsWithNullVsObjectAndMessageSupplier() {
			assertNotEquals(null, "foo", () -> "test");
		}

		@Test
		void assertNotEqualsWithEquivalentStringsAndMessage() {
			try {
				assertNotEquals(new String("foo"), new String("foo"), "test");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "test");
				assertMessageEndsWith(ex, "expected: not equal but was: <foo>");
			}
		}

		@Test
		void assertNotEqualsWithEquivalentStringsAndMessageSupplier() {
			try {
				assertNotEquals(new String("foo"), new String("foo"), () -> "test");
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				assertMessageStartsWith(ex, "test");
				assertMessageEndsWith(ex, "expected: not equal but was: <foo>");
			}
		}

		@Test
		void assertNotEqualsInvokesEqualsMethodForIdenticalObjects() {
			Object obj = new EqualsThrowsExceptionClass();
			assertThrows(NumberFormatException.class, () -> assertNotEquals(obj, obj));
		}

	}

	// -------------------------------------------------------------------------

	@Nested
	class MixedBoxedAndUnboxedPrimitivesTests {

		@Test
		void bytes() {
			byte primitive = (byte) 42;
			Byte wrapper = Byte.valueOf("99");

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void shorts() {
			short primitive = (short) 42;
			Short wrapper = Short.valueOf("99");

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void integers() {
			int primitive = 42;
			Integer wrapper = Integer.valueOf("99");

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void longs() {
			long primitive = 42L;
			Long wrapper = Long.valueOf("99");

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void floats() {
			float primitive = 42.0f;
			Float wrapper = Float.valueOf("99.0");

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, 0.0f);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, 0.0f, "message");
			assertNotEquals(primitive, wrapper, () -> "message");
			assertNotEquals(primitive, wrapper, 0.0f, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, 0.0f);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, 0.0f, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
			assertNotEquals(wrapper, primitive, 0.0f, () -> "message");
		}

		@Test
		void doubles() {
			double primitive = 42.0d;
			Double wrapper = Double.valueOf("99.0");

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, 0.0d);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, 0.0d, "message");
			assertNotEquals(primitive, wrapper, () -> "message");
			assertNotEquals(primitive, wrapper, 0.0d, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, 0.0d);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, 0.0d, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
			assertNotEquals(wrapper, primitive, 0.0d, () -> "message");
		}

		@Test
		void booleans() {
			boolean primitive = true;
			Boolean wrapper = Boolean.valueOf("false");

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
		}

		@Test
		void chars() {
			char primitive = 'a';
			Character wrapper = Character.valueOf('z');

			assertNotEquals(primitive, wrapper);
			assertNotEquals(primitive, wrapper, "message");
			assertNotEquals(primitive, wrapper, () -> "message");

			assertNotEquals(wrapper, primitive);
			assertNotEquals(wrapper, primitive, "message");
			assertNotEquals(wrapper, primitive, () -> "message");
		}

	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("overrides")
	private static class EqualsThrowsExceptionClass {

		@Override
		public boolean equals(Object obj) {
			throw new NumberFormatException();
		}
	}

}
