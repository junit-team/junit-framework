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
import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.included;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.FilterResult;

/**
 * {@link PackageNameFilter} that matches fully qualified package names that
 * are <em>not</em> prefixed by one of the package names provided to the filter.
 *
 * <p>If the fully qualified name of a package starts with at least one of the
 * packages names of the filter, the package will be excluded.
 *
 * @since 1.0
 */
class ExcludePackageNameFilter implements PackageNameFilter {

	private final List<String> packageNames;
	private final String patternDescription;

	ExcludePackageNameFilter(String... packageNames) {
		Preconditions.notEmpty(packageNames, "packageNames must not be null or empty");
		Preconditions.containsNoNullElements(packageNames, "packageNames must not contain null elements");
		this.packageNames = Arrays.asList(packageNames);
		this.patternDescription = Arrays.stream(packageNames).collect(joining("' OR '", "'", "'"));
	}

	@Override
	public FilterResult apply(String packageName) {
		return findMatchingName(packageName) //
				.map(matchedName -> excluded(formatExclusionReason(packageName, matchedName))) //
				.orElseGet(() -> included(formatInclusionReason(packageName)));
	}

	private String formatInclusionReason(String packageName) {
		return "Package name [%s] does not match any excluded names: %s".formatted(packageName,
			this.patternDescription);
	}

	private String formatExclusionReason(String packageName, String matchedName) {
		return "Package name [%s] matches excluded name: '%s'".formatted(packageName, matchedName);
	}

	@Override
	public Predicate<String> toPredicate() {
		return packageName -> findMatchingName(packageName).isEmpty();
	}

	private Optional<String> findMatchingName(String packageName) {
		return this.packageNames.stream().filter(
			name -> name.equals(packageName) || packageName.startsWith(name + ".")).findAny();
	}

	@Override
	public String toString() {
		return "%s that excludes packages whose names are either equal to or start with one of the following: %s".formatted(
			getClass().getSimpleName(), this.patternDescription);
	}

}
