/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrEmptyFor;

import org.junit.jupiter.api.Test;

/**
 * @since 1.0
 */
class ClassNameFilterTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void includeClassNamePatternsChecksPreconditions() {
		assertPreconditionViolationNotNullOrEmptyFor("patterns array",
			() -> ClassNameFilter.includeClassNamePatterns((String[]) null));
		assertPreconditionViolationNotNullOrEmptyFor("patterns array",
			() -> ClassNameFilter.includeClassNamePatterns(new String[0]));
		assertPreconditionViolationFor(() -> ClassNameFilter.includeClassNamePatterns(new String[] { null }))//
				.withMessage("patterns array must not contain null elements");
	}

	@Test
	void includeClassNamePatternsWithSinglePattern() {
		var regex = "^java\\.lang\\..*";

		var filter = ClassNameFilter.includeClassNamePatterns(regex);

		assertThat(filter).hasToString(
			"IncludeClassNameFilter that includes class names that match one of the following regular expressions: '"
					+ regex + "'");

		assertTrue(filter.apply("java.lang.String").included());
		assertTrue(filter.toPredicate().test("java.lang.String"));
		assertThat(filter.apply("java.lang.String").getReason()).contains(
			"Class name [java.lang.String] matches included pattern: '" + regex + "'");

		assertFalse(filter.apply("java.time.Instant").included());
		assertFalse(filter.toPredicate().test("java.time.Instant"));
		assertThat(filter.apply("java.time.Instant").getReason()).contains(
			"Class name [java.time.Instant] does not match any included pattern: '" + regex + "'");
	}

	@Test
	void includeClassNamePatternsWithMultiplePatterns() {
		var firstRegex = "^java\\.lang\\..*";
		var secondRegex = "^java\\.util\\..*";

		var filter = ClassNameFilter.includeClassNamePatterns(firstRegex, secondRegex);

		assertThat(filter).hasToString(
			"IncludeClassNameFilter that includes class names that match one of the following regular expressions: '"
					+ firstRegex + "' OR '" + secondRegex + "'");

		assertTrue(filter.apply("java.lang.String").included());
		assertTrue(filter.toPredicate().test("java.lang.String"));
		assertThat(filter.apply("java.lang.String").getReason()).contains(
			"Class name [java.lang.String] matches included pattern: '" + firstRegex + "'");

		assertTrue(filter.apply("java.util.Collection").included());
		assertTrue(filter.toPredicate().test("java.util.Collection"));
		assertThat(filter.apply("java.util.Collection").getReason()).contains(
			"Class name [java.util.Collection] matches included pattern: '" + secondRegex + "'");

		assertFalse(filter.apply("java.time.Instant").included());
		assertFalse(filter.toPredicate().test("java.time.Instant"));
		assertThat(filter.apply("java.time.Instant").getReason()).contains(
			"Class name [java.time.Instant] does not match any included pattern: '" + firstRegex + "' OR '"
					+ secondRegex + "'");
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void excludeClassNamePatternsChecksPreconditions() {
		assertPreconditionViolationNotNullOrEmptyFor("patterns array",
			() -> ClassNameFilter.excludeClassNamePatterns((String[]) null));
		assertPreconditionViolationNotNullOrEmptyFor("patterns array",
			() -> ClassNameFilter.excludeClassNamePatterns(new String[0]));
		assertPreconditionViolationFor(() -> ClassNameFilter.excludeClassNamePatterns(new String[] { null }))//
				.withMessage("patterns array must not contain null elements");
	}

	@Test
	void excludeClassNamePatternsWithSinglePattern() {
		var regex = "^java\\.lang\\..*";

		var filter = ClassNameFilter.excludeClassNamePatterns(regex);

		assertThat(filter).hasToString(
			"ExcludeClassNameFilter that excludes class names that match one of the following regular expressions: '"
					+ regex + "'");

		assertTrue(filter.apply("java.lang.String").excluded());
		assertFalse(filter.toPredicate().test("java.lang.String"));

		assertThat(filter.apply("java.lang.String").getReason()).contains(
			"Class name [java.lang.String] matches excluded pattern: '" + regex + "'");

		assertTrue(filter.apply("java.time.Instant").included());
		assertTrue(filter.toPredicate().test("java.time.Instant"));
		assertThat(filter.apply("java.time.Instant").getReason()).contains(
			"Class name [java.time.Instant] does not match any excluded pattern: '" + regex + "'");
	}

	@Test
	void excludeClassNamePatternsWithMultiplePatterns() {
		var firstRegex = "^java\\.lang\\..*";
		var secondRegex = "^java\\.util\\..*";

		var filter = ClassNameFilter.excludeClassNamePatterns(firstRegex, secondRegex);

		assertThat(filter).hasToString(
			"ExcludeClassNameFilter that excludes class names that match one of the following regular expressions: '"
					+ firstRegex + "' OR '" + secondRegex + "'");

		assertTrue(filter.apply("java.lang.String").excluded());
		assertFalse(filter.toPredicate().test("java.lang.String"));
		assertThat(filter.apply("java.lang.String").getReason()).contains(
			"Class name [java.lang.String] matches excluded pattern: '" + firstRegex + "'");

		assertTrue(filter.apply("java.util.Collection").excluded());
		assertFalse(filter.toPredicate().test("java.util.Collection"));
		assertThat(filter.apply("java.util.Collection").getReason()).contains(
			"Class name [java.util.Collection] matches excluded pattern: '" + secondRegex + "'");

		assertFalse(filter.apply("java.time.Instant").excluded());
		assertTrue(filter.toPredicate().test("java.time.Instant"));
		assertThat(filter.apply("java.time.Instant").getReason()).contains(
			"Class name [java.time.Instant] does not match any excluded pattern: '" + firstRegex + "' OR '"
					+ secondRegex + "'");
	}

}
