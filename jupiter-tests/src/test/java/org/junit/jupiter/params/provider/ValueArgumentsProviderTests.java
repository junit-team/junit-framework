/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @since 5.0
 */
class ValueArgumentsProviderTests {

	@Test
	void multipleInputsAreNotAllowed() {
		assertPreconditionViolationFor(
			() -> provideArguments(new short[1], new byte[0], new int[1], new long[0], new float[0], new double[0],
				new char[0], new boolean[0], new String[0], new Class<?>[0]).findAny()).withMessageContaining(
					"Exactly one type of input must be provided in the @ValueSource annotation, but there were 2");
	}

	@Test
	void onlyEmptyInputsAreNotAllowed() {
		assertPreconditionViolationFor(
			() -> provideArguments(new short[0], new byte[0], new int[0], new long[0], new float[0], new double[0],
				new char[0], new boolean[0], new String[0], new Class<?>[0]).findAny()).withMessageContaining(
					"Exactly one type of input must be provided in the @ValueSource annotation, but there were 0");
	}

	/**
	 * @since 5.1
	 */
	@Test
	void providesShorts() {
		var arguments = provideArguments(new short[] { 23, 42 }, new byte[0], new int[0], new long[0], new float[0],
			new double[0], new char[0], new boolean[0], new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array((short) 23), array((short) 42));
	}

	/**
	 * @since 5.1
	 */
	@Test
	void providesBytes() {
		var arguments = provideArguments(new short[0], new byte[] { 23, 42 }, new int[0], new long[0], new float[0],
			new double[0], new char[0], new boolean[0], new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array((byte) 23), array((byte) 42));
	}

	@Test
	void providesInts() {
		var arguments = provideArguments(new short[0], new byte[0], new int[] { 23, 42 }, new long[0], new float[0],
			new double[0], new char[0], new boolean[0], new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array(23), array(42));
	}

	@Test
	void providesLongs() {
		var arguments = provideArguments(new short[0], new byte[0], new int[0], new long[] { 23, 42 }, new float[0],
			new double[0], new char[0], new boolean[0], new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array(23L), array(42L));
	}

	/**
	 * @since 5.1
	 */
	@Test
	void providesFloats() {
		var arguments = provideArguments(new short[0], new byte[0], new int[0], new long[0],
			new float[] { 23.32F, 42.24F }, new double[0], new char[0], new boolean[0], new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array(23.32F), array(42.24F));
	}

	@Test
	void providesDoubles() {
		var arguments = provideArguments(new short[0], new byte[0], new int[0], new long[0], new float[0],
			new double[] { 23.32, 42.24 }, new char[0], new boolean[0], new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array(23.32), array(42.24));
	}

	/**
	 * @since 5.1
	 */
	@Test
	void providesChars() {
		var arguments = provideArguments(new short[0], new byte[0], new int[0], new long[0], new float[0],
			new double[0], new char[] { 'a', 'b', 'c' }, new boolean[0], new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array('a'), array('b'), array('c'));
	}

	/**
	 * @since 5.5
	 */
	@Test
	void providesBooleans() {
		var arguments = provideArguments(new short[0], new byte[0], new int[0], new long[0], new float[0],
			new double[0], new char[0], new boolean[] { true, false }, new String[0], new Class<?>[0]);

		assertThat(arguments).containsExactly(array(true), array(false));
	}

	@Test
	void providesStrings() {
		var arguments = provideArguments(new short[0], new byte[0], new int[0], new long[0], new float[0],
			new double[0], new char[0], new boolean[0], new String[] { "foo", "bar" }, new Class<?>[0]);

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	/**
	 * @since 5.1
	 */
	@Test
	void providesClasses() {
		var arguments = provideArguments(new short[0], new byte[0], new int[0], new long[0], new float[0],
			new double[0], new char[0], new boolean[0], new String[0], new Class<?>[] { Integer.class, getClass() });

		assertThat(arguments).containsExactly(array(Integer.class), array(getClass()));
	}

	private static Stream<Object[]> provideArguments(short[] shorts, byte[] bytes, int[] ints, long[] longs,
			float[] floats, double[] doubles, char[] chars, boolean[] booleans, String[] strings, Class<?>[] classes) {

		var annotation = mock(ValueSource.class);
		when(annotation.shorts()).thenReturn(shorts);
		when(annotation.bytes()).thenReturn(bytes);
		when(annotation.ints()).thenReturn(ints);
		when(annotation.longs()).thenReturn(longs);
		when(annotation.floats()).thenReturn(floats);
		when(annotation.doubles()).thenReturn(doubles);
		when(annotation.chars()).thenReturn(chars);
		when(annotation.booleans()).thenReturn(booleans);
		when(annotation.strings()).thenReturn(strings);
		when(annotation.classes()).thenReturn(classes);

		var provider = new ValueArgumentsProvider();
		provider.accept(annotation);
		return provider.provideArguments(mock(), mock(ExtensionContext.class)).map(Arguments::get);
	}

	private static Object[] array(Object... objects) {
		return objects;
	}

}
