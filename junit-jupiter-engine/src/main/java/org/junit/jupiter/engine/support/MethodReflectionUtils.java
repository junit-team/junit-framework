/*
 * Copyright 2015-2025 the original author or authors.
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
import static org.junit.platform.commons.util.KotlinReflectionUtils.invokeKotlinSuspendingFunction;
import static org.junit.platform.commons.util.KotlinReflectionUtils.isKotlinSuspendingFunction;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.ReflectionSupport;

@API(status = INTERNAL, since = "6.0")
public class MethodReflectionUtils {

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
		return ReflectionSupport.invokeMethod(method, target, arguments);
	}

	private MethodReflectionUtils() {
	}
}
