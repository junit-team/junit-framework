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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.TestMethodReturnValueHandler;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.KotlinReflectionUtils;

@API(status = INTERNAL, since = "6.0")
public class MethodReflectionUtils {

	private static List<TestMethodReturnValueHandler> getReturnValueHandlers() {
		return ServiceLoader //
				.load(TestMethodReturnValueHandler.class, ClassLoaderUtils.getDefaultClassLoader()) //
				.stream() //
				.map(ServiceLoader.Provider::get) //
				.toList();
	}

	public static boolean hasReturnValueHandler(Method method) {
		Class<?> returnType = method.getReturnType();
		return returnType != void.class
				&& getReturnValueHandlers().stream().anyMatch(h -> h.supportsReturnType(returnType));
	}

	public static @Nullable TestMethodReturnValueHandler findReturnValueHandler(Method method) {
		Class<?> returnType = method.getReturnType();
		return getReturnValueHandlers().stream() //
				.filter(h -> h.supportsReturnType(returnType)) //
				.findFirst() //
				.orElse(null);
	}

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
		if (isKotlinType(method.getDeclaringClass()) && KotlinReflectionUtils.isKotlinReflectPresent()
				&& hasInlineTypeArgument(arguments)) {
			return invokeKotlinFunction(method, target, arguments);
		}
		return ReflectionSupport.invokeMethod(method, target, arguments);
	}

	private static boolean hasInlineTypeArgument(@Nullable Object[] arguments) {
		if (!KotlinReflectionUtils.isKotlinReflectPresent()) {
			return false;
		}

		return arguments.length > 0 && Arrays.stream(arguments).anyMatch(KotlinReflectionUtils::isInstanceOfInlineType);
	}

	private MethodReflectionUtils() {
	}
}
