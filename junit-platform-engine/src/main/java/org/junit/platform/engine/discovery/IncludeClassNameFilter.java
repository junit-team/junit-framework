/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.included;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.platform.engine.FilterResult;

/**
 * {@link ClassNameFilter} that matches fully qualified class names against
 * patterns in the form of regular expressions.
 *
 * <p>If the fully qualified name of a class matches against at least one
 * pattern, the class will be included.
 *
 * @since 1.0
 */
class IncludeClassNameFilter extends AbstractClassNameFilter {

	IncludeClassNameFilter(String... patterns) {
		super(patterns);
	}

	@Override
	public FilterResult apply(String className) {
		return findMatchingPattern(className) //
				.map(pattern -> included(formatInclusionReason(className, pattern))) //
				.orElseGet(() -> excluded(formatExclusionReason(className)));
	}

	private String formatInclusionReason(String className, Pattern pattern) {
		return "Class name [%s] matches included pattern: '%s'".formatted(className, pattern);
	}

	private String formatExclusionReason(String className) {
		return "Class name [%s] does not match any included pattern: %s".formatted(className, this.patternDescription);
	}

	@Override
	public Predicate<String> toPredicate() {
		return className -> findMatchingPattern(className).isPresent();
	}

	@Override
	public String toString() {
		return "%s that includes class names that match one of the following regular expressions: %s".formatted(
			getClass().getSimpleName(), this.patternDescription);
	}

}
