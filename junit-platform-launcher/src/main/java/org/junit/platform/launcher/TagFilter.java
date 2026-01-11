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

import static java.util.Arrays.asList;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.tagexpression.TagExpression;

/**
 * Factory methods for creating {@link PostDiscoveryFilter PostDiscoveryFilters}
 * based on <em>included</em> and <em>excluded</em> tags or tag expressions.
 *
 * <p>Tag expressions are boolean expressions with the following allowed
 * operators: {@code !} (not), {@code &} (and), and {@code |} (or). Parentheses
 * can be used to adjust for operator precedence. Please refer to the
 * <a href="https://docs.junit.org/current/running-tests/tags.html#expressions">JUnit User Guide</a>
 * for usage examples.
 *
 * <p>Please note that a tag name is a valid tag expression. Thus, wherever a tag
 * expression can be used, a single tag name can also be used.
 *
 * @since 1.0
 * @see #includeTags(String...)
 * @see #excludeTags(String...)
 * @see TestTag
 */
@API(status = STABLE, since = "1.0")
public final class TagFilter {

	private TagFilter() {
		/* no-op */
	}

	/**
	 * Create an <em>include</em> filter based on the supplied tag expressions.
	 *
	 * <p>Containers and tests will only be executed if their tags match at
	 * least one of the supplied <em>included</em> tag expressions.
	 *
	 * @param tagExpressions the included tag expressions; never {@code null} or
	 * empty
	 * @throws PreconditionViolationException if the supplied tag expressions
	 * array is {@code null} or empty, or if any individual tag expression is
	 * not syntactically valid
	 * @see #includeTags(List)
	 * @see TestTag#isValid(String)
	 */
	public static PostDiscoveryFilter includeTags(String... tagExpressions) throws PreconditionViolationException {
		Preconditions.notNull(tagExpressions, "array of tag expressions must not be null");
		return includeTags(asList(tagExpressions));
	}

	/**
	 * Create an <em>include</em> filter based on the supplied tag expressions.
	 *
	 * <p>Containers and tests will only be executed if their tags match at
	 * least one of the supplied <em>included</em> tag expressions.
	 *
	 * @param tagExpressions the included tag expressions; never {@code null} or
	 * empty
	 * @throws PreconditionViolationException if the supplied tag expressions
	 * array is {@code null} or empty, or if any individual tag expression is
	 * not syntactically valid
	 * @see #includeTags(String...)
	 * @see TestTag#isValid(String)
	 */
	public static PostDiscoveryFilter includeTags(List<String> tagExpressions) throws PreconditionViolationException {
		Preconditions.notEmpty(tagExpressions, "list of tag expressions must not be null or empty");
		return includeMatching(tagExpressions);
	}

	/**
	 * Create an <em>exclude</em> filter based on the supplied tag expressions.
	 *
	 * <p>Containers and tests will only be executed if their tags do
	 * <em>not</em> match any of the supplied <em>excluded</em> tag expressions.
	 *
	 * @param tagExpressions the excluded tag expressions; never {@code null} or
	 * empty
	 * @throws PreconditionViolationException if the supplied tag expressions
	 * array is {@code null} or empty, or if any individual tag expression is
	 * not syntactically valid
	 * @see #excludeTags(List)
	 * @see TestTag#isValid(String)
	 */
	public static PostDiscoveryFilter excludeTags(String... tagExpressions) throws PreconditionViolationException {
		Preconditions.notNull(tagExpressions, "array of tag expressions must not be null");
		return excludeTags(asList(tagExpressions));
	}

	/**
	 * Create an <em>exclude</em> filter based on the supplied tag expressions.
	 *
	 * <p>Containers and tests will only be executed if their tags do
	 * <em>not</em> match any of the supplied <em>excluded</em> tag expressions.
	 *
	 * @param tagExpressions the excluded tag expressions; never {@code null} or
	 * empty
	 * @throws PreconditionViolationException if the supplied tag expressions
	 * array is {@code null} or empty, or if any individual tag expression is
	 * not syntactically valid
	 * @see #excludeTags(String...)
	 * @see TestTag#isValid(String)
	 */
	public static PostDiscoveryFilter excludeTags(List<String> tagExpressions) throws PreconditionViolationException {
		Preconditions.notEmpty(tagExpressions, "list of tag expressions must not be null or empty");
		return excludeMatching(tagExpressions);
	}

	private static PostDiscoveryFilter includeMatching(List<String> tagExpressions) {
		Supplier<@Nullable String> inclusionReason = () -> inclusionReasonExpressionSatisfy(tagExpressions);
		Supplier<@Nullable String> exclusionReason = () -> exclusionReasonExpressionNotSatisfy(tagExpressions);
		List<TagExpression> parsedTagExpressions = parseAll(tagExpressions);
		return descriptor -> {
			Set<TestTag> tags = descriptor.getTags();
			boolean included = parsedTagExpressions.stream().anyMatch(expression -> expression.evaluate(tags));

			return FilterResult.includedIf(included, inclusionReason, exclusionReason);
		};
	}

	private static String inclusionReasonExpressionSatisfy(List<String> tagExpressions) {
		return "included because tags match expression(s): [%s]".formatted(formatToString(tagExpressions));
	}

	private static String exclusionReasonExpressionNotSatisfy(List<String> tagExpressions) {
		return "excluded because tags do not match tag expression(s): [%s]".formatted(formatToString(tagExpressions));
	}

	private static PostDiscoveryFilter excludeMatching(List<String> tagExpressions) {
		Supplier<@Nullable String> inclusionReason = () -> inclusionReasonExpressionNotSatisfy(tagExpressions);
		Supplier<@Nullable String> exclusionReason = () -> exclusionReasonExpressionSatisfy(tagExpressions);
		List<TagExpression> parsedTagExpressions = parseAll(tagExpressions);
		return descriptor -> {
			Set<TestTag> tags = descriptor.getTags();
			boolean included = parsedTagExpressions.stream().noneMatch(expression -> expression.evaluate(tags));

			return FilterResult.includedIf(included, inclusionReason, exclusionReason);
		};
	}

	private static String inclusionReasonExpressionNotSatisfy(List<String> tagExpressions) {
		return "included because tags do not match expression(s): [%s]".formatted(formatToString(tagExpressions));
	}

	private static String exclusionReasonExpressionSatisfy(List<String> tagExpressions) {
		return "excluded because tags match tag expression(s): [%s]".formatted(formatToString(tagExpressions));
	}

	private static String formatToString(List<String> tagExpressions) {
		return tagExpressions.stream().map(String::strip).sorted().collect(Collectors.joining(","));
	}

	private static List<TagExpression> parseAll(List<String> tagExpressions) {
		return tagExpressions.stream().map(TagFilter::parse).toList();
	}

	private static TagExpression parse(@Nullable String tagExpression) {
		return TagExpression.parseFrom(tagExpression).tagExpressionOrThrow(
			message -> new PreconditionViolationException(
				"Unable to parse tag expression \"" + tagExpression + "\": " + message));
	}

}
