/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

/**
 * Abstract {@link MethodFilter} that servers as a superclass
 * for filters including or excluding fully qualified method names
 * without parameters based on pattern-matching.
 *
 * @since 1.12
 */
abstract class AbstractMethodFilter implements MethodFilter {

	protected final List<Pattern> patterns;
	protected final String patternDescription;

	AbstractMethodFilter(String... patterns) {
		Preconditions.notEmpty(patterns, "patterns array must not be null or empty");
		Preconditions.containsNoNullElements(patterns, "patterns array must not contain null elements");
		this.patterns = Arrays.stream(patterns).map(Pattern::compile).toList();
		this.patternDescription = Arrays.stream(patterns).collect(joining("' OR '", "'", "'"));
	}

	protected Optional<Pattern> findMatchingPattern(@Nullable String methodName) {
		if (methodName == null) {
			return Optional.empty();
		}
		return this.patterns.stream().filter(pattern -> pattern.matcher(methodName).matches()).findAny();
	}

	protected @Nullable String getFullyQualifiedMethodNameFromDescriptor(TestDescriptor descriptor) {
		return descriptor.getSource() //
				.filter(MethodSource.class::isInstance) //
				.map(methodSource -> getFullyQualifiedMethodNameWithoutParameters(((MethodSource) methodSource))) //
				.orElse(null);
	}

	private String getFullyQualifiedMethodNameWithoutParameters(MethodSource methodSource) {
		String methodNameWithParentheses = ReflectionUtils.getFullyQualifiedMethodName(methodSource.getJavaClass(),
			methodSource.getMethodName(), (Class<?>[]) null);
		return methodNameWithParentheses.substring(0, methodNameWithParentheses.length() - 2);
	}
}
