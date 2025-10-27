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

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.0
 */
class MethodFinder {

	// Pattern: [declaringClassName#]methodName(comma-separated list of parameter type names)
	private static final Pattern METHOD_PATTERN = Pattern.compile(
		"(?:(?<declaringClass>.+)#)?(?<method>.+)\\((?<parameters>.*)\\)");

	Optional<Method> findMethod(String methodSpecPart, Class<?> clazz) {
		Matcher matcher = METHOD_PATTERN.matcher(methodSpecPart);

		Preconditions.condition(matcher.matches(),
			() -> "Method [%s] does not match pattern [%s]".formatted(methodSpecPart, METHOD_PATTERN));

		Class<?> targetClass = clazz;
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
