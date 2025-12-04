/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MethodSource}.
 *
 * @since 1.0
 */
class MethodSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<Serializable> createSerializableInstances() throws Exception {
		return Stream.of( //
			MethodSource.from(getMethod("method1")), //
			MethodSource.from(getMethod("method2")) //
		);
	}

	@Test
	void methodSource() throws Exception {
		var testMethod = getMethod("method1");
		var source = MethodSource.from(testMethod);

		assertThat(source.getClassName()).isEqualTo(getClass().getName());
		assertThat(source.getMethodName()).isEqualTo(testMethod.getName());
		assertThat(source.getMethodParameterTypes()).isEqualTo(String.class.getName());
		assertThat(source.getJavaClass()).isEqualTo(getClass());
		assertThat(source.getJavaMethod()).isEqualTo(testMethod);
	}

	@Test
	void equalsAndHashCodeForMethodSource() throws Exception {
		var method1 = getMethod("method1");
		var method2 = getMethod("method2");
		assertEqualsAndHashCode(MethodSource.from(method1), MethodSource.from(method1), MethodSource.from(method2));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void instantiatingWithNullNamesShouldThrowPreconditionViolationException() {
		assertPreconditionViolationFor(() -> MethodSource.from("foo", null));
		assertPreconditionViolationFor(() -> MethodSource.from(null, "foo"));
	}

	@Test
	void instantiatingWithEmptyNamesShouldThrowPreconditionViolationException() {
		assertPreconditionViolationFor(() -> MethodSource.from("foo", ""));
		assertPreconditionViolationFor(() -> MethodSource.from("", "foo"));
	}

	@Test
	void instantiatingWithBlankNamesShouldThrowPreconditionViolationException() {
		assertPreconditionViolationFor(() -> MethodSource.from("foo", "  "));
		assertPreconditionViolationFor(() -> MethodSource.from("  ", "foo"));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void instantiationWithNullMethodShouldThrowPreconditionViolationException() {
		assertPreconditionViolationFor(() -> MethodSource.from(null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void instantiationWithNullClassOrMethodShouldThrowPreconditionViolationException() {
		assertPreconditionViolationFor(() -> MethodSource.from(null, String.class.getDeclaredMethod("getBytes")));
		assertPreconditionViolationFor(() -> MethodSource.from(String.class, null));
	}

	@Test
	void instantiationWithClassAndMethodShouldResultInACorrectObject() throws Exception {
		var source = MethodSource.from(String.class,
			String.class.getDeclaredMethod("lastIndexOf", String.class, int.class));
		assertEquals(String.class.getName(), source.getClassName());
		assertEquals("lastIndexOf", source.getMethodName());
		assertEquals("java.lang.String, int", source.getMethodParameterTypes());
	}

	@Test
	void instantiationWithClassAndMethodAsStringAndParamsAsClassVarargsShouldResultInACorrectObject() {
		var source = MethodSource.from(String.class.getName(), "lastIndexOf", String.class, int.class);
		assertEquals(String.class.getName(), source.getClassName());
		assertEquals("lastIndexOf", source.getMethodName());
		assertEquals("java.lang.String, int", source.getMethodParameterTypes());
	}

	@Test
	void twoEqualMethodsShouldHaveEqualMethodSourceObjects() {
		assertEquals(MethodSource.from("TestClass1", "testMethod1"), MethodSource.from("TestClass1", "testMethod1"));
	}

	@Test
	void twoUnequalMethodsShouldHaveUnequalMethodSourceObjects() {
		assertNotEquals(MethodSource.from("TestClass1", "testMethod1"), MethodSource.from("TestClass2", "testMethod1"));
	}

	@Test
	void twoUnequalMethodsInTheSameClassShouldHaveUnequalMethodSourceObjects() {
		assertNotEquals(MethodSource.from("TestClass1", "testMethod1"), MethodSource.from("TestClass1", "testMethod2"));
	}

	@Test
	void twoEqualMethodSourceObjectsShouldHaveEqualHashCodes() {
		assertEquals(MethodSource.from("TestClass1", "testMethod1").hashCode(),
			MethodSource.from("TestClass1", "testMethod1").hashCode());
	}

	@Test
	void twoEqualMethodsWithEqualParametersShouldHaveEqualMethodSourceObjects() {
		assertEquals(MethodSource.from("TestClass1", "testMethod1", "int, String"),
			MethodSource.from("TestClass1", "testMethod1", "int, String"));
	}

	@Test
	void twoUnequalMethodsWithEqualParametersShouldHaveUnequalMethodSourceObjects() {
		assertNotEquals(MethodSource.from("TestClass1", "testMethod1", "int, String"),
			MethodSource.from("TestClass1", "testMethod2", "int, String"));
	}

	@Test
	void twoEqualMethodsWithUnequalParametersShouldHaveUnequalMethodSourceObjects() {
		assertNotEquals(MethodSource.from("TestClass1", "testMethod1", "int, String"),
			MethodSource.from("TestClass1", "testMethod1", "float, int, String"));
	}

	@Test
	void twoEqualMethodsWithEqualParametersShouldHaveEqualMethodSourceHashCodes() {
		assertEquals(MethodSource.from("TestClass1", "testMethod1", "int, String").hashCode(),
			MethodSource.from("TestClass1", "testMethod1", "int, String").hashCode());
	}

	@Test
	void twoEqualMethodsWithUnequalParametersShouldHaveUnequalMethodSourceHashCodes() {
		assertNotEquals(MethodSource.from("TestClass1", "testMethod1", "int, String").hashCode(),
			MethodSource.from("TestClass1", "testMethod1", "float, int, String").hashCode());
	}

	@Test
	void aReflectedMethodsClassNameShouldBeConsistent() throws Exception {
		var m = String.class.getDeclaredMethod("valueOf", int.class);

		assertEquals("java.lang.String", MethodSource.from(m).getClassName());
	}

	@Test
	void aReflectedMethodsMethodNameShouldBeConsistent() throws Exception {
		var m = String.class.getDeclaredMethod("valueOf", int.class);

		assertEquals("valueOf", MethodSource.from(m).getMethodName());
	}

	@Test
	void aReflectedMethodsParameterTypesShouldBeConsistent() throws Exception {
		var m = String.class.getDeclaredMethod("valueOf", float.class);

		assertEquals("float", MethodSource.from(m).getMethodParameterTypes());
	}

	@Test
	void twoEqualReflectedMethodsShouldHaveEqualMethodSourceObjects() throws Exception {
		var m1 = String.class.getDeclaredMethod("valueOf", int.class);
		var m2 = String.class.getDeclaredMethod("valueOf", int.class);

		assertEquals(MethodSource.from(m1), MethodSource.from(m2));
	}

	@Test
	void twoEqualReflectedMethodsShouldHaveEqualMethodSourceHashCodes() throws Exception {
		var m1 = String.class.getDeclaredMethod("valueOf", int.class);
		var m2 = String.class.getDeclaredMethod("valueOf", int.class);

		assertEquals(MethodSource.from(m1).hashCode(), MethodSource.from(m2).hashCode());
	}

	@Test
	void twoUnequalReflectedMethodsShouldNotHaveEqualMethodSourceObjects() throws Exception {
		var m1 = String.class.getDeclaredMethod("valueOf", int.class);
		var m2 = Byte.class.getDeclaredMethod("byteValue");

		assertNotEquals(MethodSource.from(m1), MethodSource.from(m2));
	}

	@Test
	void twoUnequalReflectedMethodsShouldNotHaveEqualMethodSourceHashCodes() throws Exception {
		var m1 = String.class.getDeclaredMethod("valueOf", int.class);
		var m2 = Byte.class.getDeclaredMethod("byteValue");

		assertNotEquals(MethodSource.from(m1).hashCode(), MethodSource.from(m2).hashCode());
	}

	@Test
	void getJavaClassFromString() {
		var source = MethodSource.from(getClass().getName(), "method1");

		assertThat(source.getJavaClass()).isEqualTo(getClass());
	}

	@Test
	void getJavaClassShouldThrowExceptionIfClassNotFound() {
		var source = MethodSource.from(getClass().getName() + "X", "method1");

		assertPreconditionViolationFor(source::getJavaClass);
	}

	@Test
	void getJavaMethodShouldReturnGivenMethodIfOverloadExists() throws Exception {
		var testMethod = getMethod("method3");
		var source = MethodSource.from(testMethod);

		assertThat(source.getJavaMethod()).isEqualTo(testMethod);
	}

	@Test
	void getJavaMethodFromStringShouldFindVoidMethod() throws Exception {
		var testMethod = getClass().getDeclaredMethod("methodVoid");
		var source = MethodSource.from(getClass().getName(), testMethod.getName());

		assertThat(source.getJavaMethod()).isEqualTo(testMethod);
	}

	@Test
	void getJavaMethodFromStringShouldFindMethodWithParameter() throws Exception {
		var testMethod = getClass().getDeclaredMethod("method3", int.class);
		var source = MethodSource.from(getClass().getName(), testMethod.getName(), testMethod.getParameterTypes());

		assertThat(source.getJavaMethod()).isEqualTo(testMethod);
	}

	@Test
	void getJavaMethodFromStringShouldThrowExceptionIfParameterTypesAreNotSupplied() {
		var source = MethodSource.from(getClass().getName(), "method3");

		assertPreconditionViolationFor(source::getJavaMethod);
	}

	@Test
	void getJavaMethodFromStringShouldThrowExceptionIfParameterTypesDoNotMatch() {
		var source = MethodSource.from(getClass().getName(), "method3", double.class);

		assertPreconditionViolationFor(source::getJavaMethod);
	}

	@Test
	void getJavaMethodFromStringShouldThrowExceptionIfMethodDoesNotExist() {
		var source = MethodSource.from(getClass().getName(), "methodX");

		assertPreconditionViolationFor(source::getJavaMethod);
	}

	private Method getMethod(String name) throws Exception {
		return getClass().getDeclaredMethod(name, String.class);
	}

	@SuppressWarnings("unused")
	void method1(String text) {
	}

	@SuppressWarnings("unused")
	void method2(String text) {
	}

	@SuppressWarnings("unused")
	void method3(String text) {
	}

	@SuppressWarnings("unused")
	void method3(int number) {
	}

	@SuppressWarnings("unused")
	void methodVoid() {
	}

}
