/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.commons.util.ReflectionUtils.findMethod;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("TypeParameterExplicitlyExtendsObject")
class ReflectionUtilsWithGenericTypeHierarchiesTests {
	@Test
	@Disabled("Describes a new case that does not yet yield the expected result.")
	void findsMethodsIndependentlyFromOrderOfImplementationsOfInterfaces() {

		class AB implements InterfaceDouble, InterfaceGenericNumber<Number> {
		}

		class BA implements InterfaceGenericNumber<Number>, InterfaceDouble {
		}

		var methodAB = findMethod(AB.class, "foo", Double.class).orElseThrow();
		var methodBA = findMethod(BA.class, "foo", Double.class).orElseThrow();

		assertEquals(methodAB, methodBA);
	}

	@Test
	void findMoreSpecificMethodFromAbstractImplementationOverDefaultInterfaceMethod() {
		class A implements InterfaceGenericNumber<Long> {
			@Override
			public void foo(Long parameter) {
			}
		}

		var foo = findMethod(A.class, "foo", Long.class).orElseThrow();

		assertEquals(A.class, foo.getDeclaringClass());
	}

	@Test
	@Disabled("Describes a new case that does not yet yield the expected result.")
	void findMoreSpecificMethodFromOverriddenImplementationOfGenericInterfaceMethod() {
		class A implements InterfaceGenericNumber<Number> {
			@Override
			public void foo(Number parameter) {
			}
		}

		var foo = findMethod(A.class, "foo", Long.class).orElseThrow();

		assertEquals(A.class, foo.getDeclaringClass());
	}

	@Test
	@Disabled("Describes a new case that does not yet yield the expected result.")
	void findMoreSpecificMethodFromImplementationOverDefaultInterfaceMethodAndGenericClassExtension() {

		class AParent {
			@SuppressWarnings("unused")
			public void foo(Number parameter) {
			}
		}

		class A extends AParent implements InterfaceGenericNumber<Number> {
			@Override
			public void foo(Number parameter) {
			}
		}

		var foo = findMethod(A.class, "foo", Long.class).orElseThrow();

		assertEquals(A.class, foo.getDeclaringClass());
	}

	@Test
	@Disabled("Expected behaviour is not clear yet.")
	void unclearPrecedenceOfImplementationsInParentClassAndInterfaceDefault() {

		class AParent {
			public void foo(@SuppressWarnings("unused") Number parameter) {
			}
		}

		class A extends AParent implements InterfaceGenericNumber<Number> {
		}

		var foo = findMethod(A.class, "foo", Long.class).orElseThrow();

		// ????????
		assertEquals(A.class, foo.getDeclaringClass());
		assertEquals(AParent.class, foo.getDeclaringClass());
		assertEquals(InterfaceGenericNumber.class, foo.getDeclaringClass());
	}

	@Test
	@Disabled("Describes cases where current implementation returns unexpected value")
	public void findMethodWithMostSpecificParameterTypeInHierarchy() {
		// Searched Parameter Type is more specific
		assertSpecificFooMethodFound(ClassImplementingInterfaceWithInvertedHierarchy.class,
			InterfaceWithGenericNumberParameter.class, Double.class);
		assertSpecificFooMethodFound(ClassImplementingGenericInterfaceWithMoreSpecificMethod.class,
			ClassImplementingGenericInterfaceWithMoreSpecificMethod.class, Double.class);
		assertSpecificFooMethodFound(ClassImplementingGenericAndMoreSpecificInterface.class,
			InterfaceWithGenericNumberParameter.class, Double.class);
		assertSpecificFooMethodFound(ClassOverridingDefaultMethodAndImplementingMoreSpecificInterface.class,
			ClassOverridingDefaultMethodAndImplementingMoreSpecificInterface.class, Double.class);

		// Exact Type Match
		assertSpecificFooMethodFound(ClassImplementingGenericInterfaceWithMoreSpecificMethod.class,
			ClassImplementingGenericInterfaceWithMoreSpecificMethod.class, Number.class);
	}

	private void assertSpecificFooMethodFound(Class<?> classToSearchIn, Class<?> classWithMostSpecificMethod,
			Class<?> parameterType) {
		var foo = findMethod(classToSearchIn, "foo", parameterType).orElseThrow();
		assertDeclaringClass(foo, classWithMostSpecificMethod);
	}

	private void assertDeclaringClass(Method method, Class<?> expectedClass) {
		assertEquals(expectedClass, method.getDeclaringClass());
	}

	interface InterfaceDouble {
		default void foo(@SuppressWarnings("unused") Double parameter) {
		}
	}

	interface InterfaceGenericNumber<T extends Number> {
		default void foo(@SuppressWarnings("unused") T parameter) {
		}
	}

	public interface InterfaceWithGenericObjectParameter {
		default <T extends Object> void foo(@SuppressWarnings("unused") T a) {
		}
	}

	public interface InterfaceWithGenericNumberParameter {
		default <T extends Number> void foo(@SuppressWarnings("unused") T a) {
		}
	}

	public interface InterfaceExtendingNumberInterfaceWithGenericObjectMethod
			extends InterfaceWithGenericNumberParameter {
		default <T extends Object> void foo(@SuppressWarnings("unused") T a) {
		}
	}

	public static class ClassImplementingGenericInterfaceWithMoreSpecificMethod
			implements InterfaceWithGenericObjectParameter {
		public void foo(@SuppressWarnings("unused") Number a) {
		}
	}

	public static class ClassImplementingGenericAndMoreSpecificInterface
			implements InterfaceWithGenericObjectParameter, InterfaceWithGenericNumberParameter {
	}

	public static class ClassOverridingDefaultMethodAndImplementingMoreSpecificInterface
			implements InterfaceWithGenericObjectParameter, InterfaceWithGenericNumberParameter {

		@Override
		public <T> void foo(@SuppressWarnings("unused") T a) {
		}
	}

	public static class ClassImplementingInterfaceWithInvertedHierarchy
			implements InterfaceExtendingNumberInterfaceWithGenericObjectMethod {
	}
}
