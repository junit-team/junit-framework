/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.platform.commons.util.ClassLoaderUtils.getClassLoader;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.support.conversion.ConversionContext;
import org.junit.platform.commons.support.conversion.ConversionException;
import org.junit.platform.commons.support.conversion.TypeDescriptor;
import org.junit.platform.commons.test.TestClassLoader;

/**
 * Unit tests for {@link DefaultArgumentConverter}.
 *
 * @since 5.0
 */
class DefaultArgumentConverterTests {

	private final DefaultArgumentConverter underTest = spy(DefaultArgumentConverter.INSTANCE);

	@Test
	void isAwareOfNull() {
		assertConverts(null, Object.class, null);
		assertConverts(null, String.class, null);
		assertConverts(null, Boolean.class, null);
	}

	@Test
	void isAwareOfWrapperTypesForPrimitiveTypes() {
		assertConverts(true, boolean.class, true);
		assertConverts(false, boolean.class, false);
		assertConverts((byte) 1, byte.class, (byte) 1);
		assertConverts('o', char.class, 'o');
		assertConverts((short) 1, short.class, (short) 1);
		assertConverts(1, int.class, 1);
		assertConverts(1L, long.class, 1L);
		assertConverts(1.0f, float.class, 1.0f);
		assertConverts(1.0d, double.class, 1.0d);
	}

	@Test
	void isAwareOfWideningConversions() {
		assertConverts((byte) 1, short.class, (byte) 1);
		assertConverts((short) 1, int.class, (short) 1);
		assertConverts((char) 1, int.class, (char) 1);
		assertConverts((byte) 1, long.class, (byte) 1);
		assertConverts(1, long.class, 1);
		assertConverts((char) 1, float.class, (char) 1);
		assertConverts(1, float.class, 1);
		assertConverts(1L, double.class, 1L);
		assertConverts(1.0f, double.class, 1.0f);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(classes = { char.class, boolean.class, short.class, byte.class, int.class, long.class, float.class,
			double.class, void.class })
	void throwsExceptionForNullToPrimitiveTypeConversion(Class<?> type) {
		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert(null, type)) //
				.withMessage("Cannot convert null to primitive value of type " + type.getCanonicalName());

		verify(underTest, never()).delegateConversion(any(), any());
	}

	@Test
	void delegatesStringsConversion() {
		doReturn(null).when(underTest).delegateConversion(any(), any());

		convert("value", int.class);

		var context = new ConversionContext(TypeDescriptor.forClass(String.class), TypeDescriptor.forClass(int.class),
			getClassLoader(getClass()));
		verify(underTest).delegateConversion("value", context);
	}

	@Test
	void throwsExceptionForDelegatedConversionFailure() {
		ConversionException exception = new ConversionException("fail");
		doThrow(exception).when(underTest).delegateConversion(any(), any());

		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert("value", int.class)) //
				.withCause(exception) //
				.withMessage(exception.getMessage());

		var context = new ConversionContext(TypeDescriptor.forClass(String.class), TypeDescriptor.forClass(int.class),
			getClassLoader(getClass()));
		verify(underTest).delegateConversion("value", context);
	}

	@Test
	void delegatesStringToClassWithCustomTypeFromDifferentClassLoaderConversion() throws Exception {
		String customTypeName = Enigma.class.getName();
		try (var testClassLoader = TestClassLoader.forClasses(Enigma.class)) {
			var customType = testClassLoader.loadClass(customTypeName);
			assertThat(customType.getClassLoader()).isSameAs(testClassLoader);

			var declaringExecutable = ReflectionSupport.findMethod(customType, "foo").orElseThrow();
			assertThat(declaringExecutable.getDeclaringClass().getClassLoader()).isSameAs(testClassLoader);

			doReturn(customType).when(underTest).delegateConversion(any(), any());

			var clazz = (Class<?>) convert(customTypeName, Class.class, testClassLoader);
			assertThat(clazz).isNotEqualTo(Enigma.class);
			assertThat(clazz).isNotNull().isEqualTo(customType);
			assertThat(clazz.getClassLoader()).isSameAs(testClassLoader);

			var context = new ConversionContext(TypeDescriptor.forClass(String.class),
				TypeDescriptor.forClass(Class.class), testClassLoader);
			verify(underTest).delegateConversion(customTypeName, context);
		}
	}

	// -------------------------------------------------------------------------

	private void assertConverts(@Nullable Object input, Class<?> targetClass, @Nullable Object expectedOutput) {
		var result = convert(input, targetClass);

		assertThat(result) //
				.describedAs(input + " --(" + targetClass.getName() + ")--> " + expectedOutput) //
				.isEqualTo(expectedOutput);

		verify(underTest, never()).delegateConversion(any(), any());
	}

	private @Nullable Object convert(@Nullable Object input, Class<?> targetClass) {
		return convert(input, targetClass, getClassLoader(getClass()));
	}

	private @Nullable Object convert(@Nullable Object input, Class<?> targetClass, ClassLoader classLoader) {
		return underTest.convert(input, TypeDescriptor.forClass(targetClass), classLoader);
	}

	@SuppressWarnings("unused")
	private static void foo() {
	}

	private static class Enigma {

		@SuppressWarnings("unused")
		void foo() {
		}
	}

}
