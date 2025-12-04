/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * Unit tests for {@link ModifierSupport}.
 *
 * @since 1.4
 */
class ModifierSupportTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isPublicPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isPublic((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isPublic((Member) null));
	}

	@Classes
	void isPublicDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isPublic(clazz), ModifierSupport.isPublic(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isPublicDelegates(Method method) {
		assertEquals(ReflectionUtils.isPublic(method), ModifierSupport.isPublic(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isPrivatePreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isPrivate((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isPrivate((Member) null));
	}

	@Classes
	void isPrivateDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isPrivate(clazz), ModifierSupport.isPrivate(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isPrivateDelegates(Method method) {
		assertEquals(ReflectionUtils.isPrivate(method), ModifierSupport.isPrivate(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isNotPrivatePreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isNotPrivate((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isNotPrivate((Member) null));
	}

	@Classes
	void isNotPrivateDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isNotPrivate(clazz), ModifierSupport.isNotPrivate(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isNotPrivateDelegates(Method method) {
		assertEquals(ReflectionUtils.isNotPrivate(method), ModifierSupport.isNotPrivate(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isAbstractPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isAbstract((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isAbstract((Member) null));
	}

	@Classes
	void isAbstractDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isAbstract(clazz), ModifierSupport.isAbstract(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isAbstractDelegates(Method method) {
		assertEquals(ReflectionUtils.isAbstract(method), ModifierSupport.isAbstract(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isNotAbstractPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isNotAbstract((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isNotAbstract((Member) null));
	}

	@Classes
	void isNotAbstractDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isNotAbstract(clazz), ModifierSupport.isNotAbstract(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isNotAbstractDelegates(Method method) {
		assertEquals(ReflectionUtils.isNotAbstract(method), ModifierSupport.isNotAbstract(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isStaticPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isStatic((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isStatic((Member) null));
	}

	@Classes
	void isStaticDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isStatic(clazz), ModifierSupport.isStatic(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isStaticDelegates(Method method) {
		assertEquals(ReflectionUtils.isStatic(method), ModifierSupport.isStatic(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isNotStaticPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isNotStatic((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isNotStatic((Member) null));
	}

	@Classes
	void isNotStaticDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isNotStatic(clazz), ModifierSupport.isNotStatic(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isNotStaticDelegates(Method method) {
		assertEquals(ReflectionUtils.isNotStatic(method), ModifierSupport.isNotStatic(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isFinalPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isFinal((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isFinal((Member) null));
	}

	@Classes
	void isFinalDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isFinal(clazz), ModifierSupport.isFinal(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isFinalDelegates(Method method) {
		assertEquals(ReflectionUtils.isFinal(method), ModifierSupport.isFinal(method));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void isNotFinalPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ModifierSupport.isNotFinal((Class<?>) null));
		assertPreconditionViolationNotNullFor("Member", () -> ModifierSupport.isNotFinal((Member) null));
	}

	@Classes
	void isNotFinalDelegates(Class<?> clazz) {
		assertEquals(ReflectionUtils.isNotFinal(clazz), ModifierSupport.isNotFinal(clazz));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Methods
	void isNotFinalDelegates(Method method) {
		assertEquals(ReflectionUtils.isNotFinal(method), ModifierSupport.isNotFinal(method));
	}

	// -------------------------------------------------------------------------

	// Intentionally non-static
	public class PublicClass {

		public void publicMethod() {
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

	abstract static class AbstractClass {

		abstract void abstractMethod();
	}

	static class StaticClass {

		static void staticMethod() {
		}
	}

	final class FinalClass {

		@SuppressWarnings({ "FinalMethodInFinalClass", "RedundantModifier" })
		final void finalMethod() {
		}
	}

	// -------------------------------------------------------------------------

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@ValueSource(classes = { PublicClass.class, PrivateClass.class, ProtectedClass.class, PackageVisibleClass.class,
			AbstractClass.class, StaticClass.class, FinalClass.class })
	@interface Classes {
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@MethodSource("methods")
	@interface Methods {
	}

	static Stream<Method> methods() throws Exception {
		// @formatter:off
		return Stream.of(
			PublicClass.class.getMethod("publicMethod"),
			PrivateClass.class.getDeclaredMethod("privateMethod"),
			ProtectedClass.class.getDeclaredMethod("protectedMethod"),
			PackageVisibleClass.class.getDeclaredMethod("packageVisibleMethod"),
			AbstractClass.class.getDeclaredMethod("abstractMethod"),
			StaticClass.class.getDeclaredMethod("staticMethod"),
			FinalClass.class.getDeclaredMethod("finalMethod")
		);
		// @formatter:on
	}

}
