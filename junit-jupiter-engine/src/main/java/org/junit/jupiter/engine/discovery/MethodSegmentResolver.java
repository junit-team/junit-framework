/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import static org.junit.platform.commons.util.ReflectionUtils.isPackagePrivate;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.0
 */
class MethodSegmentResolver {

	// Pattern: [declaringClassName#]methodName(comma-separated list of parameter type names)
	private static final Pattern METHOD_PATTERN = Pattern.compile(
		"(?:(?<declaringClass>.+)#)?(?<method>.+)\\((?<parameters>.*)\\)");

	/**
	 * If the {@code method} is package-private and declared a class in a
	 * different package than {@code testClass}, the declaring class name is
	 * included in the method's unique ID segment. Otherwise, it only
	 * consists of the method name and its parameter types.
	 */
	String formatMethodSpecPart(Method method, Class<?> testClass) {
		var parameterTypes = ClassUtils.nullSafeToString(method.getParameterTypes());
		if (isPackagePrivate(method)
				&& !method.getDeclaringClass().getPackageName().equals(testClass.getPackageName())) {
			return "%s#%s(%s)".formatted(method.getDeclaringClass().getName(), method.getName(), parameterTypes);
		}
		return "%s(%s)".formatted(method.getName(), parameterTypes);
	}

	Optional<Method> findMethod(String methodSpecPart, Class<?> testClass) {
		Matcher matcher = METHOD_PATTERN.matcher(methodSpecPart);

		Preconditions.condition(matcher.matches(),
			() -> "Method [%s] does not match pattern [%s]".formatted(methodSpecPart, METHOD_PATTERN));

		Class<?> targetClass = testClass;
		String declaringClass = matcher.group("declaringClass");
		if (declaringClass != null) {
			targetClass = ReflectionUtils.tryToLoadClass(declaringClass).getNonNullOrThrow(
				cause -> new PreconditionViolationException(
					"Could not load declaring class with name: " + declaringClass, cause));
		}
		String methodName = matcher.group("method");
		String parameterTypeNames = matcher.group("parameters");
		return ReflectionSupport.findMethod(targetClass, methodName, parameterTypeNames);
	}

}
