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

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.AssertionUtils.formatIndexes;
import static org.junit.platform.commons.util.ReflectionUtils.isArray;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.opentest4j.AssertionFailedError;

/**
 * {@code AssertArrayEquals} is a collection of utility methods that support asserting
 * array equality in tests.
 *
 * @since 5.0
 */
class AssertArrayEquals {

	private AssertArrayEquals() {
		/* no-op */
	}

	static void assertArrayEquals(boolean @Nullable [] expected, boolean @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(boolean @Nullable [] expected, boolean @Nullable [] actual,
			@Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(boolean @Nullable [] expected, boolean @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(char @Nullable [] expected, char @Nullable [] actual, @Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(char @Nullable [] expected, char @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(char @Nullable [] expected, char @Nullable [] actual,
			@Nullable Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(byte @Nullable [] expected, byte @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(byte @Nullable [] expected, byte @Nullable [] actual, @Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(byte @Nullable [] expected, byte @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(short @Nullable [] expected, short @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(short @Nullable [] expected, short @Nullable [] actual, @Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(short @Nullable [] expected, short @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(int @Nullable [] expected, int @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(int @Nullable [] expected, int @Nullable [] actual, @Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(int @Nullable [] expected, int @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(long @Nullable [] expected, long @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(long @Nullable [] expected, long @Nullable [] actual, @Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(long @Nullable [] expected, long @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual, @Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual, float delta) {
		assertArrayEquals(expected, actual, delta, (String) null);
	}

	static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual, float delta,
			@Nullable String message) {
		assertArrayEquals(expected, actual, delta, null, message);
	}

	static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual, float delta,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, delta, null, messageSupplier);
	}

	static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual, @Nullable String message) {
		assertArrayEquals(expected, actual, null, message);
	}

	static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, null, messageSupplier);
	}

	static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual, double delta) {
		assertArrayEquals(expected, actual, delta, (String) null);
	}

	static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual, double delta,
			@Nullable String message) {
		assertArrayEquals(expected, actual, delta, null, message);
	}

	static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual, double delta,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, delta, null, messageSupplier);
	}

	static void assertArrayEquals(@Nullable Object @Nullable [] expected, @Nullable Object @Nullable [] actual) {
		assertArrayEquals(expected, actual, (String) null);
	}

	static void assertArrayEquals(@Nullable Object @Nullable [] expected, @Nullable Object @Nullable [] actual,
			@Nullable String message) {
		assertArrayEquals(expected, actual, new ArrayDeque<>(), message);
	}

	static void assertArrayEquals(@Nullable Object @Nullable [] expected, @Nullable Object @Nullable [] actual,
			Supplier<@Nullable String> messageSupplier) {
		assertArrayEquals(expected, actual, new ArrayDeque<>(), messageSupplier);
	}

	private static void assertArrayEquals(boolean @Nullable [] expected, boolean @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}
		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(char @Nullable [] expected, char @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(byte @Nullable [] expected, byte @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(short @Nullable [] expected, short @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(int @Nullable [] expected, int @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(long @Nullable [] expected, long @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (!AssertionUtils.floatsAreEqual(expected[i], actual[i])) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(float @Nullable [] expected, float @Nullable [] actual, float delta,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		AssertionUtils.assertValidDelta(delta);
		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (!AssertionUtils.floatsAreEqual(expected[i], actual[i], delta)) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (!AssertionUtils.doublesAreEqual(expected[i], actual[i])) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(double @Nullable [] expected, double @Nullable [] actual, double delta,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		AssertionUtils.assertValidDelta(delta);
		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			if (!AssertionUtils.doublesAreEqual(expected[i], actual[i], delta)) {
				failArraysNotEqual(expected[i], actual[i], nullSafeIndexes(indexes, i), messageOrSupplier);
			}
		}
	}

	private static void assertArrayEquals(@Nullable Object @Nullable [] expected, @Nullable Object @Nullable [] actual,
			Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected == actual) {
			return;
		}

		if (expected == null) {
			throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
		}
		if (actual == null) {
			throw actualArrayIsNullFailure(indexes, messageOrSupplier);
		}
		assertArraysHaveSameLength(expected.length, actual.length, indexes, messageOrSupplier);

		for (int i = 0; i < expected.length; i++) {
			Object expectedElement = expected[i];
			Object actualElement = actual[i];

			if (expectedElement == actualElement) {
				continue;
			}

			indexes.addLast(i);
			assertArrayElementsEqual(expectedElement, actualElement, indexes, messageOrSupplier);
			indexes.removeLast();
		}
	}

	private static void assertArrayElementsEqual(@Nullable Object expected, @Nullable Object actual,
			Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		if (expected instanceof Object[] expectedArray && actual instanceof Object[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof byte[] expectedArray && actual instanceof byte[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof short[] expectedArray && actual instanceof short[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof int[] expectedArray && actual instanceof int[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof long[] expectedArray && actual instanceof long[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof char[] expectedArray && actual instanceof char[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof float[] expectedArray && actual instanceof float[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof double[] expectedArray && actual instanceof double[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (expected instanceof boolean[] expectedArray && actual instanceof boolean[] actualArray) {
			assertArrayEquals(expectedArray, actualArray, indexes, messageOrSupplier);
		}
		else if (!Objects.equals(expected, actual)) {
			if (expected == null && isArray(actual)) {
				failExpectedArrayIsNull(indexes, messageOrSupplier);
			}
			else if (isArray(expected) && actual == null) {
				failActualArrayIsNull(indexes, messageOrSupplier);
			}
			else {
				failArraysNotEqual(expected, actual, indexes, messageOrSupplier);
			}
		}
	}

	private static void failExpectedArrayIsNull(@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {
		throw expectedArrayIsNullFailure(indexes, messageOrSupplier);
	}

	private static AssertionFailedError expectedArrayIsNullFailure(@Nullable Deque<Integer> indexes,
			@Nullable Object messageOrSupplier) {
		return assertionFailure() //
				.message(messageOrSupplier) //
				.reason("expected array was <null>" + formatIndexes(indexes)) //
				.trimStacktrace(Assertions.class) //
				.build();
	}

	private static void failActualArrayIsNull(@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {
		throw actualArrayIsNullFailure(indexes, messageOrSupplier);
	}

	private static AssertionFailedError actualArrayIsNullFailure(@Nullable Deque<Integer> indexes,
			@Nullable Object messageOrSupplier) {
		return assertionFailure() //
				.message(messageOrSupplier) //
				.reason("actual array was <null>" + formatIndexes(indexes)) //
				.trimStacktrace(Assertions.class) //
				.build();
	}

	private static void assertArraysHaveSameLength(int expected, int actual, @Nullable Deque<Integer> indexes,
			@Nullable Object messageOrSupplier) {

		if (expected != actual) {
			assertionFailure() //
					.message(messageOrSupplier) //
					.reason("array lengths differ" + formatIndexes(indexes)) //
					.expected(expected) //
					.actual(actual) //
					.trimStacktrace(Assertions.class) //
					.buildAndThrow();
		}
	}

	private static void failArraysNotEqual(@Nullable Object expected, @Nullable Object actual,
			@Nullable Deque<Integer> indexes, @Nullable Object messageOrSupplier) {

		assertionFailure() //
				.message(messageOrSupplier) //
				.reason("array contents differ" + formatIndexes(indexes)) //
				.expected(expected) //
				.actual(actual) //
				.trimStacktrace(Assertions.class) //
				.buildAndThrow();
	}

	private static Deque<Integer> nullSafeIndexes(@Nullable Deque<Integer> indexes, int newIndex) {
		Deque<Integer> result = (indexes != null ? indexes : new ArrayDeque<>());
		result.addLast(newIndex);
		return result;
	}

}
