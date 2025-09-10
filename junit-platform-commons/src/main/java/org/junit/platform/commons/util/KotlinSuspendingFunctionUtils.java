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

import static kotlin.jvm.JvmClassMappingKt.getJavaClass;
import static kotlin.reflect.full.KCallables.callSuspendBy;
import static kotlin.reflect.jvm.KCallablesJvm.isAccessible;
import static kotlin.reflect.jvm.KCallablesJvm.setAccessible;
import static kotlin.reflect.jvm.KTypesJvm.getJvmErasure;
import static kotlin.reflect.jvm.ReflectJvmMapping.getJavaType;
import static kotlinx.coroutines.BuildersKt.runBlocking;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.commons.util.ReflectionUtils.EMPTY_CLASS_ARRAY;
import static org.junit.platform.commons.util.ReflectionUtils.getUnderlyingCause;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;

import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

class KotlinSuspendingFunctionUtils {

	static Class<?> getReturnType(Method method) {
		var returnType = getJavaClass(getJvmErasure(getKotlinFunction(method).getReturnType()));
		if (Unit.class.equals(returnType)) {
			return void.class;
		}
		return returnType;
	}

	static Type getGenericReturnType(Method method) {
		return getJavaType(getKotlinFunction(method).getReturnType());
	}

	static Parameter[] getParameters(Method method) {
		var parameterCount = method.getParameterCount();
		if (parameterCount == 1) {
			return new Parameter[0];
		}
		return Arrays.copyOf(method.getParameters(), parameterCount - 1);
	}

	static Class<?>[] getParameterTypes(Method method) {
		var parameterCount = method.getParameterCount();
		if (parameterCount == 1) {
			return EMPTY_CLASS_ARRAY;
		}
		return Arrays.stream(method.getParameterTypes()).limit(parameterCount - 1).toArray(Class<?>[]::new);
	}

	static @Nullable Object invoke(Method method, @Nullable Object target, @Nullable Object[] args) {
		try {
			return invoke(getKotlinFunction(method), target, args);
		}
		catch (InterruptedException e) {
			throw throwAsUncheckedException(e);
		}
	}

	private static <T> @Nullable T invoke(KFunction<T> function, @Nullable Object target, @Nullable Object[] args)
			throws InterruptedException {
		if (!isAccessible(function)) {
			setAccessible(function, true);
		}
		return runBlocking(EmptyCoroutineContext.INSTANCE, (__, continuation) -> {
			try {
				return callSuspendBy(function, toArgumentMap(target, args, function), continuation);
			}
			catch (Exception e) {
				throw throwAsUncheckedException(getUnderlyingCause(e));
			}
		});
	}

	private static Map<KParameter, @Nullable Object> toArgumentMap(@Nullable Object target, @Nullable Object[] args,
			KFunction<?> function) {
		Map<KParameter, @Nullable Object> arguments = new HashMap<>(args.length + 1);
		int index = 0;
		for (KParameter parameter : function.getParameters()) {
			switch (parameter.getKind()) {
				case INSTANCE -> arguments.put(parameter, target);
				case VALUE, EXTENSION_RECEIVER -> {
					arguments.put(parameter, args[index]);
					index++;
				}
				default -> throw new JUnitException("Unsupported parameter kind: " + parameter.getKind());
			}
		}
		return arguments;
	}

	private static KFunction<?> getKotlinFunction(Method method) {
		return Preconditions.notNull(ReflectJvmMapping.getKotlinFunction(method),
			() -> "Failed to get Kotlin function for method: " + method);
	}

	private KotlinSuspendingFunctionUtils() {
	}
}
