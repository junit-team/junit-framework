/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Collection of utilities for creating filters based on class names.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.7
 */
@API(status = INTERNAL, since = "1.7")
public class ClassNamePatternFilterUtils {

	private ClassNamePatternFilterUtils() {
		/* no-op */
	}

	public static final String ALL_PATTERN = "*";

	public static final String BLANK = "";

	/**
	 * Create a {@link Predicate} that can be used to exclude (i.e., filter out)
	 * objects of type {@code T} whose fully qualified class names match any of
	 * the supplied patterns.
	 *
	 * @param patterns a comma-separated list of patterns
	 */
	public static <T> Predicate<T> excludeMatchingClasses(@Nullable String patterns) {
		return matchingClasses(patterns, object -> object.getClass().getName(), FilterType.EXCLUDE);
	}

	/**
	 * Create a {@link Predicate} that can be used to exclude (i.e., filter out)
	 * fully qualified class names matching any of the supplied patterns.
	 *
	 * @param patterns a comma-separated list of patterns
	 */
	public static Predicate<String> excludeMatchingClassNames(@Nullable String patterns) {
		return matchingClasses(patterns, Function.identity(), FilterType.EXCLUDE);
	}

	/**
	 * Create a {@link Predicate} that can be used to include (i.e., filter in)
	 * objects of type {@code T} whose fully qualified class names match any of
	 * the supplied patterns.
	 *
	 * @param patterns a comma-separated list of patterns
	 */
	public static <T> Predicate<T> includeMatchingClasses(@Nullable String patterns) {
		return matchingClasses(patterns, object -> object.getClass().getName(), FilterType.INCLUDE);
	}

	/**
	 * Create a {@link Predicate} that can be used to include (i.e., filter in)
	 * fully qualified class names matching any of the supplied patterns.
	 *
	 * @param patterns a comma-separated list of patterns
	 */
	public static Predicate<String> includeMatchingClassNames(@Nullable String patterns) {
		return matchingClasses(patterns, Function.identity(), FilterType.INCLUDE);
	}

	private enum FilterType {
		INCLUDE, EXCLUDE
	}

	private static <T> Predicate<T> matchingClasses(@Nullable String patterns, Function<T, String> classNameProvider,
			FilterType type) {
		// @formatter:off
		return Optional.ofNullable(patterns)
				.filter(StringUtils::isNotBlank)
				.map(String::strip)
				.map(trimmedPatterns -> createPredicateFromPatterns(trimmedPatterns, classNameProvider, type))
				.orElse(type == FilterType.EXCLUDE ? __ -> true : __ -> false);
		// @formatter:on
	}

	private static <T> Predicate<T> createPredicateFromPatterns(String patterns, Function<T, String> classNameProvider,
			FilterType type) {
		if (ALL_PATTERN.equals(patterns)) {
			return type == FilterType.INCLUDE ? __ -> true : __ -> false;
		}

		List<Pattern> patternList = convertToRegularExpressions(patterns);
		return object -> {
			boolean isMatchingAnyPattern = patternList.stream().anyMatch(
				pattern -> pattern.matcher(classNameProvider.apply(object)).matches());
			return (type == FilterType.INCLUDE) == isMatchingAnyPattern;
		};
	}

	private static List<Pattern> convertToRegularExpressions(String patterns) {
		// @formatter:off
		return Arrays.stream(patterns.split(","))
				.filter(StringUtils::isNotBlank)
				.map(String::strip)
				.map(ClassNamePatternFilterUtils::replaceRegExElements)
				.map(Pattern::compile)
				.toList();
		// @formatter:on
	}

	private static String replaceRegExElements(String pattern) {
		return Matcher.quoteReplacement(pattern)
				// Match "." against "." and "$" since users may declare a "." instead of a
				// "$" as the separator between classes and nested classes.
				.replace(".", "[.$]")
				// Convert our "*" wildcard into a proper RegEx pattern.
				.replace("*", ".+");
	}

}
