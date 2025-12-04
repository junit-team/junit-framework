/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.time.Duration.ofMillis;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrBlankFor;
import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP;
import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.util.ReflectionUtils.findFields;
import static org.junit.platform.commons.util.ReflectionUtils.findMethod;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.commons.util.ReflectionUtils.invokeMethod;
import static org.junit.platform.commons.util.ReflectionUtils.isWideningConversion;
import static org.junit.platform.commons.util.ReflectionUtils.readFieldValues;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.test.TestClassLoader;
import org.junit.platform.commons.util.ReflectionUtils.CycleErrorHandling;
import org.junit.platform.commons.util.ReflectionUtilsTests.NestedClassTests.ClassWithNestedClasses.Nested1;
import org.junit.platform.commons.util.ReflectionUtilsTests.NestedClassTests.ClassWithNestedClasses.Nested2;
import org.junit.platform.commons.util.ReflectionUtilsTests.NestedClassTests.ClassWithNestedClasses.Nested3;
import org.junit.platform.commons.util.ReflectionUtilsTests.NestedClassTests.Interface45.Nested5;
import org.junit.platform.commons.util.ReflectionUtilsTests.NestedClassTests.InterfaceWithNestedClass;
import org.junit.platform.commons.util.ReflectionUtilsTests.NestedClassTests.InterfaceWithNestedClass.Nested4;
import org.junit.platform.commons.util.ReflectionUtilsTests.OuterClass.InnerClass;
import org.junit.platform.commons.util.ReflectionUtilsTests.OuterClass.InnerClass.RecursiveInnerInnerClass;
import org.junit.platform.commons.util.ReflectionUtilsTests.OuterClass.InnerSiblingClass;
import org.junit.platform.commons.util.ReflectionUtilsTests.OuterClass.RecursiveInnerClass;
import org.junit.platform.commons.util.ReflectionUtilsTests.OuterClass.StaticNestedClass;
import org.junit.platform.commons.util.ReflectionUtilsTests.OuterClass.StaticNestedSiblingClass;
import org.junit.platform.commons.util.ReflectionUtilsTests.OuterClassImplementingInterface.InnerClassImplementingInterface;
import org.junit.platform.commons.util.classes.CustomType;
import org.junit.platform.commons.util.pkg1.SuperclassWithStaticPackagePrivateBeforeMethod;
import org.junit.platform.commons.util.pkg1.SuperclassWithStaticPackagePrivateTempDirField;
import org.junit.platform.commons.util.pkg1.subpkg.SubclassWithNonStaticPackagePrivateBeforeMethod;
import org.junit.platform.commons.util.pkg1.subpkg.SubclassWithNonStaticPackagePrivateTempDirField;

/**
 * Unit tests for {@link ReflectionUtils}.
 *
 * @since 1.0
 */
class ReflectionUtilsTests {

	private static final Predicate<Method> isFooMethod = method -> method.getName().equals("foo");
	private static final Predicate<Method> methodContains1 = method -> method.getName().contains("1");
	private static final Predicate<Method> methodContains2 = method -> method.getName().contains("2");
	private static final Predicate<Method> methodContains4 = method -> method.getName().contains("4");
	private static final Predicate<Method> methodContains5 = method -> method.getName().contains("5");

	@Nested
	class MiscellaneousTests {

		@Test
		void returnsPrimitiveVoid() throws Exception {
			Class<?> clazz = ClassWithVoidAndNonVoidMethods.class;
			assertTrue(ReflectionUtils.returnsPrimitiveVoid(clazz.getDeclaredMethod("voidMethod")));

			assertFalse(ReflectionUtils.returnsPrimitiveVoid(clazz.getDeclaredMethod("methodReturningVoidReference")));
			assertFalse(ReflectionUtils.returnsPrimitiveVoid(clazz.getDeclaredMethod("methodReturningObject")));
			assertFalse(ReflectionUtils.returnsPrimitiveVoid(clazz.getDeclaredMethod("methodReturningPrimitive")));
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void getAllAssignmentCompatibleClassesWithNullClass() {
			assertPreconditionViolationFor(() -> ReflectionUtils.getAllAssignmentCompatibleClasses(null));
		}

		@Test
		void getAllAssignmentCompatibleClasses() {
			var superclasses = ReflectionUtils.getAllAssignmentCompatibleClasses(B.class);
			assertThat(superclasses).containsExactly(B.class, InterfaceC.class, InterfaceA.class, InterfaceB.class,
				A.class, InterfaceD.class, Object.class);
			assertTrue(superclasses.stream().allMatch(clazz -> clazz.isAssignableFrom(B.class)));
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void newInstance() {
			// @formatter:off
			assertThat(ReflectionUtils.newInstance(C.class, "one", "two")).isNotNull();
			assertThat(ReflectionUtils.newInstance(C.class)).isNotNull();

			assertPreconditionViolationFor(() -> ReflectionUtils.newInstance(C.class, "one", null));
			assertPreconditionViolationFor(() -> ReflectionUtils.newInstance(C.class, null, "two"));
			assertPreconditionViolationFor(() -> ReflectionUtils.newInstance(C.class, null, null));
			assertPreconditionViolationFor(() -> ReflectionUtils.newInstance(C.class, ((Object[]) null)));

			var exception = assertThrows(RuntimeException.class, () -> ReflectionUtils.newInstance(Exploder.class));
			assertThat(exception).hasMessage("boom");
			// @formatter:on
		}

		@Test
		void wideningConversion() {
			// byte
			assertTrue(isWideningConversion(byte.class, short.class));
			assertTrue(isWideningConversion(byte.class, int.class));
			assertTrue(isWideningConversion(byte.class, long.class));
			assertTrue(isWideningConversion(byte.class, float.class));
			assertTrue(isWideningConversion(byte.class, double.class));
			// Byte
			assertTrue(isWideningConversion(Byte.class, short.class));
			assertTrue(isWideningConversion(Byte.class, int.class));
			assertTrue(isWideningConversion(Byte.class, long.class));
			assertTrue(isWideningConversion(Byte.class, float.class));
			assertTrue(isWideningConversion(Byte.class, double.class));

			// short
			assertTrue(isWideningConversion(short.class, int.class));
			assertTrue(isWideningConversion(short.class, long.class));
			assertTrue(isWideningConversion(short.class, float.class));
			assertTrue(isWideningConversion(short.class, double.class));
			// Short
			assertTrue(isWideningConversion(Short.class, int.class));
			assertTrue(isWideningConversion(Short.class, long.class));
			assertTrue(isWideningConversion(Short.class, float.class));
			assertTrue(isWideningConversion(Short.class, double.class));

			// char
			assertTrue(isWideningConversion(char.class, int.class));
			assertTrue(isWideningConversion(char.class, long.class));
			assertTrue(isWideningConversion(char.class, float.class));
			assertTrue(isWideningConversion(char.class, double.class));
			// Character
			assertTrue(isWideningConversion(Character.class, int.class));
			assertTrue(isWideningConversion(Character.class, long.class));
			assertTrue(isWideningConversion(Character.class, float.class));
			assertTrue(isWideningConversion(Character.class, double.class));

			// int
			assertTrue(isWideningConversion(int.class, long.class));
			assertTrue(isWideningConversion(int.class, float.class));
			assertTrue(isWideningConversion(int.class, double.class));
			// Integer
			assertTrue(isWideningConversion(Integer.class, long.class));
			assertTrue(isWideningConversion(Integer.class, float.class));
			assertTrue(isWideningConversion(Integer.class, double.class));

			// long
			assertTrue(isWideningConversion(long.class, float.class));
			assertTrue(isWideningConversion(long.class, double.class));
			// Long
			assertTrue(isWideningConversion(Long.class, float.class));
			assertTrue(isWideningConversion(Long.class, double.class));

			// float
			assertTrue(isWideningConversion(float.class, double.class));
			// Float
			assertTrue(isWideningConversion(Float.class, double.class));

			// double and Double --> nothing to test

			// Unsupported
			assertFalse(isWideningConversion(int.class, byte.class)); // narrowing
			assertFalse(isWideningConversion(float.class, int.class)); // narrowing
			assertFalse(isWideningConversion(int.class, int.class)); // direct match
			assertFalse(isWideningConversion(String.class, int.class)); // neither a primitive nor a wrapper
		}

		@Test
		void getWrapperType() {
			assertEquals(Boolean.class, ReflectionUtils.getWrapperType(boolean.class));
			assertEquals(Byte.class, ReflectionUtils.getWrapperType(byte.class));
			assertEquals(Character.class, ReflectionUtils.getWrapperType(char.class));
			assertEquals(Short.class, ReflectionUtils.getWrapperType(short.class));
			assertEquals(Integer.class, ReflectionUtils.getWrapperType(int.class));
			assertEquals(Long.class, ReflectionUtils.getWrapperType(long.class));
			assertEquals(Float.class, ReflectionUtils.getWrapperType(float.class));
			assertEquals(Double.class, ReflectionUtils.getWrapperType(double.class));
			assertNull(ReflectionUtils.getWrapperType(Object.class));
		}

		@Test
		void getAllClasspathRootDirectories(@TempDir Path tempDirectory) throws Exception {
			var root1 = tempDirectory.resolve("root1").toAbsolutePath();
			var root2 = tempDirectory.resolve("root2").toAbsolutePath();
			var testClassPath = root1 + File.pathSeparator + root2;

			var originalClassPath = System.setProperty("java.class.path", testClassPath);
			try {
				createDirectories(root1, root2);

				assertThat(ReflectionUtils.getAllClasspathRootDirectories()).containsOnly(root1, root2);
			}
			finally {
				System.setProperty("java.class.path", originalClassPath);
			}
		}

		private static void createDirectories(Path... paths) throws IOException {
			for (var path : paths) {
				Files.createDirectory(path);
			}
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void getDeclaredConstructorPreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> ReflectionUtils.getDeclaredConstructor(null));
			assertPreconditionViolationFor(() -> ReflectionUtils.getDeclaredConstructor(ClassWithTwoConstructors.class));
			// @formatter:on
		}

		@Test
		void getDeclaredConstructor() {
			Constructor<?> constructor = ReflectionUtils.getDeclaredConstructor(getClass());
			assertNotNull(constructor);
			assertEquals(getClass(), constructor.getDeclaringClass());

			constructor = ReflectionUtils.getDeclaredConstructor(ClassWithOneCustomConstructor.class);
			assertNotNull(constructor);
			assertEquals(ClassWithOneCustomConstructor.class, constructor.getDeclaringClass());
			assertEquals(String.class, constructor.getParameterTypes()[0]);
		}

		@Test
		void isGeneric() {
			for (var method : Generic.class.getMethods()) {
				assertTrue(ReflectionUtils.isGeneric(method));
			}
			for (var method : NonGenericClass.class.getMethods()) {
				assertFalse(ReflectionUtils.isGeneric(method));
			}
		}

		/**
		 * @see <a href="https://github.com/junit-team/junit-framework/issues/3684">#3684</a>
		 */
		@Test
		void getInterfaceMethodIfPossible() throws Exception {
			// "anonymous" because it's implemented as an anonymous class.
			InputStream anonymousInputStream = InputStream.nullInputStream();
			Class<?> targetType = anonymousInputStream.getClass();
			assertThat(targetType.isAnonymousClass()).isTrue();

			Method method = targetType.getMethod("close");
			assertThat(method).isNotNull();
			assertThat(method.getDeclaringClass()).isEqualTo(targetType);

			Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(method, targetType);
			assertThat(interfaceMethod).isNotNull().isNotEqualTo(method);
			// InputStream implements Closeable directly, so we find the `close` method
			// in Closeable instead of AutoCloseable.
			assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(Closeable.class);
		}

		@Test
		void isRecordObject() {
			assertTrue(ReflectionUtils.isRecordObject(new SomeRecord(1)));
			assertFalse(ReflectionUtils.isRecordObject(new ClassWithOneCustomConstructor("")));
			assertFalse(ReflectionUtils.isRecordObject(null));
		}

		@Test
		void isRecordClass() {
			assertTrue(ReflectionUtils.isRecordClass(SomeRecord.class));
			assertFalse(ReflectionUtils.isRecordClass(ClassWithOneCustomConstructor.class));
			assertFalse(ReflectionUtils.isRecordClass(Object.class));
		}

		record SomeRecord(int n) {
		}

		static class ClassWithVoidAndNonVoidMethods {

			void voidMethod() {
			}

			Void methodReturningVoidReference() {
				return null;
			}

			String methodReturningObject() {
				return "";
			}

			int methodReturningPrimitive() {
				return 0;
			}

		}

		static class Exploder {

			Exploder() {
				throw new RuntimeException("boom");
			}

		}

		interface InterfaceA {
		}

		interface InterfaceB {
		}

		interface InterfaceC extends InterfaceA, InterfaceB {
		}

		interface InterfaceD {
		}

		static class A implements InterfaceA, InterfaceD {
		}

		static class B extends A implements InterfaceC {
		}

		static class C {

			C() {
			}

			C(String a, String b) {
			}

		}

		@SuppressWarnings("unused")
		private static class ClassWithOneCustomConstructor {

			ClassWithOneCustomConstructor(String str) {
			}
		}

		@SuppressWarnings("unused")
		private static class ClassWithTwoConstructors {

			ClassWithTwoConstructors() {
			}

			ClassWithTwoConstructors(String str) {
			}
		}

		public class NonGenericClass {

			public void publicMethod() {
			}
		}

	}

	@Nested
	class ModifierTests {

		@Test
		void isPublic() throws Exception {
			assertTrue(ReflectionUtils.isPublic(PublicClass.class));
			assertTrue(ReflectionUtils.isPublic(PublicClass.class.getMethod("publicMethod")));

			assertFalse(ReflectionUtils.isPublic(PrivateClass.class));
			assertFalse(ReflectionUtils.isPublic(PrivateClass.class.getDeclaredMethod("privateMethod")));
			assertFalse(ReflectionUtils.isPublic(ProtectedClass.class));
			assertFalse(ReflectionUtils.isPublic(ProtectedClass.class.getDeclaredMethod("protectedMethod")));
			assertFalse(ReflectionUtils.isPublic(PackageVisibleClass.class));
			assertFalse(ReflectionUtils.isPublic(PackageVisibleClass.class.getDeclaredMethod("packageVisibleMethod")));
		}

		@Test
		void isPrivate() throws Exception {
			assertTrue(ReflectionUtils.isPrivate(PrivateClass.class));
			assertTrue(ReflectionUtils.isPrivate(PrivateClass.class.getDeclaredMethod("privateMethod")));

			assertFalse(ReflectionUtils.isPrivate(PublicClass.class));
			assertFalse(ReflectionUtils.isPrivate(PublicClass.class.getMethod("publicMethod")));
			assertFalse(ReflectionUtils.isPrivate(ProtectedClass.class));
			assertFalse(ReflectionUtils.isPrivate(ProtectedClass.class.getDeclaredMethod("protectedMethod")));
			assertFalse(ReflectionUtils.isPrivate(PackageVisibleClass.class));
			assertFalse(ReflectionUtils.isPrivate(PackageVisibleClass.class.getDeclaredMethod("packageVisibleMethod")));
		}

		@Test
		void isNotPrivate() throws Exception {
			assertTrue(ReflectionUtils.isNotPrivate(PublicClass.class));
			assertTrue(ReflectionUtils.isNotPrivate(PublicClass.class.getDeclaredMethod("publicMethod")));
			assertTrue(ReflectionUtils.isNotPrivate(ProtectedClass.class));
			assertTrue(ReflectionUtils.isNotPrivate(ProtectedClass.class.getDeclaredMethod("protectedMethod")));
			assertTrue(ReflectionUtils.isNotPrivate(PackageVisibleClass.class));
			assertTrue(
				ReflectionUtils.isNotPrivate(PackageVisibleClass.class.getDeclaredMethod("packageVisibleMethod")));

			assertFalse(ReflectionUtils.isNotPrivate(PrivateClass.class.getDeclaredMethod("privateMethod")));
		}

		@Test
		void isAbstract() throws Exception {
			assertTrue(ReflectionUtils.isAbstract(AbstractClass.class));
			assertTrue(ReflectionUtils.isAbstract(AbstractClass.class.getDeclaredMethod("abstractMethod")));

			assertFalse(ReflectionUtils.isAbstract(PublicClass.class));
			assertFalse(ReflectionUtils.isAbstract(PublicClass.class.getDeclaredMethod("publicMethod")));
		}

		@Test
		void isStatic() throws Exception {
			assertTrue(ReflectionUtils.isStatic(StaticClass.class));
			assertTrue(ReflectionUtils.isStatic(StaticClass.class.getDeclaredMethod("staticMethod")));

			assertFalse(ReflectionUtils.isStatic(PublicClass.class));
			assertFalse(ReflectionUtils.isStatic(PublicClass.class.getDeclaredMethod("publicMethod")));
		}

		@Test
		void isNotStatic() throws Exception {
			assertTrue(ReflectionUtils.isNotStatic(PublicClass.class));
			assertTrue(ReflectionUtils.isNotStatic(PublicClass.class.getDeclaredMethod("publicMethod")));

			assertFalse(ReflectionUtils.isNotStatic(StaticClass.class));
			assertFalse(ReflectionUtils.isNotStatic(StaticClass.class.getDeclaredMethod("staticMethod")));
		}

		@Test
		void isFinal() throws Exception {
			assertTrue(ReflectionUtils.isFinal(FinalClass.class));
			assertTrue(ReflectionUtils.isFinal(FinalClass.class.getDeclaredMethod("finalMethod")));

			assertFalse(ReflectionUtils.isFinal(PublicClass.class));
			assertFalse(ReflectionUtils.isFinal(PublicClass.class.getDeclaredMethod("publicMethod")));
		}

		@Test
		void isNotFinal() throws Exception {
			assertTrue(ReflectionUtils.isNotFinal(PublicClass.class));
			assertTrue(ReflectionUtils.isNotFinal(PublicClass.class.getDeclaredMethod("publicMethod")));

			assertFalse(ReflectionUtils.isNotFinal(FinalClass.class));
			assertFalse(ReflectionUtils.isNotFinal(FinalClass.class.getDeclaredMethod("finalMethod")));
		}

		// Intentionally non-static
		public class PublicClass {

			public void publicMethod() {
			}

			public void method(String str, Integer num) {
			}

			public void method(String[] strings, Integer[] nums) {
			}

			public void method(boolean b, char c) {
			}

			public void method(char[] characters, int[] nums) {
			}
		}

		private class PrivateClass {

			@SuppressWarnings("unused")
			private void privateMethod() {
			}
		}

		protected class ProtectedClass {

			@SuppressWarnings("unused")
			protected void protectedMethod() {
			}
		}

		class PackageVisibleClass {

			@SuppressWarnings("unused")
			void packageVisibleMethod() {
			}
		}

		final class FinalClass {

			@SuppressWarnings({ "unused", "FinalMethodInFinalClass", "RedundantModifier" })
			final void finalMethod() {
			}
		}

		abstract static class AbstractClass {

			abstract void abstractMethod();
		}

		static class StaticClass {

			static void staticMethod() {
			}
		}

	}

	@Nested
	class IsClassAssignableToClassTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void isAssignableToForNullSourceType() {
			assertPreconditionViolationNotNullFor("source type",
				() -> ReflectionUtils.isAssignableTo(null, getClass()));
		}

		@Test
		void isAssignableToForPrimitiveSourceType() {
			assertPreconditionViolationFor(() -> ReflectionUtils.isAssignableTo(int.class, Integer.class))//
					.withMessage("source type must not be a primitive type");
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void isAssignableToForNullTargetType() {
			assertPreconditionViolationNotNullFor("target type",
				() -> ReflectionUtils.isAssignableTo(getClass(), null));
		}

		@Test
		void isAssignableTo() {
			// Reference Types
			assertTrue(ReflectionUtils.isAssignableTo(String.class, Object.class));
			assertTrue(ReflectionUtils.isAssignableTo(String.class, CharSequence.class));
			assertTrue(ReflectionUtils.isAssignableTo(String.class, String.class));
			assertTrue(ReflectionUtils.isAssignableTo(Integer.class, Number.class));
			assertTrue(ReflectionUtils.isAssignableTo(Integer.class, Integer.class));

			assertFalse(ReflectionUtils.isAssignableTo(Object.class, String.class));
			assertFalse(ReflectionUtils.isAssignableTo(CharSequence.class, String.class));
			assertFalse(ReflectionUtils.isAssignableTo(Number.class, Integer.class));

			// Arrays
			assertTrue(ReflectionUtils.isAssignableTo(int[].class, int[].class));
			assertTrue(ReflectionUtils.isAssignableTo(double[].class, double[].class));
			assertTrue(ReflectionUtils.isAssignableTo(double[].class, Object.class));
			assertTrue(ReflectionUtils.isAssignableTo(String[].class, Object.class));
			assertTrue(ReflectionUtils.isAssignableTo(String[].class, Object[].class));
			assertTrue(ReflectionUtils.isAssignableTo(String[].class, String[].class));

			// Wrappers to Primitives
			assertTrue(ReflectionUtils.isAssignableTo(Integer.class, int.class));
			assertTrue(ReflectionUtils.isAssignableTo(Boolean.class, boolean.class));

			// Void to void
			assertFalse(ReflectionUtils.isAssignableTo(Void.class, void.class));

			// Widening Conversions from Wrappers to Primitives
			assertTrue(ReflectionUtils.isAssignableTo(Integer.class, long.class));
			assertTrue(ReflectionUtils.isAssignableTo(Float.class, double.class));
			assertTrue(ReflectionUtils.isAssignableTo(Byte.class, double.class));

			// Widening Conversions from Wrappers to Wrappers (not supported by Java)
			assertFalse(ReflectionUtils.isAssignableTo(Integer.class, Long.class));
			assertFalse(ReflectionUtils.isAssignableTo(Float.class, Double.class));
			assertFalse(ReflectionUtils.isAssignableTo(Byte.class, Double.class));

			// Narrowing Conversions
			assertFalse(ReflectionUtils.isAssignableTo(Integer.class, char.class));
			assertFalse(ReflectionUtils.isAssignableTo(Long.class, byte.class));
			assertFalse(ReflectionUtils.isAssignableTo(Long.class, int.class));
		}

	}

	@Nested
	class IsObjectAssignableToClassTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void isAssignableToForNullClass() {
			assertPreconditionViolationFor(() -> ReflectionUtils.isAssignableTo(new Object(), null));
		}

		@Test
		void isAssignableTo() {
			// Reference Types
			assertTrue(ReflectionUtils.isAssignableTo("string", String.class));
			assertTrue(ReflectionUtils.isAssignableTo("string", CharSequence.class));
			assertTrue(ReflectionUtils.isAssignableTo("string", Object.class));

			assertFalse(ReflectionUtils.isAssignableTo(new Object(), String.class));
			assertFalse(ReflectionUtils.isAssignableTo(Integer.valueOf("1"), StringBuilder.class));
			assertFalse(ReflectionUtils.isAssignableTo(new StringBuilder(), String.class));

			// Arrays
			assertTrue(ReflectionUtils.isAssignableTo(new int[0], int[].class));
			assertTrue(ReflectionUtils.isAssignableTo(new double[0], Object.class));
			assertTrue(ReflectionUtils.isAssignableTo(new String[0], String[].class));
			assertTrue(ReflectionUtils.isAssignableTo(new String[0], Object.class));

			// Primitive Types
			assertTrue(ReflectionUtils.isAssignableTo(1, int.class));
			assertTrue(ReflectionUtils.isAssignableTo(Long.valueOf("1"), long.class));
			assertTrue(ReflectionUtils.isAssignableTo(Boolean.TRUE, boolean.class));

			// Widening Conversions to Primitives
			assertTrue(ReflectionUtils.isAssignableTo(1, long.class));
			assertTrue(ReflectionUtils.isAssignableTo(1f, double.class));
			assertTrue(ReflectionUtils.isAssignableTo((byte) 1, double.class));

			// Widening Conversions to Wrappers (not supported by Java)
			assertFalse(ReflectionUtils.isAssignableTo(1, Long.class));
			assertFalse(ReflectionUtils.isAssignableTo(1f, Double.class));
			assertFalse(ReflectionUtils.isAssignableTo((byte) 1, Double.class));

			// Narrowing Conversions
			assertFalse(ReflectionUtils.isAssignableTo(1, char.class));
			assertFalse(ReflectionUtils.isAssignableTo(1L, byte.class));
			assertFalse(ReflectionUtils.isAssignableTo(1L, int.class));
		}

		@Test
		void isAssignableToForNullObject() {
			assertTrue(ReflectionUtils.isAssignableTo((Object) null, Object.class));
			assertTrue(ReflectionUtils.isAssignableTo((Object) null, String.class));
			assertTrue(ReflectionUtils.isAssignableTo((Object) null, Long.class));
			assertTrue(ReflectionUtils.isAssignableTo((Object) null, Character[].class));
		}

		@Test
		void isAssignableToForNullObjectAndPrimitive() {
			assertFalse(ReflectionUtils.isAssignableTo((Object) null, byte.class));
			assertFalse(ReflectionUtils.isAssignableTo((Object) null, int.class));
			assertFalse(ReflectionUtils.isAssignableTo((Object) null, long.class));
			assertFalse(ReflectionUtils.isAssignableTo((Object) null, boolean.class));
		}

	}

	@Nested
	class MethodInvocationTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void invokeMethodPreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> invokeMethod(null, new Object()));
			assertPreconditionViolationFor(() -> invokeMethod(Object.class.getMethod("hashCode"), null));
			// @formatter:on
		}

		@Test
		void invokePublicMethod() throws Exception {
			var tracker = new InvocationTracker();
			invokeMethod(InvocationTracker.class.getDeclaredMethod("publicMethod"), tracker);
			assertTrue(tracker.publicMethodInvoked);
		}

		@Test
		void invokePrivateMethod() throws Exception {
			var tracker = new InvocationTracker();
			invokeMethod(InvocationTracker.class.getDeclaredMethod("privateMethod"), tracker);
			assertTrue(tracker.privateMethodInvoked);
		}

		@Test
		void invokePublicStaticMethod() throws Exception {
			invokeMethod(InvocationTracker.class.getDeclaredMethod("publicStaticMethod"), null);
			assertTrue(InvocationTracker.publicStaticMethodInvoked);
		}

		@Test
		void invokePrivateStaticMethod() throws Exception {
			invokeMethod(InvocationTracker.class.getDeclaredMethod("privateStaticMethod"), null);
			assertTrue(InvocationTracker.privateStaticMethodInvoked);
		}

		static class InvocationTracker {

			static boolean publicStaticMethodInvoked;
			static boolean privateStaticMethodInvoked;

			boolean publicMethodInvoked;
			boolean privateMethodInvoked;

			public static void publicStaticMethod() {
				publicStaticMethodInvoked = true;
			}

			@SuppressWarnings("unused")
			private static void privateStaticMethod() {
				privateStaticMethodInvoked = true;
			}

			public void publicMethod() {
				publicMethodInvoked = true;
			}

			@SuppressWarnings("unused")
			private void privateMethod() {
				privateMethodInvoked = true;
			}
		}

	}

	@Nested
	class ResourceLoadingTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void tryToGetResourcePreconditions() {
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToGetResources(""));
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToGetResources("   "));

			assertPreconditionViolationFor(() -> ReflectionUtils.tryToGetResources(null));
			assertPreconditionViolationFor(
				() -> ReflectionUtils.tryToGetResources("org/junit/platform/commons/example.resource", null));
		}

		@Test
		void tryToGetResource() {
			var tryToGetResource = ReflectionUtils.tryToGetResources("org/junit/platform/commons/example.resource");
			var resource = assertDoesNotThrow(tryToGetResource::get);
			assertAll( //
				() -> assertThat(resource).hasSize(1), //
				() -> assertThat(resource).extracting(Resource::getName) //
						.containsExactly("org/junit/platform/commons/example.resource"));
		}

		@Test
		void tryToGetResourceWithPrefixedSlash() {
			var tryToGetResource = ReflectionUtils.tryToGetResources("/org/junit/platform/commons/example.resource");
			var resource = assertDoesNotThrow(tryToGetResource::get);
			assertAll( //
				() -> assertThat(resource).hasSize(1), //
				() -> assertThat(resource).extracting(Resource::getName) //
						.containsExactly("org/junit/platform/commons/example.resource"));
		}

		@Test
		void tryToGetResourceWhenResourceNotFound() {
			var tryToGetResource = ReflectionUtils.tryToGetResources("org/junit/platform/commons/no-such.resource");
			var resource = assertDoesNotThrow(tryToGetResource::get);
			assertThat(resource).isEmpty();
		}
	}

	@Nested
	class ClassLoadingTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void tryToLoadClassPreconditions() {
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToLoadClass(null));
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToLoadClass(""));
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToLoadClass("   "));

			assertPreconditionViolationFor(() -> ReflectionUtils.tryToLoadClass(null, null));
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToLoadClass(getClass().getName(), null));
		}

		@Test
		void tryToLoadClassWhenClassNotFoundException() {
			assertThrows(ClassNotFoundException.class,
				() -> ReflectionUtils.tryToLoadClass("foo.bar.EnigmaClassThatDoesNotExist").get());
		}

		@Test
		void tryToLoadClassFailsWithinReasonableTimeForInsanelyLargeAndInvalidMultidimensionalPrimitiveArrayName() {
			// Create a class name of the form int[][][]...[][][]X
			String className = IntStream.rangeClosed(1, 20_000)//
					.mapToObj(i -> "[]")//
					.collect(joining("", "int", "X"));

			// The following should ideally fail in less than 50ms. So we just make
			// sure it fails in less than 500ms in order to (hopefully) allow the
			// test to pass on CI servers with limited resources.
			assertTimeoutPreemptively(ofMillis(500), () -> assertThrows(ClassNotFoundException.class,
				() -> ReflectionUtils.tryToLoadClass(className).get()));
		}

		@Test
		void tryToLoadClass() {
			assertTryToLoadClass(getClass().getName(), getClass());
			assertTryToLoadClass(Integer.class.getName(), Integer.class);
			assertTryToLoadClass(Void.class.getName(), Void.class);
		}

		@Test
		void tryToLoadClassTrimsClassName() {
			assertTryToLoadClass("  " + Integer.class.getName() + "\t", Integer.class);
		}

		@Test
		void tryToLoadClassForVoidPseudoPrimitiveType() {
			assertTryToLoadClass("void", void.class);
		}

		@Test
		void tryToLoadClassForPrimitiveType() {
			assertTryToLoadClass("boolean", boolean.class);
			assertTryToLoadClass("char", char.class);
			assertTryToLoadClass("byte", byte.class);
			assertTryToLoadClass("short", short.class);
			assertTryToLoadClass("int", int.class);
			assertTryToLoadClass("long", long.class);
			assertTryToLoadClass("float", float.class);
			assertTryToLoadClass("double", double.class);
		}

		@Test
		void tryToLoadClassForBinaryPrimitiveArrayName() {
			assertTryToLoadClass("[Z", boolean[].class);
			assertTryToLoadClass("[C", char[].class);
			assertTryToLoadClass("[B", byte[].class);
			assertTryToLoadClass("[S", short[].class);
			assertTryToLoadClass("[I", int[].class);
			assertTryToLoadClass("[J", long[].class);
			assertTryToLoadClass("[F", float[].class);
			assertTryToLoadClass("[D", double[].class);
		}

		@Test
		void tryToLoadClassForCanonicalPrimitiveArrayName() {
			assertTryToLoadClass("boolean[]", boolean[].class);
			assertTryToLoadClass("char[]", char[].class);
			assertTryToLoadClass("byte[]", byte[].class);
			assertTryToLoadClass("short[]", short[].class);
			assertTryToLoadClass("int[]", int[].class);
			assertTryToLoadClass("long[]", long[].class);
			assertTryToLoadClass("float[]", float[].class);
			assertTryToLoadClass("double[]", double[].class);
		}

		@Test
		void tryToLoadClassForBinaryObjectArrayName() {
			assertTryToLoadClass(String[].class.getName(), String[].class);
		}

		@Test
		void tryToLoadClassForCanonicalObjectArrayName() {
			assertTryToLoadClass("java.lang.String[]", String[].class);
		}

		@Test
		void tryToLoadClassForBinaryTwoDimensionalPrimitiveArrayName() {
			assertTryToLoadClass("[[Z", boolean[][].class);
			assertTryToLoadClass("[[C", char[][].class);
			assertTryToLoadClass("[[B", byte[][].class);
			assertTryToLoadClass("[[S", short[][].class);
			assertTryToLoadClass("[[I", int[][].class);
			assertTryToLoadClass("[[J", long[][].class);
			assertTryToLoadClass("[[F", float[][].class);
			assertTryToLoadClass("[[D", double[][].class);
		}

		@Test
		void tryToLoadClassForCanonicalTwoDimensionalPrimitiveArrayName() {
			assertTryToLoadClass("boolean[][]", boolean[][].class);
			assertTryToLoadClass("char[][]", char[][].class);
			assertTryToLoadClass("byte[][]", byte[][].class);
			assertTryToLoadClass("short[][]", short[][].class);
			assertTryToLoadClass("int[][]", int[][].class);
			assertTryToLoadClass("long[][]", long[][].class);
			assertTryToLoadClass("float[][]", float[][].class);
			assertTryToLoadClass("double[][]", double[][].class);
		}

		@Test
		void tryToLoadClassForBinaryMultidimensionalPrimitiveArrayName() {
			assertTryToLoadClass("[[[[[Z", boolean[][][][][].class);
			assertTryToLoadClass("[[[[[C", char[][][][][].class);
			assertTryToLoadClass("[[[[[B", byte[][][][][].class);
			assertTryToLoadClass("[[[[[S", short[][][][][].class);
			assertTryToLoadClass("[[[[[I", int[][][][][].class);
			assertTryToLoadClass("[[[[[J", long[][][][][].class);
			assertTryToLoadClass("[[[[[F", float[][][][][].class);
			assertTryToLoadClass("[[[[[D", double[][][][][].class);
		}

		@Test
		void tryToLoadClassForCanonicalMultidimensionalPrimitiveArrayName() {
			assertTryToLoadClass("boolean[][][][][]", boolean[][][][][].class);
			assertTryToLoadClass("char[][][][][]", char[][][][][].class);
			assertTryToLoadClass("byte[][][][][]", byte[][][][][].class);
			assertTryToLoadClass("short[][][][][]", short[][][][][].class);
			assertTryToLoadClass("int[][][][][]", int[][][][][].class);
			assertTryToLoadClass("long[][][][][]", long[][][][][].class);
			assertTryToLoadClass("float[][][][][]", float[][][][][].class);
			assertTryToLoadClass("double[][][][][]", double[][][][][].class);
		}

		@Test
		void tryToLoadClassForBinaryMultidimensionalObjectArrayName() {
			assertTryToLoadClass(String[][][][][].class.getName(), String[][][][][].class);
		}

		@Test
		void tryToLoadClassForCanonicalMultidimensionalObjectArrayName() {
			assertTryToLoadClass("java.lang.String[][][][][]", String[][][][][].class);
		}

		private static void assertTryToLoadClass(String name, Class<?> type) {
			try {
				assertThat(ReflectionUtils.tryToLoadClass(name).get()).as(name).isEqualTo(type);
			}
			catch (Exception ex) {
				ExceptionUtils.throwAsUncheckedException(ex);
			}
		}

	}

	@Nested
	class FullyQualifiedMethodNameTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void getFullyQualifiedMethodNamePreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> ReflectionUtils.getFullyQualifiedMethodName(null, null));
			assertPreconditionViolationFor(() -> ReflectionUtils.getFullyQualifiedMethodName(null, "testMethod"));
			assertPreconditionViolationFor(() -> ReflectionUtils.getFullyQualifiedMethodName(Object.class, null));
			// @formatter:on
		}

		@Test
		void getFullyQualifiedMethodNameForMethodWithoutParameters() {
			assertThat(ReflectionUtils.getFullyQualifiedMethodName(Object.class, "toString"))//
					.isEqualTo("java.lang.Object#toString()");
		}

		@Test
		void getFullyQualifiedMethodNameForMethodWithNullParameters() {
			assertThat(ReflectionUtils.getFullyQualifiedMethodName(Object.class, "toString", (Class<?>[]) null))//
					.isEqualTo("java.lang.Object#toString()");
		}

		@Test
		void getFullyQualifiedMethodNameForMethodWithSingleParameter() {
			assertThat(ReflectionUtils.getFullyQualifiedMethodName(Object.class, "equals", Object.class))//
					.isEqualTo("java.lang.Object#equals(java.lang.Object)");
		}

		@Test
		void getFullyQualifiedMethodNameForMethodWithMultipleParameters() {
			// @formatter:off
			assertThat(ReflectionUtils.getFullyQualifiedMethodName(Object.class, "testMethod", int.class, Object.class))//
					.isEqualTo("java.lang.Object#testMethod(int, java.lang.Object)");
			// @formatter:on
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void parseFullyQualifiedMethodNamePreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName(null));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName(""));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("   "));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("java.lang.Object#"));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("#equals"));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("#"));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("java.lang.Object"));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("equals"));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("()"));
			assertPreconditionViolationFor(() -> ReflectionUtils.parseFullyQualifiedMethodName("(int, java.lang.Object)"));
			// @formatter:on
		}

		@Test
		void parseFullyQualifiedMethodNameForMethodWithoutParameters() {
			assertThat(ReflectionUtils.parseFullyQualifiedMethodName("com.example.Test#method()"))//
					.containsExactly("com.example.Test", "method", "");
		}

		@Test
		void parseFullyQualifiedMethodNameForMethodWithSingleParameter() {
			assertThat(ReflectionUtils.parseFullyQualifiedMethodName("com.example.Test#method(java.lang.Object)"))//
					.containsExactly("com.example.Test", "method", "java.lang.Object");
		}

		@Test
		void parseFullyQualifiedMethodNameForMethodWithMultipleParameters() {
			assertThat(ReflectionUtils.parseFullyQualifiedMethodName("com.example.Test#method(int, java.lang.Object)"))//
					.containsExactly("com.example.Test", "method", "int, java.lang.Object");
		}

	}

	@Nested
	class NestedClassTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void findNestedClassesPreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> ReflectionUtils.findNestedClasses(null, null));
			assertPreconditionViolationFor(() -> ReflectionUtils.findNestedClasses(null, clazz -> true));
			assertPreconditionViolationFor(() -> ReflectionUtils.findNestedClasses(getClass(), null));
			// @formatter:on
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void isNestedClassPresentPreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> ReflectionUtils.isNestedClassPresent(null, null, null));
			assertPreconditionViolationFor(() -> ReflectionUtils.isNestedClassPresent(null, clazz -> true, CycleErrorHandling.THROW_EXCEPTION));
			assertPreconditionViolationFor(() -> ReflectionUtils.isNestedClassPresent(getClass(), null, CycleErrorHandling.ABORT_VISIT));
			assertPreconditionViolationFor(() -> ReflectionUtils.isNestedClassPresent(getClass(), clazz -> true, null));
			// @formatter:on
		}

		@Test
		void findNestedClasses() {
			// @formatter:off
			assertThat(findNestedClasses(Object.class)).isEmpty();
			assertThat(isNestedClassPresent(Object.class)).isFalse();

			assertThat(findNestedClasses(ClassWithNestedClasses.class))
					.containsOnly(Nested1.class, Nested2.class, Nested3.class);
			assertThat(isNestedClassPresent(ClassWithNestedClasses.class))
					.isTrue();

			assertThat(ReflectionUtils.findNestedClasses(ClassWithNestedClasses.class, clazz -> clazz.getName().contains("1")))
					.containsExactly(Nested1.class);
			assertThat(ReflectionUtils.isNestedClassPresent(ClassWithNestedClasses.class, clazz -> clazz.getName().contains("1"), CycleErrorHandling.THROW_EXCEPTION))
					.isTrue();

			assertThat(ReflectionUtils.findNestedClasses(ClassWithNestedClasses.class, ReflectionUtils::isStatic))
					.containsExactly(Nested3.class);
			assertThat(ReflectionUtils.isNestedClassPresent(ClassWithNestedClasses.class, ReflectionUtils::isStatic, CycleErrorHandling.THROW_EXCEPTION))
					.isTrue();

			assertThat(findNestedClasses(ClassExtendingClassWithNestedClasses.class))
					.containsOnly(Nested1.class, Nested2.class, Nested3.class, Nested4.class, Nested5.class);
			assertThat(isNestedClassPresent(ClassExtendingClassWithNestedClasses.class))
					.isTrue();

			assertThat(findNestedClasses(ClassWithNestedClasses.Nested1.class)).isEmpty();
			assertThat(isNestedClassPresent(ClassWithNestedClasses.Nested1.class)).isFalse();
			// @formatter:on
		}

		/**
		 * @since 1.6
		 */
		@Test
		void findNestedClassesWithSeeminglyRecursiveHierarchies() {
			assertThat(findNestedClasses(AbstractOuterClass.class))//
					.containsExactly(AbstractOuterClass.InnerClass.class);
			assertThat(isNestedClassPresent(AbstractOuterClass.class))//
					.isTrue();

			// OuterClass contains recursive hierarchies, but the non-matching
			// predicate should prevent cycle detection.
			// See https://github.com/junit-team/junit-framework/issues/2249
			assertThat(ReflectionUtils.findNestedClasses(OuterClass.class, clazz -> false)).isEmpty();
			assertThat(ReflectionUtils.isNestedClassPresent(OuterClass.class, clazz -> false,
				CycleErrorHandling.THROW_EXCEPTION)).isFalse();

			// RecursiveInnerInnerClass is part of a recursive hierarchy, but the non-matching
			// predicate should prevent cycle detection.
			assertThat(ReflectionUtils.findNestedClasses(RecursiveInnerInnerClass.class, clazz -> false)).isEmpty();
			assertThat(ReflectionUtils.isNestedClassPresent(RecursiveInnerInnerClass.class, clazz -> false,
				CycleErrorHandling.THROW_EXCEPTION)).isFalse();

			// Sibling types don't actually result in cycles.
			assertThat(findNestedClasses(StaticNestedSiblingClass.class))//
					.containsExactly(AbstractOuterClass.InnerClass.class);
			assertThat(isNestedClassPresent(StaticNestedSiblingClass.class))//
					.isTrue();
			assertThat(findNestedClasses(InnerSiblingClass.class))//
					.containsExactly(AbstractOuterClass.InnerClass.class);
			assertThat(isNestedClassPresent(InnerSiblingClass.class))//
					.isTrue();

			// Interfaces with static nested classes
			assertThat(findNestedClasses(OuterClassImplementingInterface.class))//
					.containsExactly(InnerClassImplementingInterface.class, Nested4.class);
			assertThat(isNestedClassPresent(OuterClassImplementingInterface.class))//
					.isTrue();
			assertThat(findNestedClasses(InnerClassImplementingInterface.class))//
					.containsExactly(Nested4.class);
			assertThat(isNestedClassPresent(InnerClassImplementingInterface.class))//
					.isTrue();
		}

		/**
		 * @since 1.6
		 */
		@Test
		void findNestedClassesWithRecursiveHierarchies() {
			Runnable runnable1 = () -> assertNestedCycle(OuterClass.class, InnerClass.class, OuterClass.class);
			Runnable runnable2 = () -> assertNestedCycle(StaticNestedClass.class, InnerClass.class, OuterClass.class);
			Runnable runnable3 = () -> assertNestedCycle(RecursiveInnerClass.class, OuterClass.class);
			Runnable runnable4 = () -> assertNestedCycle(RecursiveInnerInnerClass.class, OuterClass.class);
			Runnable runnable5 = () -> assertNestedCycle(InnerClass.class, RecursiveInnerInnerClass.class,
				OuterClass.class);
			Stream.of(runnable1, runnable1, runnable1, runnable2, runnable2, runnable2, runnable3, runnable3, runnable3,
				runnable4, runnable4, runnable4, runnable5, runnable5, runnable5).parallel().forEach(Runnable::run);
		}

		@Test
		void findNestedClassesWithMultipleNestedClasses() {
			var nestedClasses = findNestedClasses(OuterClassWithMultipleNestedClasses.class);

			assertThat(nestedClasses) //
					.map(Class::getSimpleName) //
					.containsExactly("Beta", "Zeta", "Eta", "Alpha", "Delta", "Gamma", "Theta", "Epsilon");
		}

		private static List<Class<?>> findNestedClasses(Class<?> clazz) {
			return ReflectionUtils.findNestedClasses(clazz, c -> true);
		}

		private static boolean isNestedClassPresent(Class<?> clazz) {
			return ReflectionUtils.isNestedClassPresent(clazz, c -> true, CycleErrorHandling.THROW_EXCEPTION);
		}

		private void assertNestedCycle(Class<?> from, Class<?> to) {
			assertNestedCycle(from, from, to);
		}

		private void assertNestedCycle(Class<?> start, Class<?> from, Class<?> to) {
			assertThatExceptionOfType(JUnitException.class)//
					.as("expected cycle from %s to %s", from.getSimpleName(), to.getSimpleName())//
					.isThrownBy(() -> findNestedClasses(start))//
					.withMessageMatching("Detected cycle in inner class hierarchy between .+%s and .+%s".formatted(
						from.getSimpleName(), to.getSimpleName()));
		}

		/**
		 * @since 1.3
		 */
		@Test
		void findNestedClassesWithInvalidNestedClassFile(@TrackLogRecords LogRecordListener listener) throws Exception {
			var jarUrl = getClass().getResource("/gh-1436-invalid-nested-class-file.jar");

			try (var classLoader = new URLClassLoader(new URL[] { jarUrl })) {
				var fqcn = "tests.NestedInterfaceGroovyTests";
				var classWithInvalidNestedClassFile = classLoader.loadClass(fqcn);

				assertEquals(fqcn, classWithInvalidNestedClassFile.getName());
				var noClassDefFoundError = assertThrows(NoClassDefFoundError.class,
					classWithInvalidNestedClassFile::getDeclaredClasses);
				assertThat(noClassDefFoundError) //
						.hasMessageMatching("tests[./]NestedInterfaceGroovyTests\\$NestedInterface\\$1");

				assertThat(findNestedClasses(classWithInvalidNestedClassFile)).isEmpty();
				// @formatter:off
				var logMessage = listener.stream(ReflectionUtils.class, Level.FINE)
						.findFirst()
						.map(LogRecord::getMessage)
						.orElse("didn't find log record");
				// @formatter:on
				assertThat(logMessage).isEqualTo("Failed to retrieve declared classes for " + fqcn);
			}
		}

		static class ClassWithNestedClasses {

			class Nested1 {
			}

			class Nested2 {
			}

			static class Nested3 {
			}
		}

		interface InterfaceWithNestedClass {

			class Nested4 {
			}
		}

		interface Interface45 extends InterfaceWithNestedClass {

			class Nested5 {
			}
		}

		static class ClassExtendingClassWithNestedClasses extends ClassWithNestedClasses implements Interface45 {
		}

	}

	@Nested
	class MethodUtilitiesTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void tryToGetMethodPreconditions() {
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToGetMethod(null, null));
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToGetMethod(String.class, null));
			assertPreconditionViolationFor(() -> ReflectionUtils.tryToGetMethod(null, "hashCode"));
		}

		@Test
		void tryToGetMethod() throws Exception {
			assertThat(ReflectionUtils.tryToGetMethod(Object.class, "hashCode").get())//
					.isEqualTo(Object.class.getMethod("hashCode"));
			assertThat(ReflectionUtils.tryToGetMethod(String.class, "charAt", int.class).get())//
					.isEqualTo(String.class.getMethod("charAt", int.class));

			assertThat(ReflectionUtils.tryToGetMethod(Path.class, "subpath", int.class, int.class).get())//
					.isEqualTo(Path.class.getMethod("subpath", int.class, int.class));
			assertThat(ReflectionUtils.tryToGetMethod(String.class, "chars").get())//
					.isEqualTo(String.class.getMethod("chars"));

			assertThat(ReflectionUtils.tryToGetMethod(String.class, "noSuchMethod").toOptional()).isEmpty();
			assertThat(ReflectionUtils.tryToGetMethod(Object.class, "clone", int.class).toOptional()).isEmpty();
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void isMethodPresentPreconditions() {
			assertPreconditionViolationFor(() -> ReflectionUtils.isMethodPresent(null, m -> true));
			assertPreconditionViolationFor(() -> ReflectionUtils.isMethodPresent(getClass(), null));
		}

		@Test
		void isMethodPresent() {
			Predicate<Method> isMethod1 = method -> (method.getName().equals("method1")
					&& method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == String.class);

			assertThat(ReflectionUtils.isMethodPresent(MethodShadowingChild.class, isMethod1)).isTrue();

			assertThat(ReflectionUtils.isMethodPresent(getClass(), isMethod1)).isFalse();
		}

	}

	@Nested
	class FindMethodTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void findMethodByParameterTypesPreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> findMethod(null, null));
			assertPreconditionViolationFor(() -> findMethod(null, "method"));

			assertPreconditionViolationNotNullOrBlankFor("Method name", () -> findMethod(String.class, null));

			assertPreconditionViolationNotNullOrBlankFor("Method name", () -> findMethod(String.class, "   "));

			assertPreconditionViolationNotNullFor("Parameter types array",
					() -> findMethod(Files.class, "copy", (Class<?>[]) null));

			assertPreconditionViolationNotNullFor("Individual parameter types",
					() -> findMethod(Files.class, "copy", (Class<?>) null));

			assertPreconditionViolationNotNullFor("Individual parameter types",
					() -> findMethod(Files.class, "copy", new Class<?>[] { Path.class, null }));
			// @formatter:on
		}

		@Test
		void findMethodByParameterTypes() throws Exception {
			assertThat(findMethod(Object.class, "noSuchMethod")).isEmpty();
			assertThat(findMethod(String.class, "noSuchMethod")).isEmpty();

			assertThat(findMethod(String.class, "chars")).contains(String.class.getMethod("chars"));
			assertThat(findMethod(Files.class, "copy", Path.class, OutputStream.class))//
					.contains(Files.class.getMethod("copy", Path.class, OutputStream.class));

			assertThat(findMethod(MethodShadowingChild.class, "method1", String.class))//
					.contains(MethodShadowingChild.class.getMethod("method1", String.class));
		}

		@Test
		void findMethodByParameterTypesInGenericInterface() {
			Class<?> ifc = InterfaceWithGenericDefaultMethod.class;
			var method = findMethod(ifc, "foo", Number.class);
			assertThat(method).isNotEmpty();
			assertThat(method.get().getName()).isEqualTo("foo");
		}

		/**
		 * @see #findMethodByParameterTypesWithOverloadedMethodNextToGenericDefaultMethod()
		 */
		@Test
		void findMethodByParameterTypesInGenericInterfaceViaParameterizedSubclass() {
			Class<?> clazz = InterfaceWithGenericDefaultMethodImpl.class;
			var method = findMethod(clazz, "foo", Long.class);
			assertThat(method).isNotEmpty();
			assertThat(method.get().getName()).isEqualTo("foo");

			// One might expect or desire that the signature for the generic foo(N)
			// default method would be "foo(java.lang.Long)" when looked up via the
			// concrete parameterized class, but it apparently is only _visible_ as
			// "foo(java.lang.Number)" via reflection. Hence the following assertion
			// checks for java.lang.Number instead of java.lang.Long.
			assertThat(method.get().getParameterTypes()[0]).isEqualTo(Number.class);
		}

		/**
		 * This test is identical to
		 * {@link #findMethodByParameterTypesInGenericInterfaceViaParameterizedSubclass()},
		 * except that this test attempts to find the overloaded
		 * {@link InterfaceWithGenericDefaultMethodImpl#foo(Double)} method instead of
		 * the {@link InterfaceWithGenericDefaultMethod#foo(Number)} default method.
		 */
		@Test
		void findMethodByParameterTypesWithOverloadedMethodNextToGenericDefaultMethod() {
			Class<?> clazz = InterfaceWithGenericDefaultMethodImpl.class;
			Class<?> parameterType = Double.class;
			var method = findMethod(clazz, "foo", parameterType);
			assertThat(method).isNotEmpty();
			assertThat(method.get().getName()).isEqualTo("foo");
			assertThat(method.get().getParameterTypes()[0]).isEqualTo(parameterType);
		}

		@Test
		void findMethodByParameterNamesWithPrimitiveArrayParameter() throws Exception {
			assertFindMethodByParameterNames("methodWithPrimitiveArray", int[].class);
		}

		@Test
		void findMethodByParameterNamesWithTwoDimensionalPrimitiveArrayParameter() throws Exception {
			assertFindMethodByParameterNames("methodWithTwoDimensionalPrimitiveArray", int[][].class);
		}

		@Test
		void findMethodByParameterNamesWithMultidimensionalPrimitiveArrayParameter() throws Exception {
			assertFindMethodByParameterNames("methodWithMultidimensionalPrimitiveArray", int[][][][][].class);
		}

		@Test
		void findMethodByParameterNamesWithObjectArrayParameter() throws Exception {
			assertFindMethodByParameterNames("methodWithObjectArray", String[].class);
		}

		@Test
		void findMethodByParameterNamesWithMultidimensionalObjectArrayParameter() throws Exception {
			assertFindMethodByParameterNames("methodWithMultidimensionalObjectArray", Double[][][][][].class);
		}

		/**
		 * @since 5.10
		 */
		@Test
		void findMethodByParameterNamesWithWithCustomTypeFromDifferentClassLoader() throws Exception {
			var methodName = "customMethod";
			var customTypeName = CustomType.class.getName();
			var nestedTypeName = CustomType.NestedType.class.getName();

			try (var testClassLoader = TestClassLoader.forClassNamePrefix(customTypeName)) {
				var customType = testClassLoader.loadClass(customTypeName);
				assertThat(customType.getClassLoader()).isSameAs(testClassLoader);

				var optional = findMethod(customType, methodName, nestedTypeName);
				assertThat(optional).get().satisfies(method -> {
					assertThat(method.getName()).isEqualTo(methodName);

					var declaringClass = method.getDeclaringClass();
					assertThat(declaringClass.getClassLoader()).isSameAs(testClassLoader);
					assertThat(declaringClass.getName()).isEqualTo(customTypeName);
					assertThat(declaringClass).isNotEqualTo(CustomType.class);

					var parameterTypes = method.getParameterTypes();
					assertThat(parameterTypes).extracting(Class::getName).containsExactly(nestedTypeName);
					Class<?> parameterType = parameterTypes[0];
					assertThat(parameterType).isNotEqualTo(CustomType.NestedType.class);
					assertThat(parameterType.getClassLoader()).isSameAs(testClassLoader);
				});
			}
		}

		@Test
		void findMethodByParameterNamesWithParameterizedMapParameter() throws Exception {
			var methodName = "methodWithParameterizedMap";

			// standard, supported use case
			assertFindMethodByParameterNames(methodName, Map.class);

			// generic type info in parameter list
			var clazz = getClass();
			var method = clazz.getDeclaredMethod(methodName, Map.class);
			var genericParameterTypeName = method.getGenericParameterTypes()[0].getTypeName();
			var exception = assertThrows(JUnitException.class,
				() -> findMethod(clazz, methodName, genericParameterTypeName));

			assertThat(exception).hasMessageStartingWith(
				"Failed to load parameter type [java.util.Map<java.lang.String");
		}

		private void assertFindMethodByParameterNames(String methodName, Class<?> parameterType)
				throws NoSuchMethodException {

			var clazz = getClass();
			var method = clazz.getDeclaredMethod(methodName, parameterType);
			var optional = findMethod(clazz, methodName, parameterType.getName());
			assertThat(optional).contains(method);
		}

		void methodWithPrimitiveArray(int[] nums) {
		}

		void methodWithTwoDimensionalPrimitiveArray(int[][] grid) {
		}

		void methodWithMultidimensionalPrimitiveArray(int[][][][][] grid) {
		}

		void methodWithObjectArray(String[] info) {
		}

		void methodWithTwoDimensionalObjectArray(String[][] info) {
		}

		void methodWithMultidimensionalObjectArray(Double[][][][][] data) {
		}

		void methodWithParameterizedMap(Map<String, String> map) {
		}

	}

	@Nested
	class FindMethodsTests {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void findMethodsPreconditions() {
			// @formatter:off
			assertPreconditionViolationFor(() -> findMethods(null, null));
			assertPreconditionViolationFor(() -> findMethods(null, clazz -> true));
			assertPreconditionViolationFor(() -> findMethods(String.class, null));

			assertPreconditionViolationFor(() -> findMethods(null, null, null));
			assertPreconditionViolationFor(() -> findMethods(null, clazz -> true, BOTTOM_UP));
			assertPreconditionViolationFor(() -> findMethods(String.class, null, BOTTOM_UP));
			assertPreconditionViolationFor(() -> findMethods(String.class, clazz -> true, null));
			// @formatter:on
		}

		@Test
		void findMethodsInInterface() {
			assertOneFooMethodIn(InterfaceWithOneDeclaredMethod.class);
			assertOneFooMethodIn(InterfaceWithDefaultMethod.class);
			assertOneFooMethodIn(InterfaceWithDefaultMethodImpl.class);
			assertOneFooMethodIn(InterfaceWithStaticMethod.class);
			assertOneFooMethodIn(InterfaceWithStaticMethodImpl.class);
		}

		private static void assertOneFooMethodIn(Class<?> clazz) {
			assertThat(findMethods(clazz, isFooMethod)).hasSize(1);
		}

		/**
		 * @since 1.9.1
		 * @see https://github.com/junit-team/junit-framework/issues/2993
		 */
		@Test
		void findMethodsFindsDistinctMethodsDeclaredInMultipleInterfaces() {
			Predicate<Method> isStringsMethod = method -> method.getName().equals("strings");
			assertThat(findMethods(DoubleInheritedInterfaceMethodTestCase.class, isStringsMethod)).hasSize(1);
		}

		@Test
		void findMethodsInObject() {
			var methods = findMethods(Object.class, method -> true);
			assertNotNull(methods);
			assertTrue(methods.size() > 10);
		}

		@Test
		void findMethodsInVoid() {
			assertThat(findMethods(void.class, method -> true)).isEmpty();
			assertThat(findMethods(Void.class, method -> true)).isEmpty();
		}

		@Test
		void findMethodsInPrimitive() {
			assertThat(findMethods(int.class, method -> true)).isEmpty();
		}

		@Test
		void findMethodsInArrays() {
			assertThat(findMethods(int[].class, method -> true)).isEmpty();
			assertThat(findMethods(Integer[].class, method -> true)).isEmpty();
		}

		@Test
		void findMethodsIgnoresSyntheticMethods() {
			assertTrue(stream(ClassWithSyntheticMethod.class.getDeclaredMethods()).anyMatch(Method::isSynthetic),
				"ClassWithSyntheticMethod must actually contain at least one synthetic method.");

			var methods = findMethods(ClassWithSyntheticMethod.class, method -> true);
			assertThat(methods).isEmpty();
		}

		@Test
		void findMethodsUsingHierarchyUpMode() throws Exception {
			assertThat(findMethods(ChildClass.class, method -> method.getName().contains("method"), BOTTOM_UP))//
					.containsExactly(ChildClass.class.getMethod("method4"), ParentClass.class.getMethod("method3"),
						GrandparentInterface.class.getMethod("method2"), GrandparentClass.class.getMethod("method1"));

			assertThat(findMethods(ChildClass.class, method -> method.getName().contains("other"), BOTTOM_UP))//
					.containsExactly(ChildClass.class.getMethod("otherMethod3"),
						ParentClass.class.getMethod("otherMethod2"), GrandparentClass.class.getMethod("otherMethod1"));

			assertThat(findMethods(ChildClass.class, method -> method.getName().equals("method2"), BOTTOM_UP))//
					.containsExactly(ParentClass.class.getMethod("method2"));

			assertThat(findMethods(ChildClass.class, method -> method.getName().equals("wrongName"), BOTTOM_UP))//
					.isEmpty();

			assertThat(findMethods(ParentClass.class, method -> method.getName().contains("method"), BOTTOM_UP))//
					.containsExactly(ParentClass.class.getMethod("method3"),
						GrandparentInterface.class.getMethod("method2"), GrandparentClass.class.getMethod("method1"));
		}

		@Test
		void findMethodsUsingHierarchyDownMode() throws Exception {
			assertThat(findMethods(ChildClass.class, method -> method.getName().contains("method"), TOP_DOWN))//
					.containsExactly(GrandparentClass.class.getMethod("method1"),
						GrandparentInterface.class.getMethod("method2"), ParentClass.class.getMethod("method3"),
						ChildClass.class.getMethod("method4"));

			assertThat(findMethods(ChildClass.class, method -> method.getName().contains("other"), TOP_DOWN))//
					.containsExactly(GrandparentClass.class.getMethod("otherMethod1"),
						ParentClass.class.getMethod("otherMethod2"), ChildClass.class.getMethod("otherMethod3"));

			assertThat(findMethods(ChildClass.class, method -> method.getName().equals("method2"), TOP_DOWN))//
					.containsExactly(ParentClass.class.getMethod("method2"));

			assertThat(findMethods(ChildClass.class, method -> method.getName().equals("wrongName"), TOP_DOWN))//
					.isEmpty();

			assertThat(findMethods(ParentClass.class, method -> method.getName().contains("method"), TOP_DOWN))//
					.containsExactly(GrandparentClass.class.getMethod("method1"),
						GrandparentInterface.class.getMethod("method2"), ParentClass.class.getMethod("method3"));
		}

		@Test
		void findMethodsWithShadowingUsingHierarchyUpMode() throws Exception {
			assertThat(findMethods(MethodShadowingChild.class, methodContains1, BOTTOM_UP))//
					.containsExactly(MethodShadowingChild.class.getMethod("method1", String.class));

			assertThat(findMethods(MethodShadowingChild.class, methodContains2, BOTTOM_UP))//
					.containsExactly(MethodShadowingParent.class.getMethod("method2", int.class, int.class, int.class),
						MethodShadowingInterface.class.getMethod("method2", int.class, int.class));

			assertThat(findMethods(MethodShadowingChild.class, methodContains4, BOTTOM_UP))//
					.containsExactly(MethodShadowingChild.class.getMethod("method4", boolean.class));

			assertThat(findMethods(MethodShadowingChild.class, methodContains5, BOTTOM_UP))//
					.containsExactly(MethodShadowingChild.class.getMethod("method5", Long.class),
						MethodShadowingParent.class.getMethod("method5", String.class));

			var methods = findMethods(MethodShadowingChild.class, method -> true, BOTTOM_UP);
			assertEquals(6, methods.size());
			assertThat(methods.subList(0, 3)).containsOnly(
				MethodShadowingChild.class.getMethod("method4", boolean.class),
				MethodShadowingChild.class.getMethod("method1", String.class),
				MethodShadowingChild.class.getMethod("method5", Long.class));
			assertThat(methods.subList(3, 5)).containsOnly(
				MethodShadowingParent.class.getMethod("method2", int.class, int.class, int.class),
				MethodShadowingParent.class.getMethod("method5", String.class));
			assertEquals(MethodShadowingInterface.class.getMethod("method2", int.class, int.class), methods.get(5));
		}

		@Test
		void findMethodsWithShadowingUsingHierarchyDownMode() throws Exception {
			assertThat(findMethods(MethodShadowingChild.class, methodContains1, TOP_DOWN))//
					.containsExactly(MethodShadowingChild.class.getMethod("method1", String.class));

			assertThat(findMethods(MethodShadowingChild.class, methodContains2, TOP_DOWN))//
					.containsExactly(MethodShadowingInterface.class.getMethod("method2", int.class, int.class),
						MethodShadowingParent.class.getMethod("method2", int.class, int.class, int.class));

			assertThat(findMethods(MethodShadowingChild.class, methodContains4, TOP_DOWN))//
					.containsExactly(MethodShadowingChild.class.getMethod("method4", boolean.class));

			assertThat(findMethods(MethodShadowingChild.class, methodContains5, TOP_DOWN))//
					.containsExactly(MethodShadowingParent.class.getMethod("method5", String.class),
						MethodShadowingChild.class.getMethod("method5", Long.class));

			var methods = findMethods(MethodShadowingChild.class, method -> true, TOP_DOWN);
			assertEquals(6, methods.size());
			assertEquals(MethodShadowingInterface.class.getMethod("method2", int.class, int.class), methods.getFirst());
			assertThat(methods.subList(1, 3)).containsOnly(
				MethodShadowingParent.class.getMethod("method2", int.class, int.class, int.class),
				MethodShadowingParent.class.getMethod("method5", String.class));
			assertThat(methods.subList(3, 6)).containsOnly(
				MethodShadowingChild.class.getMethod("method4", boolean.class),
				MethodShadowingChild.class.getMethod("method1", String.class),
				MethodShadowingChild.class.getMethod("method5", Long.class));
		}

		/**
		 * In non-legacy mode, "static hiding" does not occur.
		 */
		@Test
		void findMethodsWithoutStaticHidingUsingHierarchyUpMode() throws Exception {
			Class<?> ifc = StaticMethodHidingInterface.class;
			Class<?> parent = StaticMethodHidingParent.class;
			Class<?> child = StaticMethodHidingChild.class;

			var ifcMethod1 = ifc.getDeclaredMethod("method1", String.class);
			var ifcMethod2 = ifc.getDeclaredMethod("method2", int.class, int.class);
			var childMethod1 = child.getDeclaredMethod("method1", String.class);
			var childMethod4 = child.getDeclaredMethod("method4", boolean.class);
			var childMethod5 = child.getDeclaredMethod("method5", Long.class);
			var parentMethod1 = parent.getDeclaredMethod("method1", String.class);
			var parentMethod2 = parent.getDeclaredMethod("method2", int.class, int.class, int.class);
			var parentMethod4 = parent.getDeclaredMethod("method4", boolean.class);
			var parentMethod5 = parent.getDeclaredMethod("method5", String.class);

			assertThat(findMethods(child, methodContains1, BOTTOM_UP)).containsExactly(childMethod1, parentMethod1,
				ifcMethod1);
			assertThat(findMethods(child, methodContains2, BOTTOM_UP)).containsExactly(parentMethod2, ifcMethod2);
			assertThat(findMethods(child, methodContains4, BOTTOM_UP)).containsExactly(childMethod4, parentMethod4);
			assertThat(findMethods(child, methodContains5, BOTTOM_UP)).containsExactly(childMethod5, parentMethod5);

			var methods = findMethods(child, method -> true, BOTTOM_UP);
			assertEquals(9, methods.size());
			assertThat(methods.subList(0, 3)).containsOnly(childMethod1, childMethod4, childMethod5);
			assertThat(methods.subList(3, 7)).containsOnly(parentMethod1, parentMethod2, parentMethod4, parentMethod5);
			assertThat(methods.subList(7, 9)).containsOnly(ifcMethod1, ifcMethod2);
		}

		/**
		 * In non-legacy mode, "static hiding" does not occur.
		 */
		@Test
		void findMethodsWithoutStaticHidingUsingHierarchyDownMode() throws Exception {
			Class<?> ifc = StaticMethodHidingInterface.class;
			Class<?> parent = StaticMethodHidingParent.class;
			Class<?> child = StaticMethodHidingChild.class;

			var ifcMethod1 = ifc.getDeclaredMethod("method1", String.class);
			var ifcMethod2 = ifc.getDeclaredMethod("method2", int.class, int.class);
			var childMethod1 = child.getDeclaredMethod("method1", String.class);
			var childMethod4 = child.getDeclaredMethod("method4", boolean.class);
			var childMethod5 = child.getDeclaredMethod("method5", Long.class);
			var parentMethod1 = parent.getDeclaredMethod("method1", String.class);
			var parentMethod2 = parent.getDeclaredMethod("method2", int.class, int.class, int.class);
			var parentMethod4 = parent.getDeclaredMethod("method4", boolean.class);
			var parentMethod5 = parent.getDeclaredMethod("method5", String.class);

			assertThat(findMethods(child, methodContains1, TOP_DOWN)).containsExactly(ifcMethod1, parentMethod1,
				childMethod1);
			assertThat(findMethods(child, methodContains2, TOP_DOWN)).containsExactly(ifcMethod2, parentMethod2);
			assertThat(findMethods(child, methodContains4, TOP_DOWN)).containsExactly(parentMethod4, childMethod4);
			assertThat(findMethods(child, methodContains5, TOP_DOWN)).containsExactly(parentMethod5, childMethod5);

			var methods = findMethods(child, method -> true, TOP_DOWN);
			assertEquals(9, methods.size());
			assertThat(methods.subList(0, 2)).containsOnly(ifcMethod1, ifcMethod2);
			assertThat(methods.subList(2, 6)).containsOnly(parentMethod1, parentMethod2, parentMethod4, parentMethod5);
			assertThat(methods.subList(6, 9)).containsOnly(childMethod1, childMethod4, childMethod5);
		}

		@Test
		void findMethodsDoesNotReturnOverriddenMethods() {
			Predicate<Method> isSpecial = method -> method.isAnnotationPresent(Special.class);

			// Search for all @Special methods.
			var methods = findMethods(SuperclassWithInstanceMethods.class, isSpecial);

			assertThat(signaturesOf(methods))//
					.containsExactlyInAnyOrder("specialFoo()", "specialFoo(int)", "specialFoo(char)", "specialBar()",
						"specialBaz()");

			// Search for all @Special methods.
			methods = findMethods(SubclassWithOverriddenInstanceMethods.class, isSpecial);

			assertThat(signaturesOf(methods))//
					.containsExactlyInAnyOrder("foo()", "specialFoo()", "specialFoo(int)", "specialBar()");
		}

		@Test
		void findMethodsReturnsAllOverloadedMethodsInGenericTypeHierarchy() {
			Class<?> clazz = InterfaceWithGenericDefaultMethodImpl.class;

			// Search for all foo(*) methods.
			var methods = findMethods(clazz, isFooMethod);

			// One might expect or desire that the signature for the generic foo(N)
			// default method would be "foo(java.lang.Long)" when looked up via the
			// concrete parameterized class, but it apparently is only _visible_ as
			// "foo(java.lang.Number)" via reflection.
			assertThat(signaturesOf(methods)).containsExactly("foo(java.lang.Number)", "foo(java.lang.Double)");
		}

		@Test
		void findMethodsDoesNotReturnOverriddenDefaultMethods() {
			Class<?> clazz = InterfaceWithOverriddenGenericDefaultMethodImpl.class;

			// Search for all foo(*) methods.
			var methods = findMethods(clazz, isFooMethod);
			var signatures = signaturesOf(methods);

			// Although the subsequent assertion covers this case as well, this
			// assertion is in place to provide a more informative failure message.
			assertThat(signatures).as("overridden default method should not be in results").doesNotContain(
				"foo(java.lang.Number)");
			assertThat(signatures).containsExactly("foo(java.lang.Long)", "foo(java.lang.Double)");
		}

		private static List<String> signaturesOf(List<Method> methods) {
			// @formatter:off
			return methods.stream()
					.map(m -> "%s(%s)".formatted(m.getName(), ClassUtils.nullSafeToString(m.getParameterTypes())))
					.toList();
			// @formatter:on
		}

		@Test
		void findMethodsIgnoresBridgeMethods() throws Exception {
			assertFalse(Modifier.isPublic(PublicChildClass.class.getSuperclass().getModifiers()));
			assertTrue(Modifier.isPublic(PublicChildClass.class.getModifiers()));
			assertTrue(PublicChildClass.class.getDeclaredMethod("method1").isBridge());
			assertTrue(PublicChildClass.class.getDeclaredMethod("method3").isBridge());

			var methods = findMethods(PublicChildClass.class, method -> true);
			var signatures = signaturesOf(methods);
			assertThat(signatures).containsOnly("method1()", "method2()", "method3()", "otherMethod1()",
				"otherMethod2()");
			assertEquals(0, methods.stream().filter(Method::isBridge).count());
		}

		/**
		 * @see https://github.com/junit-team/junit-framework/issues/3553
		 */
		@Test
		void findMethodsDoesNotAllowInstanceMethodToHideStaticMethod() throws Exception {
			final String BEFORE = "before";
			Class<?> superclass = SuperclassWithStaticPackagePrivateBeforeMethod.class;
			Method staticMethod = superclass.getDeclaredMethod(BEFORE);
			Class<?> subclass = SubclassWithNonStaticPackagePrivateBeforeMethod.class;
			Method nonStaticMethod = subclass.getDeclaredMethod(BEFORE);

			// Prerequisite
			var methods = findMethods(superclass, ReflectionUtils::isStatic);
			assertThat(methods).containsExactly(staticMethod);

			// Actual use cases for this test
			methods = findMethods(subclass, ReflectionUtils::isStatic);
			assertThat(methods).containsExactly(staticMethod);
			methods = findMethods(subclass, ReflectionUtils::isNotStatic);
			assertThat(methods).containsExactly(nonStaticMethod);
		}

		interface StringsInterface1 {
			static Stream<String> strings() {
				return Stream.of("abc", "def");
			}
		}

		interface StringsInterface2 extends StringsInterface1 {
		}

		/**
		 * Inherits strings() from interfaces StringsInterface1 and StringsInterface2.
		 */
		static class DoubleInheritedInterfaceMethodTestCase implements StringsInterface1, StringsInterface2 {
		}

		@Target(ElementType.METHOD)
		@Retention(RetentionPolicy.RUNTIME)
		@interface Special {
		}

		static class SuperclassWithInstanceMethods {

			void foo() {
			}

			void bar() {
			}

			void baz() {
			}

			@Special
			void specialFoo() {
			}

			@Special
			void specialFoo(int i) {
			}

			@Special
			void specialFoo(char ch) {
			}

			@Special
			int specialBar() {
				return 99;
			}

			@Special
			String specialBaz() {
				return "42";
			}
		}

		static class SubclassWithOverriddenInstanceMethods extends SuperclassWithInstanceMethods {

			// foo() is now special.
			@Special
			@Override
			void foo() {
			}

			// No longer special.
			// Simulates overriding a @Test method without redeclaring @Test.
			@Override
			void specialFoo(char ch) {
			}

			// No longer special.
			// Simulates overriding a @TestFactory method without redeclaring @TestFactory.
			@Override
			String specialBaz() {
				return super.specialBaz();
			}
		}

	}

	@Nested
	class ReadFieldTests {

		@Test
		void tryToReadFieldValueOfNonexistentStaticField() {
			assertThrows(NoSuchFieldException.class,
				() -> tryToReadFieldValue(MyClass.class, "doesNotExist", null).get());
			assertThrows(NoSuchFieldException.class,
				() -> tryToReadFieldValue(MySubClass.class, "staticField", null).get());
		}

		@Test
		void tryToReadFieldValueOfNonexistentInstanceField() {
			assertThrows(NoSuchFieldException.class,
				() -> tryToReadFieldValue(MyClass.class, "doesNotExist", new MyClass(42)).get());
			assertThrows(NoSuchFieldException.class,
				() -> tryToReadFieldValue(MyClass.class, "doesNotExist", new MySubClass(42)).get());
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void tryToReadFieldValueOfExistingStaticField() throws Exception {
			assertThat(tryToReadFieldValue(MyClass.class, "staticField", null).get()).isEqualTo(42);

			var field = MyClass.class.getDeclaredField("staticField");
			assertThat(tryToReadFieldValue(field).get()).isEqualTo(42);
			assertThat(tryToReadFieldValue(field, null).get()).isEqualTo(42);
		}

		@Test
		void tryToReadFieldValueOfExistingInstanceField() throws Exception {
			var instance = new MyClass(42);
			assertThat(tryToReadFieldValue(MyClass.class, "instanceField", instance).get()).isEqualTo(42);

			var field = MyClass.class.getDeclaredField("instanceField");
			assertThat(tryToReadFieldValue(field, instance).getNonNull()).isEqualTo(42);

			assertPreconditionViolationFor(() -> tryToReadFieldValue(field, null).get())//
					.withMessageStartingWith("Cannot read non-static field")//
					.withMessageEndingWith("on a null instance.");
		}

	}

	@Nested
	class FindAndReadFieldsTests {

		/**
		 * @see https://github.com/junit-team/junit-framework/issues/3553
		 */
		@Test
		void findFieldsDoesNotAllowInstanceFieldToHideStaticField() throws Exception {
			final String TEMP_DIR = "tempDir";
			Class<?> superclass = SuperclassWithStaticPackagePrivateTempDirField.class;
			Field staticField = superclass.getDeclaredField(TEMP_DIR);
			Class<?> subclass = SubclassWithNonStaticPackagePrivateTempDirField.class;
			Field nonStaticField = subclass.getDeclaredField(TEMP_DIR);

			// Prerequisite
			var fields = findFields(superclass, ReflectionUtils::isStatic, TOP_DOWN);
			assertThat(fields).containsExactly(staticField);

			// Actual use cases for this test
			fields = findFields(subclass, ReflectionUtils::isStatic, TOP_DOWN);
			assertThat(fields).containsExactly(staticField);
			fields = findFields(subclass, ReflectionUtils::isNotStatic, TOP_DOWN);
			assertThat(fields).containsExactly(nonStaticField);
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void readFieldValuesPreconditions() {
			List<Field> fields = new ArrayList<>();
			assertPreconditionViolationFor(() -> readFieldValues(null, new Object()));
			assertPreconditionViolationFor(() -> readFieldValues(fields, null, null));
			assertPreconditionViolationFor(() -> readFieldValues(fields, new Object(), null));
		}

		@Test
		void readFieldValuesFromInstance() {
			var fields = findFields(ClassWithFields.class, f -> true, TOP_DOWN);

			var values = readFieldValues(fields, new ClassWithFields());

			Assertions.<Object> assertThat(values).containsExactly("enigma", 3.14, "text", 2.5, null, 42, "constant",
				99);
		}

		@Test
		void readFieldValuesFromClass() {
			var fields = findFields(ClassWithFields.class, ReflectionUtils::isStatic, TOP_DOWN);

			var values = readFieldValues(fields, null);

			Assertions.<Object> assertThat(values).containsExactly(2.5, "constant", 99);
		}

		/**
		 * @see https://github.com/junit-team/junit-framework/issues/3646
		 * @since 1.11
		 */
		@Test
		void readFieldValuesFromInterfacesAndClassesInTypeHierarchy() {
			var fields = findFields(InterfaceWithField.class, ReflectionUtils::isStatic, TOP_DOWN);
			var values = readFieldValues(fields, null);
			Assertions.<Object> assertThat(values).containsOnly("ifc");

			fields = findFields(SuperclassWithFieldAndFieldFromInterface.class, ReflectionUtils::isStatic, TOP_DOWN);
			values = readFieldValues(fields, null);
			Assertions.<Object> assertThat(values).containsExactly("ifc", "super");

			fields = findFields(SubclassWithFieldAndFieldFromInterface.class, ReflectionUtils::isStatic, TOP_DOWN);
			values = readFieldValues(fields, null);
			Assertions.<Object> assertThat(values).containsExactly("ifc", "super", "sub");
		}

		@Test
		void readFieldValuesFromInstanceWithTypeFilterForString() {
			var fields = findFields(ClassWithFields.class, isA(String.class), TOP_DOWN);

			var values = readFieldValues(fields, new ClassWithFields(), isA(String.class));

			Assertions.<Object> assertThat(values).containsExactly("enigma", "text", null, "constant");
		}

		@Test
		void readFieldValuesFromClassWithTypeFilterForString() {
			var fields = findFields(ClassWithFields.class, isA(String.class).and(ReflectionUtils::isStatic), TOP_DOWN);

			var values = readFieldValues(fields, null, isA(String.class));

			Assertions.<Object> assertThat(values).containsExactly("constant");
		}

		@Test
		void readFieldValuesFromInstanceWithTypeFilterForInteger() {
			var fields = findFields(ClassWithFields.class, isA(int.class), TOP_DOWN);

			var values = readFieldValues(fields, new ClassWithFields(), isA(int.class));

			Assertions.<Object> assertThat(values).containsExactly(42);
		}

		@Test
		void readFieldValuesFromClassWithTypeFilterForInteger() {
			var fields = findFields(ClassWithFields.class, isA(Integer.class).and(ReflectionUtils::isStatic), TOP_DOWN);

			var values = readFieldValues(fields, null, isA(Integer.class));

			Assertions.<Object> assertThat(values).containsExactly(99);
		}

		@Test
		void readFieldValuesFromInstanceWithTypeFilterForDouble() {
			var fields = findFields(ClassWithFields.class, isA(double.class), TOP_DOWN);

			var values = readFieldValues(fields, new ClassWithFields(), isA(double.class));

			Assertions.<Object> assertThat(values).containsExactly(3.14);
		}

		@Test
		void readFieldValuesFromClassWithTypeFilterForDouble() {
			var fields = findFields(ClassWithFields.class, isA(Double.class).and(ReflectionUtils::isStatic), TOP_DOWN);

			var values = readFieldValues(fields, null, isA(Double.class));

			Assertions.<Object> assertThat(values).containsExactly(2.5);
		}

		private static Predicate<Field> isA(Class<?> type) {
			return f -> f.getType().isAssignableFrom(type);
		}

		public static class ClassWithFields {

			public static final String CONST = "constant";

			public static final Integer CONST_INTEGER = 99;

			public static final Double CONST_DOUBLE = 2.5;

			public final String stringField = "text";

			@SuppressWarnings("unused")
			private final String privateStringField = "enigma";

			@Nullable
			final String nullStringField = null;

			public final int integerField = 42;

			public final double doubleField = 3.14;

		}

		interface InterfaceWithField {

			String interfacePath = "ifc";
		}

		static class SuperclassWithFieldAndFieldFromInterface implements InterfaceWithField {

			static final String superPath = "super";
		}

		static class SubclassWithFieldAndFieldFromInterface extends SuperclassWithFieldAndFieldFromInterface
				implements InterfaceWithField {

			static final String subPath = "sub";
		}

	}

	// -------------------------------------------------------------------------

	interface Generic<X, Y, Z extends X> {

		X foo();

		Y foo(X x, Y y);

		Z foo(Z[][] zees);

		<T> int foo(T t);

		<T> T foo(int i);
	}

	class ClassWithSyntheticMethod {

		// The following lambda expression results in a synthetic method in the
		// compiled byte code.
		Comparable<Number> synthetic = number -> 0;
	}

	interface InterfaceWithOneDeclaredMethod {

		void foo();
	}

	interface InterfaceWithDefaultMethod {

		default void foo() {
		}
	}

	static class InterfaceWithDefaultMethodImpl implements InterfaceWithDefaultMethod {
	}

	interface InterfaceWithGenericDefaultMethod<N extends Number> {

		default void foo(N number) {
		}
	}

	static class InterfaceWithGenericDefaultMethodImpl implements InterfaceWithGenericDefaultMethod<Long> {

		void foo(Double number) {
		}
	}

	static class InterfaceWithOverriddenGenericDefaultMethodImpl implements InterfaceWithGenericDefaultMethod<Long> {

		@Override
		public void foo(Long number) {
		}

		void foo(Double number) {
		}
	}

	interface InterfaceWithStaticMethod {

		static void foo() {
		}
	}

	static class InterfaceWithStaticMethodImpl implements InterfaceWithStaticMethod {
	}

	static class MyClass {

		static final int staticField = 42;

		final int instanceField;

		MyClass(int value) {
			this.instanceField = value;
		}
	}

	static class MySubClass extends MyClass {

		MySubClass(int value) {
			super(value);
		}
	}

	static class GrandparentClass {

		public void method1() {
		}

		public void otherMethod1() {
		}
	}

	interface GrandparentInterface {

		default void method2() {
		}
	}

	static class ParentClass extends GrandparentClass implements GrandparentInterface {

		public void method3() {
		}

		public void otherMethod2() {
		}
	}

	static class ChildClass extends ParentClass {

		public void method4() {
		}

		public void otherMethod3() {
		}
	}

	// "public" modifier is necessary here, so that the compiler creates a bridge method.
	public static class PublicChildClass extends ParentClass {

		@Override
		public void otherMethod1() {
		}

		@Override
		public void otherMethod2() {
		}
	}

	interface MethodShadowingInterface {

		default void method1(String string) {
		}

		default void method2(int i, int j) {
		}
	}

	static class MethodShadowingParent implements MethodShadowingInterface {

		@Override
		public void method1(String string) {
		}

		public void method2(int i, int j, int k) {
		}

		public void method4(boolean flag) {
		}

		public void method5(String string) {
		}
	}

	static class MethodShadowingChild extends MethodShadowingParent {

		@Override
		public void method1(String string) {
		}

		@Override
		public void method4(boolean flag) {
		}

		public void method5(Long i) {
		}
	}

	interface StaticMethodHidingInterface {

		static void method1(String string) {
		}

		static void method2(int i, int j) {
		}
	}

	static class StaticMethodHidingParent implements StaticMethodHidingInterface {

		static void method1(String string) {
		}

		static void method2(int i, int j, int k) {
		}

		static void method4(boolean flag) {
		}

		static void method5(String string) {
		}
	}

	static class StaticMethodHidingChild extends StaticMethodHidingParent {

		static void method1(String string) {
		}

		static void method4(boolean flag) {
		}

		static void method5(Long i) {
		}
	}

	abstract static class AbstractOuterClass {

		class InnerClass {
		}
	}

	static class OuterClass extends AbstractOuterClass {

		// sibling of OuterClass due to common super type
		static class StaticNestedSiblingClass extends AbstractOuterClass {
		}

		// sibling of OuterClass due to common super type
		class InnerSiblingClass extends AbstractOuterClass {
		}

		static class StaticNestedClass extends OuterClass {
		}

		class RecursiveInnerClass extends OuterClass {
		}

		class InnerClass {
			class RecursiveInnerInnerClass extends OuterClass {
			}
		}
	}

	static class OuterClassImplementingInterface implements InterfaceWithNestedClass {

		class InnerClassImplementingInterface implements InterfaceWithNestedClass {
		}
	}

	static class OuterClassWithMultipleNestedClasses {
		class Alpha {
		}
		class Beta {
		}
		class Gamma {
		}
		class Delta {
		}
		class Epsilon {
		}
		class Zeta {
		}
		class Eta {
		}
		class Theta {
		}
	}

}
