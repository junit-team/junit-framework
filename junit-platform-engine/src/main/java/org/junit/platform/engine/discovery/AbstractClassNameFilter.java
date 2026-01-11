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

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.platform.commons.util.Preconditions;

/**
 * Abstract {@link ClassNameFilter} that servers as a superclass
 * for filters including or excluding fully qualified class names
 * based on pattern-matching.
 *
 * @since 1.0
 */
abstract class AbstractClassNameFilter implements ClassNameFilter {

	protected final List<Pattern> patterns;
	protected final String patternDescription;

	AbstractClassNameFilter(String... patterns) {
		Preconditions.notEmpty(patterns, "patterns array must not be null or empty");
		Preconditions.containsNoNullElements(patterns, "patterns array must not contain null elements");
		this.patterns = Arrays.stream(patterns).map(Pattern::compile).toList();
		this.patternDescription = Arrays.stream(patterns).collect(joining("' OR '", "'", "'"));
	}

	@Override
	public abstract Predicate<String> toPredicate();

	protected Optional<Pattern> findMatchingPattern(String className) {
		return this.patterns.stream().filter(pattern -> pattern.matcher(className).matches()).findAny();
	}

}
