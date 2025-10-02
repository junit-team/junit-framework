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
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NestedClassSelector}.
 *
 * @since 1.6
 * @see DiscoverySelectorsTests
 */
class NestedClassSelectorTests {

	@Test
	void equalsAndHashCode() {
		var selector1 = new NestedClassSelector(null, List.of("org.example.EnclosingTestClass"),
			"org.example.NestedTestClass");
		var selector2 = new NestedClassSelector(null, List.of("org.example.EnclosingTestClass"),
			"org.example.NestedTestClass");
		var selector3 = new NestedClassSelector(null, List.of("org.example.X"), "org.example.Y");

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

	@Test
	void preservesOriginalExceptionWhenTryingToLoadEnclosingClasses() {
		var selector = new NestedClassSelector(null, List.of("org.example.EnclosingTestClass"),
			"org.example.NestedTestClass");

		assertPreconditionViolationFor(selector::getEnclosingClasses).withMessage(
			"Could not load class with name: org.example.EnclosingTestClass").withCauseInstanceOf(
				ClassNotFoundException.class);
	}

	@Test
	void preservesOriginalExceptionWhenTryingToLoadNestedClass() {
		var selector = new NestedClassSelector(null, List.of("org.example.EnclosingTestClass"),
			"org.example.NestedTestClass");

		assertPreconditionViolationFor(selector::getNestedClass).withMessage(
			"Could not load class with name: org.example.NestedTestClass").withCauseInstanceOf(
				ClassNotFoundException.class);
	}

	@Test
	void usesClassClassLoader() {
		var selector = new NestedClassSelector(List.of(getClass()), NestedTestCase.class);

		assertThat(selector.getClassLoader()).isNotNull().isSameAs(getClass().getClassLoader());
	}

	@SuppressWarnings("InnerClassMayBeStatic")
	class NestedTestCase {
	}
}
