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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ClassSelector}.
 *
 * @since 1.3
 * @see DiscoverySelectorsTests
 */
class ClassSelectorTests {

	@Test
	void equalsAndHashCode() {
		var selector1 = new ClassSelector(null, "org.example.TestClass");
		var selector2 = new ClassSelector(null, "org.example.TestClass");
		var selector3 = new ClassSelector(null, "org.example.X");

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

	@Test
	void preservesOriginalExceptionWhenTryingToLoadClass() {
		var selector = new ClassSelector(null, "org.example.TestClass");

		assertPreconditionViolationFor(selector::getJavaClass).withMessage(
			"Could not load class with name: org.example.TestClass").withCauseInstanceOf(ClassNotFoundException.class);
	}

	@Test
	void usesClassClassLoader() {
		var selector = new ClassSelector(getClass());

		assertThat(selector.getClassLoader()).isNotNull().isSameAs(getClass().getClassLoader());
	}

}
