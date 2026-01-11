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

import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.included;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;

/**
 * {@link MethodFilter} that matches fully qualified method names against
 * patterns in the form of regular expressions.
 *
 * <p>If the fully qualified name of a method matches against at least one
 * pattern, the method will be included.
 *
 * @since 1.12
 */
class IncludeMethodFilter extends AbstractMethodFilter {

	IncludeMethodFilter(String... patterns) {
		super(patterns);
	}

	@Override
	public FilterResult apply(TestDescriptor descriptor) {
		String methodName = getFullyQualifiedMethodNameFromDescriptor(descriptor);
		return findMatchingPattern(methodName) //
				.map(pattern -> included(formatInclusionReason(methodName, pattern))) //
				.orElseGet(() -> excluded(formatExclusionReason(methodName)));
	}

	private String formatInclusionReason(@Nullable String methodName, Pattern pattern) {
		return "Method name [%s] matches included pattern: '%s'".formatted(methodName, pattern);
	}

	private String formatExclusionReason(@Nullable String methodName) {
		return "Method name [%s] does not match any included pattern: %s".formatted(methodName, patternDescription);
	}

	@Override
	public String toString() {
		return "%s that includes method names that match one of the following regular expressions: %s".formatted(
			getClass().getSimpleName(), patternDescription);
	}
}
