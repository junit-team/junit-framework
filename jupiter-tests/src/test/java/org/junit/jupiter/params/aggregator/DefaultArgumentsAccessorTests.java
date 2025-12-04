/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultArgumentsAccessor}.
 *
 * @since 5.2
 */
class DefaultArgumentsAccessorTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void argumentsMustNotBeNull() {
		assertPreconditionViolationFor(() -> defaultArgumentsAccessor(1, (Object[]) null));
	}

	@Test
	void indexMustNotBeNegative() {
		ArgumentsAccessor arguments = defaultArgumentsAccessor(1, 1, 2);
		assertPreconditionViolationFor(() -> arguments.get(-1)).withMessage("index must be >= 0 and < 2");
	}

	@Test
	void indexMustBeSmallerThanLength() {
		ArgumentsAccessor arguments = defaultArgumentsAccessor(1, 1, 2);
		assertPreconditionViolationFor(() -> arguments.get(2)).withMessage("index must be >= 0 and < 2");
	}

	@Test
	void getNull() {
		assertNull(defaultArgumentsAccessor(1, new @Nullable Object[] { null }).get(0));
	}

	@Test
	void getWithNullCastToWrapperType() {
		assertNull(defaultArgumentsAccessor(1, (Object[]) new @Nullable Integer[] { null }).get(0, Integer.class));
	}

	@Test
	void get() {
		assertEquals(1, defaultArgumentsAccessor(1, 1).get(0));
	}

	@Test
	void getWithCast() {
		assertEquals(Integer.valueOf(1), defaultArgumentsAccessor(1, 1).get(0, Integer.class));
		assertEquals(Character.valueOf('A'), defaultArgumentsAccessor(1, 'A').get(0, Character.class));
	}

	@Test
	void getWithCastToPrimitiveType() {
		Exception exception = assertThrows(ArgumentAccessException.class,
			() -> defaultArgumentsAccessor(1, 1).get(0, int.class));
		assertThat(exception.getMessage()).isEqualTo(
			"Argument at index [0] with value [1] and type [java.lang.Integer] could not be converted or cast to type [int].");

		exception = assertThrows(ArgumentAccessException.class,
			() -> defaultArgumentsAccessor(1, new @Nullable Object[] { null }).get(0, int.class));
		assertThat(exception.getMessage()).isEqualTo(
			"Argument at index [0] with value [null] and type [null] could not be converted or cast to type [int].");
	}

	@Test
	void getWithCastToIncompatibleType() {
		Exception exception = assertThrows(ArgumentAccessException.class,
			() -> defaultArgumentsAccessor(1, 1).get(0, Character.class));
		assertThat(exception.getMessage()).isEqualTo(
			"Argument at index [0] with value [1] and type [java.lang.Integer] could not be converted or cast to type [java.lang.Character].");
	}

	@Test
	void getCharacter() {
		assertEquals(Character.valueOf('A'), defaultArgumentsAccessor(1, 'A', 'B').getCharacter(0));
	}

	@Test
	void getBoolean() {
		assertEquals(Boolean.TRUE, defaultArgumentsAccessor(1, true, false).getBoolean(0));
	}

	@Test
	void getByte() {
		assertEquals(Byte.valueOf((byte) 42), defaultArgumentsAccessor(1, (byte) 42).getByte(0));
	}

	@Test
	void getShort() {
		assertEquals(Short.valueOf((short) 42), defaultArgumentsAccessor(1, (short) 42).getShort(0));
	}

	@Test
	void getInteger() {
		assertEquals(Integer.valueOf(42), defaultArgumentsAccessor(1, 42).getInteger(0));
	}

	@Test
	void getLong() {
		assertEquals(Long.valueOf(42L), defaultArgumentsAccessor(1, 42L).getLong(0));
	}

	@Test
	void getFloat() {
		assertEquals(Float.valueOf(42.0f), defaultArgumentsAccessor(1, 42.0f).getFloat(0));
	}

	@Test
	void getDouble() {
		assertEquals(Double.valueOf(42.0), defaultArgumentsAccessor(1, 42.0).getDouble(0));
	}

	@Test
	void getString() {
		assertEquals("foo", defaultArgumentsAccessor(1, "foo", "bar").getString(0));
	}

	@Test
	void toArray() {
		var arguments = defaultArgumentsAccessor(1, "foo", "bar");
		var copy = arguments.toArray();
		assertArrayEquals(new String[] { "foo", "bar" }, copy);

		// Modify local copy:
		copy[0] = "Boom!";
		assertEquals("foo", arguments.toArray()[0]);
	}

	@Test
	void toList() {
		var arguments = defaultArgumentsAccessor(1, "foo", "bar");
		var copy = arguments.toList();
		assertIterableEquals(Arrays.asList("foo", "bar"), copy);

		// Modify local copy:
		assertThrows(UnsupportedOperationException.class, () -> copy.set(0, "Boom!"));
	}

	@Test
	void size() {
		assertEquals(0, defaultArgumentsAccessor(1).size());
		assertEquals(1, defaultArgumentsAccessor(1, 42).size());
		assertEquals(5, defaultArgumentsAccessor(1, 'a', 'b', 'c', 'd', 'e').size());
	}

	private static DefaultArgumentsAccessor defaultArgumentsAccessor(int invocationIndex,
			@Nullable Object... arguments) {
		var classLoader = DefaultArgumentsAccessorTests.class.getClassLoader();
		return DefaultArgumentsAccessor.create(invocationIndex, classLoader, arguments);
	}

	@SuppressWarnings("unused")
	private static void foo() {
	}

}
