/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.tagexpression;

import java.util.Collection;

import org.junit.platform.engine.TestTag;

/**
 * @since 1.1
 */
class TagExpressions {

	static TagExpression tag(String tag) {
		TestTag testTag = TestTag.create(tag);
		return new TagExpression() {
			@Override
			public boolean evaluate(Collection<TestTag> tags) {
				return tags.contains(testTag);
			}

			@Override
			public String toString() {
				return testTag.getName();
			}
		};
	}

	static TagExpression none() {
		return new TagExpression() {
			@Override
			public boolean evaluate(Collection<TestTag> tags) {
				return tags.isEmpty();
			}

			@Override
			public String toString() {
				return "none()";
			}
		};
	}

	static TagExpression any() {
		return new TagExpression() {
			@Override
			public boolean evaluate(Collection<TestTag> tags) {
				return !tags.isEmpty();
			}

			@Override
			public String toString() {
				return "any()";
			}
		};
	}

	static TagExpression not(TagExpression toNegate) {
		return new TagExpression() {
			@Override
			public boolean evaluate(Collection<TestTag> tags) {
				return !toNegate.evaluate(tags);
			}

			@Override
			public String toString() {
				return "!" + toNegate;
			}
		};
	}

	static TagExpression and(TagExpression lhs, TagExpression rhs) {
		return new TagExpression() {
			@Override
			public boolean evaluate(Collection<TestTag> tags) {
				return lhs.evaluate(tags) && rhs.evaluate(tags);
			}

			@Override
			public String toString() {
				return "(" + lhs + " & " + rhs + ")";
			}
		};
	}

	static TagExpression or(TagExpression lhs, TagExpression rhs) {
		return new TagExpression() {
			@Override
			public boolean evaluate(Collection<TestTag> tags) {
				return lhs.evaluate(tags) || rhs.evaluate(tags);
			}

			@Override
			public String toString() {
				return "(" + lhs + " | " + rhs + ")";
			}
		};
	}

	private TagExpressions() {
	}

}
