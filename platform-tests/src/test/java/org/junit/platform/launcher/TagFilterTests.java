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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.launcher.TagFilter.excludeTags;
import static org.junit.platform.launcher.TagFilter.includeTags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.DemoClassTestDescriptor;

/**
 * Unit tests for {@link TagFilter}.
 *
 * <p>NOTE: part of the behavior of these tests regarding tags is
 * influenced by the implementation of {@link DemoClassTestDescriptor#getTags()}
 * rather than any concrete test engine.
 *
 * @since 1.0
 */
class TagFilterTests {

	private static final TestDescriptor classWithTag1 = classTestDescriptor("class1", ClassWithTag1.class);
	private static final TestDescriptor classWithTag1AndSurroundingWhitespace = classTestDescriptor(
		"class1-surrounding-whitespace", ClassWithTag1AndSurroundingWhitespace.class);
	private static final TestDescriptor classWithTag2 = classTestDescriptor("class2", ClassWithTag2.class);
	private static final TestDescriptor classWithBothTags = classTestDescriptor("class12", ClassWithBothTags.class);
	private static final TestDescriptor classWithDifferentTags = classTestDescriptor("classX",
		ClassWithDifferentTags.class);
	private static final TestDescriptor classWithNoTags = classTestDescriptor("class", ClassWithNoTags.class);

	@Test
	void includeTagsWithInvalidSyntax() {
		// @formatter:off
		assertAll(
			() -> assertSyntaxViolationForIncludes(null),
			() -> assertSyntaxViolationForIncludes(""),
			() -> assertSyntaxViolationForIncludes("   "),
			() -> assertSyntaxViolationForIncludes("foo bar")
		);
		// @formatter:on
	}

	@SuppressWarnings("DataFlowIssue")
	private void assertSyntaxViolationForIncludes(@Nullable String tag) {
		assertPreconditionViolationFor(() -> includeTags(tag))//
				.withMessageStartingWith("Unable to parse tag expression");
	}

	@Test
	void excludeTagsWithInvalidSyntax() {
		// @formatter:off
		assertAll(
			() -> assertSyntaxViolationForExcludes(null),
			() -> assertSyntaxViolationForExcludes(""),
			() -> assertSyntaxViolationForExcludes("   "),
			() -> assertSyntaxViolationForExcludes("foo bar")
		);
		// @formatter:on
	}

	@SuppressWarnings("DataFlowIssue")
	private void assertSyntaxViolationForExcludes(@Nullable String tag) {
		assertPreconditionViolationFor(() -> excludeTags(tag))//
				.withMessageStartingWith("Unable to parse tag expression");
	}

	@Test
	void includeSingleTag() {
		includeSingleTag(includeTags("tag1"));
	}

	@Test
	void includeSingleTagAndWhitespace() {
		includeSingleTag(includeTags("\t \n tag1  "));
	}

	@Test
	void includeMultipleTags() {
		var filter = includeTags("tag1", "  tag2  ");

		assertTrue(filter.apply(classWithBothTags).included());
		assertTrue(filter.apply(classWithTag1).included());
		assertTrue(filter.apply(classWithTag1AndSurroundingWhitespace).included());
		assertTrue(filter.apply(classWithTag2).included());

		assertTrue(filter.apply(classWithDifferentTags).excluded());
		assertTrue(filter.apply(classWithNoTags).excluded());
	}

	@Test
	void excludeSingleTag() {
		excludeSingleTag(excludeTags("tag1"));
	}

	@Test
	void excludeSingleTagAndWhitespace() {
		excludeSingleTag(excludeTags("\t \n tag1  "));
	}

	@Test
	void excludeMultipleTags() {
		var filter = excludeTags("tag1", "  tag2  ");

		var exclusionReason = "excluded because tags match tag expression(s): [tag1,tag2]";
		assertExcluded(filter.apply(classWithTag1), exclusionReason);
		assertExcluded(filter.apply(classWithTag1AndSurroundingWhitespace), exclusionReason);
		assertExcluded(filter.apply(classWithBothTags), exclusionReason);
		assertExcluded(filter.apply(classWithTag2), exclusionReason);

		var inclusionReason = "included because tags do not match expression(s): [tag1,tag2]";
		assertIncluded(filter.apply(classWithDifferentTags), inclusionReason);
		assertIncluded(filter.apply(classWithNoTags), inclusionReason);
	}

	@Test
	void rejectSingleUnparsableTagExpressions() {
		var brokenTagExpression = "tag & ";
		assertPreconditionViolationFor(() -> TagFilter.includeTags(brokenTagExpression))//
				.withMessageStartingWith("Unable to parse tag expression \"" + brokenTagExpression + "\"");
	}

	@Test
	void rejectUnparsableTagExpressionFromArray() {
		var brokenTagExpression = "tag & ";
		assertPreconditionViolationFor(() -> TagFilter.excludeTags(brokenTagExpression, "foo", "bar"))//
				.withMessageStartingWith("Unable to parse tag expression \"" + brokenTagExpression + "\"");
	}

	private void includeSingleTag(PostDiscoveryFilter filter) {
		var inclusionReason = "included because tags match expression(s): [tag1]";
		assertIncluded(filter.apply(classWithTag1), inclusionReason);
		assertIncluded(filter.apply(classWithTag1AndSurroundingWhitespace), inclusionReason);
		assertIncluded(filter.apply(classWithBothTags), inclusionReason);

		var exclusionReason = "excluded because tags do not match tag expression(s): [tag1]";
		assertExcluded(filter.apply(classWithTag2), exclusionReason);
		assertExcluded(filter.apply(classWithDifferentTags), exclusionReason);
		assertExcluded(filter.apply(classWithNoTags), exclusionReason);
	}

	private void excludeSingleTag(PostDiscoveryFilter filter) {
		var exclusionReason = "excluded because tags match tag expression(s): [tag1]";
		assertExcluded(filter.apply(classWithTag1), exclusionReason);
		assertExcluded(filter.apply(classWithTag1AndSurroundingWhitespace), exclusionReason);
		assertExcluded(filter.apply(classWithBothTags), exclusionReason);

		var inclusionReason = "included because tags do not match expression(s): [tag1]";
		assertIncluded(filter.apply(classWithTag2), inclusionReason);
		assertIncluded(filter.apply(classWithDifferentTags), inclusionReason);
		assertIncluded(filter.apply(classWithNoTags), inclusionReason);
	}

	private void assertIncluded(FilterResult filterResult, String expectedReason) {
		assertTrue(filterResult.included());
		assertThat(filterResult.getReason()).contains(expectedReason);
	}

	private void assertExcluded(FilterResult filterResult, String expectedReason) {
		assertTrue(filterResult.excluded());
		assertThat(filterResult.getReason()).contains(expectedReason);
	}

	// -------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	@Tag("tag1")
	private @interface Tag1 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Tag("tag2")
	private @interface Tag2 {
	}

	@Tag1
	private static class ClassWithTag1 {
	}

	@Tag("   tag1  \t    ")
	private static class ClassWithTag1AndSurroundingWhitespace {
	}

	@Tag2
	private static class ClassWithTag2 {
	}

	@Tag1
	@Tag2
	private static class ClassWithBothTags {
	}

	@Tag("foo")
	@Tag("bar")
	private static class ClassWithDifferentTags {
	}

	@Tag("   ") // intentionally "blank"
	private static class ClassWithNoTags {
	}

	private static TestDescriptor classTestDescriptor(String uniqueId, Class<?> testClass) {
		var rootUniqueId = UniqueId.root("class", uniqueId);
		return new DemoClassTestDescriptor(rootUniqueId, testClass);
	}

}
