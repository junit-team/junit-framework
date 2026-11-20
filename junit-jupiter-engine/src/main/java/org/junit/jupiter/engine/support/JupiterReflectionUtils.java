/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.support;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.KotlinReflectionUtils.getKotlinSuspendingFunctionGenericReturnType;
import static org.junit.platform.commons.util.KotlinReflectionUtils.getKotlinSuspendingFunctionReturnType;
import static org.junit.platform.commons.util.KotlinReflectionUtils.invokeKotlinFunction;
import static org.junit.platform.commons.util.KotlinReflectionUtils.invokeKotlinSuspendingFunction;
import static org.junit.platform.commons.util.KotlinReflectionUtils.isKotlinSuspendingFunction;
import static org.junit.platform.commons.util.KotlinReflectionUtils.isKotlinType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.KotlinReflectionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

@API(status = INTERNAL, since = "6.0")
public class JupiterReflectionUtils {

	public static Class<?> getReturnType(Method method) {
		return isKotlinSuspendingFunction(method) //
				? getKotlinSuspendingFunctionReturnType(method) //
				: method.getReturnType();
	}

	public static Type getGenericReturnType(Method method) {
		return isKotlinSuspendingFunction(method) //
				? getKotlinSuspendingFunctionGenericReturnType(method) //
				: method.getGenericReturnType();
	}

	public static @Nullable Object invoke(Method method, @Nullable Object target, @Nullable Object[] arguments) {
		if (isKotlinSuspendingFunction(method)) {
			return invokeKotlinSuspendingFunction(method, target, arguments);
		}
		if (isKotlinType(method.getDeclaringClass()) && hasInlineTypeArgument(arguments)) {
			return invokeKotlinFunction(method, target, arguments);
		}
		return ReflectionUtils.invokeMethod(method, target, arguments);
	}

	@API(status = INTERNAL, since = "6.2")
	public static <T> T newInstance(Constructor<T> constructor, @Nullable Object[] arguments) {
		var declaringClass = constructor.getDeclaringClass();
		if (isKotlinType(declaringClass) && hasInlineTypeArgument(arguments)) {
			var constructorWithKotlinConstructor = findConstructorWithKotlinConstructor(constructor);
			return KotlinReflectionUtils.newInstance(constructorWithKotlinConstructor, arguments);
		}
		return ReflectionUtils.newInstance(constructor, arguments);
	}

	private static boolean hasInlineTypeArgument(@Nullable Object[] arguments) {
		for (Object argument : arguments) {
			if (KotlinReflectionUtils.isInstanceOfInlineType(argument)) {
				return true;
			}
		}
		return false;
	}

	private static <T> Constructor<T> findConstructorWithKotlinConstructor(Constructor<T> constructor) {
		// Because the arguments include Kotlin inline value types we can't use
		// the Java constructor and must use KFunction.callBy instead. This
		// requires a Kotlin constructor.
		//
		// For ordinary constructors we can use kotlin-reflect to find the
		// Kotlin constructor matching the Java constructor. However, when
		// inline value types there is a single Kotlin constructor and two
		// Java constructors.
		//
		// Kotlin:
		// 0 = fun `<init>`(kotlin.UInt): ExampleTest
		// Java:
		// 0 =           private ExampleTest(int)
		// 1 = synthetic public ExampleTest(int,DefaultConstructorMarker)
		//
		// To further complicate things, private method doesn't have a matching
		// Kotlin function for use with KFunction.callBy. The synthetic method
		// is not usable because of the DefaultConstructorMarker.
		//
		// Additionally, because Kotlin reflect is an optional dependency the
		// implementation has to be written using java.lang.reflect types
		// rather than kotlin.reflect types.
		//
		// To resolve this we have to swap the private Java constructor we have
		// for the default constructor and then get its kotlin function for
		// KFunction.callBy.
		//
		// By noting that this method is only ever used with constructors
		// produced by ReflectionUtils.getDeclaredConstructor we know that
		// there is a single Kotlin constructor that takes at-least one
		// argument.
		//
		// So we look it up using kotlin-reflect. Then we find the matching
		// Java method for that. Because of the DefaultConstructorMarker we
		// can't use this method directly, but it will have a usable Kotlin
		// function. So we can pass that to KotlinReflectionUtils.newInstance
		// avoiding the use of kotlin.reflect types.
		var declaringClass = constructor.getDeclaringClass();
		return KotlinReflectionUtils.getKotlinConstructor(declaringClass);
	}

	@API(status = INTERNAL, since = "6.2")
	public static boolean isAssignableTo(@Nullable Object obj, Class<?> targetType) {
		Preconditions.notNull(targetType, "target type must not be null");
		return ReflectionUtils.isAssignableTo(obj, targetType) //
				|| KotlinReflectionUtils.isInlineTypeAssignableTo(obj, targetType);
	}

	private JupiterReflectionUtils() {
	}
}
