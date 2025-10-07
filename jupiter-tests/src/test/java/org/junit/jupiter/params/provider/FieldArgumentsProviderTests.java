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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.engine.extension.MutableExtensionRegistry.createRegistryWithDefaultExtensions;
import static org.junit.platform.commons.support.ReflectionSupport.findMethod;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.DefaultExecutableInvoker;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.test.TestClassLoader;

/**
 * Unit tests for {@link FieldArgumentsProvider}.
 *
 * @since 5.11
 */
class FieldArgumentsProviderTests {

	@Test
	void providesArgumentsUsingStreamSupplier() {
		var arguments = provideArguments("stringStreamSupplier");

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void providesArgumentsUsingIntStreamSupplier() {
		var arguments = provideArguments("intStreamSupplier");

		assertThat(arguments).containsExactly(array(1), array(2));
	}

	@Test
	void providesArgumentsUsingLongStreamSupplier() {
		var arguments = provideArguments("longStreamSupplier");

		assertThat(arguments).containsExactly(array(1L), array(2L));
	}

	@Test
	void providesArgumentsUsingDoubleStreamSupplier() {
		var arguments = provideArguments("doubleStreamSupplier");

		assertThat(arguments).containsExactly(array(1.2), array(3.4));
	}

	@Test
	void providesArgumentsUsingStreamSupplierOfIntArrays() {
		var arguments = provideArguments("intArrayStreamSupplier");

		assertThat(arguments).containsExactly( //
			new Object[] { new int[] { 1, 2 } }, //
			new Object[] { new int[] { 3, 4 } } //
		);
	}

	@Test
	void providesArgumentsUsingStreamSupplierOfTwoDimensionalIntArrays() {
		var arguments = provideArguments("twoDimensionalIntArrayStreamSupplier");

		assertThat(arguments).containsExactly( //
			array((Object) new int[][] { { 1, 2 }, { 2, 3 } }), //
			array((Object) new int[][] { { 4, 5 }, { 5, 6 } }) //
		);
	}

	@Test
	void providesArgumentsUsingStreamSupplierOfObjectArrays() {
		var arguments = provideArguments("objectArrayStreamSupplier");

		assertThat(arguments).containsExactly(array("foo", 42), array("bar", 23));
	}

	@Test
	void providesArgumentsUsingStreamSupplierOfTwoDimensionalObjectArrays() {
		var arguments = provideArguments("twoDimensionalObjectArrayStreamSupplier");

		assertThat(arguments).containsExactly( //
			array((Object) array(array("a", 1), array("b", 2))), //
			array((Object) array(array("c", 3), array("d", 4))) //
		);
	}

	@Test
	void providesArgumentsUsingStreamSupplierOfArguments() {
		var arguments = provideArguments("argumentsStreamSupplier");

		assertThat(arguments).containsExactly(array("foo", 42), array("bar", 23));
	}

	@Test
	void providesArgumentsUsingIterable() {
		var arguments = provideArguments("stringIterable");

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void providesArgumentsUsingMultipleFields() {
		var arguments = provideArguments("stringStreamSupplier", "stringIterable");

		assertThat(arguments).containsExactly(array("foo"), array("bar"), array("foo"), array("bar"));
	}

	@Test
	void providesArgumentsUsingIterableOfObjectArrays() {
		var arguments = provideArguments("objectArrayIterable");

		assertThat(arguments).containsExactly(array("foo", 42), array("bar", 23));
	}

	@Test
	void providesArgumentsUsingListOfStrings() {
		var arguments = provideArguments("stringList");

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void providesArgumentsUsingListOfObjectArrays() {
		var arguments = provideArguments("objectArrayList");

		assertThat(arguments).containsExactly(array("foo", 42), array("bar", 23));
	}

	@Test
	void providesArgumentsFromNonStaticFieldWhenStaticIsNotRequired() {
		var lifecyclePerClass = true;
		var arguments = provideArguments(NonStaticTestCase.class, lifecyclePerClass, "nonStaticStringStreamSupplier");

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void providesArgumentsUsingDefaultFieldName() {
		var testClass = DefaultFieldNameTestCase.class;
		var methodName = "testDefaultFieldName";
		var testMethod = findMethod(testClass, methodName, String.class).get();

		var arguments = provideArguments(testClass, testMethod, false, new String[0]);

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void providesArgumentsUsingExternalField() {
		var arguments = provideArguments(ExternalFields.class.getName() + "#strings");

		assertThat(arguments).containsExactly(array("string1"), array("string2"));
	}

	@Test
	void providesArgumentsUsingExternalFieldInTypeFromDifferentClassLoader() throws Exception {
		try (var testClassLoader = TestClassLoader.forClasses(TestCase.class, ExternalFields.class)) {
			var testClass = testClassLoader.loadClass(TestCase.class.getName());
			var fullyQualifiedFieldName = ExternalFields.class.getName() + "#strings";

			assertThat(testClass.getClassLoader()).isSameAs(testClassLoader);

			var arguments = provideArguments(testClass, false, fullyQualifiedFieldName);
			assertThat(arguments).containsExactly(array("string1"), array("string2"));

			var field = FieldArgumentsProvider.findField(testClass, fullyQualifiedFieldName);
			assertThat(field).isNotNull();
			assertThat(field.getName()).isEqualTo("strings");

			var declaringClass = field.getDeclaringClass();
			assertThat(declaringClass.getName()).isEqualTo(ExternalFields.class.getName());
			assertThat(declaringClass).isNotEqualTo(ExternalFields.class);
			assertThat(declaringClass.getClassLoader()).isSameAs(testClassLoader);
		}
	}

	@Test
	void providesArgumentsUsingExternalFieldFromStaticNestedClass() {
		var arguments = provideArguments(ExternalFields.Nested.class.getName() + "#strings");

		assertThat(arguments).containsExactly(array("nested string1"), array("nested string2"));
	}

	@Test
	void providesArgumentsUsingExternalAndInternalFieldsCombined() {
		var arguments = provideArguments("stringStreamSupplier", ExternalFields.class.getName() + "#strings");

		assertThat(arguments).containsExactly(array("foo"), array("bar"), array("string1"), array("string2"));
	}

	@Nested
	class PrimitiveArrays {

		@Test
		void providesArgumentsUsingBooleanArray() {
			var arguments = provideArguments("booleanArray");

			assertThat(arguments).containsExactly(array(Boolean.TRUE), array(Boolean.FALSE));
		}

		@Test
		void providesArgumentsUsingByteArray() {
			var arguments = provideArguments("byteArray");

			assertThat(arguments).containsExactly(array((byte) 1), array(Byte.MIN_VALUE));
		}

		@Test
		void providesArgumentsUsingCharArray() {
			var arguments = provideArguments("charArray");

			assertThat(arguments).containsExactly(array((char) 1), array(Character.MIN_VALUE));
		}

		@Test
		void providesArgumentsUsingDoubleArray() {
			var arguments = provideArguments("doubleArray");

			assertThat(arguments).containsExactly(array(1d), array(Double.MIN_VALUE));
		}

		@Test
		void providesArgumentsUsingFloatArray() {
			var arguments = provideArguments("floatArray");

			assertThat(arguments).containsExactly(array(1f), array(Float.MIN_VALUE));
		}

		@Test
		void providesArgumentsUsingIntArray() {
			var arguments = provideArguments("intArray");

			assertThat(arguments).containsExactly(array(47), array(Integer.MIN_VALUE));
		}

		@Test
		void providesArgumentsUsingLongArray() {
			var arguments = provideArguments("longArray");

			assertThat(arguments).containsExactly(array(47L), array(Long.MIN_VALUE));
		}

		@Test
		void providesArgumentsUsingShortArray() {
			var arguments = provideArguments("shortArray");

			assertThat(arguments).containsExactly(array((short) 47), array(Short.MIN_VALUE));
		}

	}

	@Nested
	class ObjectArrays {

		@Test
		void providesArgumentsUsingObjectArray() {
			var arguments = provideArguments("objectArray");

			assertThat(arguments).containsExactly(array(42), array("bar"));
		}

		@Test
		void providesArgumentsUsingStringArray() {
			var arguments = provideArguments("stringArray");

			assertThat(arguments).containsExactly(array("foo"), array("bar"));
		}

		@Test
		void providesArgumentsUsing2dStringArray() {
			var arguments = provideArguments("twoDimensionalStringArray");

			assertThat(arguments).containsExactly(array("foo", "bar"), array("baz", "qux"));
		}

		@Test
		void providesArgumentsUsing2dObjectArray() {
			var arguments = provideArguments("twoDimensionalObjectArray");

			assertThat(arguments).containsExactly(array("foo", 42), array("bar", 23));
		}

	}

	@Nested
	class ErrorCases {

		@Test
		void throwsExceptionWhenNonStaticLocalFieldIsReferencedWithLifecyclePerMethodSemantics() {
			var lifecyclePerClass = false;
			assertPreconditionViolationFor(() -> provideArguments(NonStaticTestCase.class, lifecyclePerClass,
				"nonStaticStringStreamSupplier").toArray())//
						.withMessageContainingAll("Field '",
							"' must be static: local @FieldSource fields must be static ",
							"unless the PER_CLASS @TestInstance lifecycle mode is used; ",
							"external @FieldSource fields must always be static.");
		}

		@Test
		void throwsExceptionWhenNonStaticExternalFieldIsReferencedWithLifecyclePerMethodSemantics() {
			var factoryClass = NonStaticTestCase.class.getName();
			var field = factoryClass + "#nonStaticStringStreamSupplier";
			var lifecyclePerClass = false;
			assertPreconditionViolationFor(() -> provideArguments(TestCase.class, lifecyclePerClass, field).toArray())//
					.withMessageContainingAll("Field '", "' must be static: local @FieldSource fields must be static ",
						"unless the PER_CLASS @TestInstance lifecycle mode is used; ",
						"external @FieldSource fields must always be static.");
		}

		@Test
		void throwsExceptionWhenNonStaticExternalFieldIsReferencedWithLifecyclePerClassSemantics() {
			var factoryClass = NonStaticTestCase.class.getName();
			var field = factoryClass + "#nonStaticStringStreamSupplier";
			boolean lifecyclePerClass = true;
			assertPreconditionViolationFor(() -> provideArguments(TestCase.class, lifecyclePerClass, field).toArray())//
					.withMessageContainingAll("Field '", "' must be static: local @FieldSource fields must be static ",
						"unless the PER_CLASS @TestInstance lifecycle mode is used; ",
						"external @FieldSource fields must always be static.");
		}

		@ParameterizedTest
		@ValueSource(strings = { "org.example.MyUtils", "org.example.MyUtils#", "#fieldName" })
		void throwsExceptionWhenFullyQualifiedFieldNameSyntaxIsInvalid(String fieldName) {
			assertPreconditionViolationFor(() -> provideArguments(fieldName).toArray())//
					.withMessage("""
							[%s] is not a valid fully qualified field name: \
							it must start with a fully qualified class name followed by a \
							'#' and then the field name.""", fieldName, TestCase.class.getName());
		}

		@Test
		void throwsExceptionWhenClassForExternalFieldCannotBeLoaded() {
			var exception = assertThrows(JUnitException.class,
				() -> provideArguments("com.example.NonExistentClass#strings").toArray());

			assertThat(exception.getMessage()).isEqualTo("Could not load class [com.example.NonExistentClass]");
		}

		@Test
		void throwsExceptionWhenLocalFieldDoesNotExist() {
			assertPreconditionViolationFor(() -> provideArguments("nonExistentField").toArray())//
					.withMessage("Could not find field named [nonExistentField] in class [%s]",
						TestCase.class.getName());
		}

		@ParameterizedTest
		@ValueSource(strings = { "nonExistentField", "strings()" })
		void throwsExceptionWhenExternalFieldDoesNotExist(String fieldName) {
			String factoryClass = ExternalFields.class.getName();

			assertPreconditionViolationFor(() -> provideArguments(factoryClass + "#" + fieldName).toArray())//
					.withMessage("Could not find field named [%s] in class [%s]", fieldName, factoryClass);
		}

		@Test
		void throwsExceptionWhenLocalFieldHasNullValue() {
			String field = "nullList";
			String factoryClass = TestCase.class.getName();

			assertPreconditionViolationFor(() -> provideArguments(field).toArray())//
					.withMessage("The value of field [%s] in class [%s] must not be null", field, factoryClass);
		}

		@Test
		void throwsExceptionWhenLocalFieldHasInvalidReturnType() {
			String field = "object";
			String factoryClass = TestCase.class.getName();

			assertPreconditionViolationFor(() -> provideArguments(field).toArray())//
					.withMessage("The value of field [%s] in class [%s] must be convertible to a Stream", field,
						factoryClass);
		}

		@Test
		void throwsExceptionWhenExternalFieldHasInvalidReturnType() {
			String factoryClass = ExternalFields.class.getName();
			String fieldName = "object";
			String field = factoryClass + "#" + fieldName;

			assertPreconditionViolationFor(() -> provideArguments(TestCase.class, false, field).toArray())//
					.withMessage("The value of field [%s] in class [%s] must be convertible to a Stream", fieldName,
						factoryClass);
		}

		@ParameterizedTest
		@ValueSource(strings = { "stream", "intStream", "longStream", "doubleStream" })
		void throwsExceptionWhenLocalFieldHasStreamReturnType(String field) {
			String factoryClass = TestCase.class.getName();

			assertPreconditionViolationFor(() -> provideArguments(field).toArray())//
					.withMessage("The value of field [%s] in class [%s] must not be a stream", field, factoryClass);
		}

		@Test
		void throwsExceptionWhenLocalFieldHasIteratorReturnType() {
			String field = "iterator";
			String factoryClass = TestCase.class.getName();

			assertPreconditionViolationFor(() -> provideArguments(field).toArray())//
					.withMessage("The value of field [%s] in class [%s] must not be an Iterator", field, factoryClass);
		}

	}

	// -------------------------------------------------------------------------

	private static Object[] array(Object... objects) {
		return objects;
	}

	private static Stream<Object[]> provideArguments(String... fieldNames) {
		return provideArguments(TestCase.class, false, fieldNames);
	}

	private static Stream<Object[]> provideArguments(Class<?> testClass, boolean allowNonStaticMethod,
			String... fieldNames) {

		// Ensure we have a non-null test method, even if it's not a real test method.
		// If this throws an exception, make sure that the supplied test class defines a "void test()" method.
		Method testMethod = ReflectionSupport.findMethod(testClass, "test").get();
		return provideArguments(testClass, testMethod, allowNonStaticMethod, fieldNames);
	}

	private static Stream<Object[]> provideArguments(Class<?> testClass, Method testMethod,
			boolean allowNonStaticMethod, String... fieldNames) {

		var extensionRegistry = createRegistryWithDefaultExtensions(mock());
		var fieldSource = mock(FieldSource.class);

		when(fieldSource.value()).thenReturn(fieldNames);

		var parameters = mock(ParameterDeclarations.class);
		var extensionContext = mock(ExtensionContext.class);
		when(extensionContext.getTestClass()).thenReturn(Optional.of(testClass));
		when(extensionContext.getTestMethod()).thenReturn(Optional.of(testMethod));
		when(extensionContext.getExecutableInvoker()).thenReturn(
			new DefaultExecutableInvoker(extensionContext, extensionRegistry));

		doCallRealMethod().when(extensionContext).getRequiredTestMethod();
		doCallRealMethod().when(extensionContext).getRequiredTestClass();

		var testInstance = allowNonStaticMethod ? ReflectionSupport.newInstance(testClass) : null;
		when(extensionContext.getTestInstance()).thenReturn(Optional.ofNullable(testInstance));

		var lifeCycle = allowNonStaticMethod ? Lifecycle.PER_CLASS : Lifecycle.PER_METHOD;
		when(extensionContext.getTestInstanceLifecycle()).thenReturn(Optional.of(lifeCycle));

		var provider = new FieldArgumentsProvider();
		provider.accept(fieldSource);
		return provider.provideArguments(parameters, extensionContext).map(Arguments::get);
	}

	// -------------------------------------------------------------------------

	static class DefaultFieldNameTestCase {

		// Test
		void testDefaultFieldName(String param) {
		}

		// Field
		static List<String> testDefaultFieldName = List.of("foo", "bar");
	}

	static class TestCase {

		void test() {
		}

		// --- Invalid ---------------------------------------------------------

		@Nullable
		static List<String> nullList = null;

		static Object object = -1;

		static Stream<String> stream = Stream.of("foo", "bar");

		static DoubleStream doubleStream = DoubleStream.of(1.2, 3.4);

		static IntStream intStream = IntStream.of(1, 2);

		static LongStream longStream = LongStream.of(1L, 2L);

		static Iterator<String> iterator = List.of("foo", "bar").iterator();

		// --- Stream Supplier -------------------------------------------------

		static Supplier<Stream<String>> stringStreamSupplier = () -> Stream.of("foo", "bar");

		static Supplier<DoubleStream> doubleStreamSupplier = () -> DoubleStream.of(1.2, 3.4);

		static Supplier<LongStream> longStreamSupplier = () -> LongStream.of(1L, 2L);

		static Supplier<IntStream> intStreamSupplier = () -> IntStream.of(1, 2);

		static Supplier<Stream<int[]>> intArrayStreamSupplier = //
			() -> Stream.of(new int[] { 1, 2 }, new int[] { 3, 4 });

		static Supplier<Stream<int[][]>> twoDimensionalIntArrayStreamSupplier = //
			() -> Stream.of(new int[][] { { 1, 2 }, { 2, 3 } }, new int[][] { { 4, 5 }, { 5, 6 } });

		static Supplier<Stream<Object[]>> objectArrayStreamSupplier = //
			() -> Stream.of(new Object[] { "foo", 42 }, new Object[] { "bar", 23 });

		static Supplier<Stream<Object[][]>> twoDimensionalObjectArrayStreamSupplier = //
			() -> Stream.of(new Object[][] { { "a", 1 }, { "b", 2 } }, new Object[][] { { "c", 3 }, { "d", 4 } });

		static Supplier<Stream<Arguments>> argumentsStreamSupplier = //
			() -> objectArrayStreamSupplier.get().map(Arguments::of);

		// --- Collection / Iterable -------------------------------------------

		static List<String> stringList = List.of("foo", "bar");

		static List<Object[]> objectArrayList = List.of(array("foo", 42), array("bar", 23));

		static Iterable<String> stringIterable = stringList::iterator;

		static Iterable<Object[]> objectArrayIterable = objectArrayList::iterator;

		// --- Array of primitives ---------------------------------------------

		static boolean[] booleanArray = new boolean[] { true, false };

		static byte[] byteArray = new byte[] { (byte) 1, Byte.MIN_VALUE };

		static char[] charArray = new char[] { (char) 1, Character.MIN_VALUE };

		static double[] doubleArray = new double[] { 1d, Double.MIN_VALUE };

		static float[] floatArray = new float[] { 1f, Float.MIN_VALUE };

		static int[] intArray = new int[] { 47, Integer.MIN_VALUE };

		static long[] longArray = new long[] { 47L, Long.MIN_VALUE };

		static short[] shortArray = new short[] { (short) 47, Short.MIN_VALUE };

		// --- Array of objects ------------------------------------------------

		static Object[] objectArray = new Object[] { 42, "bar" };

		static String[] stringArray = new String[] { "foo", "bar" };

		static String[][] twoDimensionalStringArray = new String[][] { { "foo", "bar" }, { "baz", "qux" } };

		static Object[][] twoDimensionalObjectArray = new Object[][] { { "foo", 42 }, { "bar", 23 } };

	}

	// This test case mimics @TestInstance(Lifecycle.PER_CLASS)
	static class NonStaticTestCase {

		void test() {
		}

		Supplier<Stream<String>> nonStaticStringStreamSupplier = () -> Stream.of("foo", "bar");
	}

	static class ExternalFields {

		static Object object = -1;

		static List<String> strings = List.of("string1", "string2");

		static class Nested {

			static List<String> strings = List.of("nested string1", "nested string2");

		}
	}

}
