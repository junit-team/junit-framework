/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.tagexpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.engine.TestTag.create;
import static org.junit.platform.launcher.tagexpression.TagExpressions.and;
import static org.junit.platform.launcher.tagexpression.TagExpressions.any;
import static org.junit.platform.launcher.tagexpression.TagExpressions.none;
import static org.junit.platform.launcher.tagexpression.TagExpressions.not;
import static org.junit.platform.launcher.tagexpression.TagExpressions.or;
import static org.junit.platform.launcher.tagexpression.TagExpressions.tag;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestTag;

class TagExpressionsTests {

	private static final TagExpression True = tags -> true;
	private static final TagExpression False = tags -> false;

	@Test
	void tagIsJustATestTag() {
		assertThat(tag("foo")).hasToString("foo");
	}

	@Test
	void rejectInvalidTestTags() {
		assertPreconditionViolationFor(() -> tag("tags with spaces are not allowed")).withMessageContaining(
			"tags with spaces are not allowed");
	}

	@Test
	void tagEvaluation() {
		var tagExpression = tag("foo");

		assertThat(tagExpression.evaluate(Set.of(create("foo")))).isTrue();
		assertThat(tagExpression.evaluate(Set.of(create("not_foo")))).isFalse();
	}

	@Test
	void justConcatenateNot() {
		assertThat(not(tag("foo"))).hasToString("!foo");
		assertThat(not(and(tag("foo"), tag("bar")))).hasToString("!(foo & bar)");
		assertThat(not(or(tag("foo"), tag("bar")))).hasToString("!(foo | bar)");
	}

	@Test
	void notEvaluation() {
		assertThat(not(True).evaluate(Set.of())).isFalse();
		assertThat(not(False).evaluate(Set.of())).isTrue();
	}

	@Test
	void encloseAndWithParenthesis() {
		assertThat(and(tag("foo"), tag("bar"))).hasToString("(foo & bar)");
	}

	@Test
	void andEvaluation() {
		assertThat(and(True, True).evaluate(Set.of())).isTrue();
		assertThat(and(True, False).evaluate(Set.of())).isFalse();
		assertThat(and(False, onEvaluateThrow()).evaluate(Set.of())).isFalse();
	}

	@Test
	void encloseOrWithParenthesis() {
		assertThat(or(tag("foo"), tag("bar"))).hasToString("(foo | bar)");
	}

	@Test
	void orEvaluation() {
		assertThat(or(False, False).evaluate(Set.of())).isFalse();
		assertThat(or(True, onEvaluateThrow()).evaluate(Set.of())).isTrue();
		assertThat(or(False, True).evaluate(Set.of())).isTrue();
	}

	@Test
	void anyEvaluation() {
		assertThat(any().evaluate(Set.of())).isFalse();
		assertThat(any().evaluate(Set.of(TestTag.create("foo")))).isTrue();
	}

	@Test
	void noneEvaluation() {
		assertThat(none().evaluate(Set.of())).isTrue();
		assertThat(none().evaluate(Set.of(TestTag.create("foo")))).isFalse();
	}

	private TagExpression onEvaluateThrow() {
		return tags -> {
			throw new RuntimeException("should not be evaluated");
		};
	}

}
