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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

/**
 * Unit tests for {@link MethodSelector}.
 *
 * @since 1.3
 * @see DiscoverySelectorsTests
 */
class MethodSelectorTests {

	private static final String TEST_CASE_NAME = TestCase.class.getName();

	@Test
	void equalsAndHashCode() {
		var selector1 = new MethodSelector(null, TEST_CASE_NAME, "method", "int, boolean");
		var selector2 = new MethodSelector(null, TEST_CASE_NAME, "method", "int, boolean");
		var selector3 = new MethodSelector(TestCase.class, "method", "int, boolean");
		var selector4 = new MethodSelector(TestCase.class, "method", int.class, boolean.class);

		Stream.of(selector2, selector3, selector4).forEach(selector -> {
			assertEqualsAndHashCode(selector1, selector, new MethodSelector(null, TEST_CASE_NAME, "method", "int"));
			assertEqualsAndHashCode(selector1, selector, new MethodSelector(null, TEST_CASE_NAME, "method", ""));
			assertEqualsAndHashCode(selector1, selector, new MethodSelector(null, TEST_CASE_NAME, "X", "int, boolean"));
			assertEqualsAndHashCode(selector1, selector, new MethodSelector(null, TEST_CASE_NAME, "X", ""));
			assertEqualsAndHashCode(selector1, selector, new MethodSelector(null, "X", "method", "int, boolean"));
			assertEqualsAndHashCode(selector1, selector, new MethodSelector(null, "X", "method", ""));
		});
	}

	@Test
	void preservesOriginalExceptionWhenTryingToLoadJavaClass() {
		var selector = new MethodSelector(null, "org.example.BogusClass", "method", "int, boolean");

		assertThat(selector.getClassName()).isEqualTo("org.example.BogusClass");
		assertThat(selector.getMethodName()).isEqualTo("method");
		assertThat(selector.getParameterTypeNames()).isEqualTo("int, boolean");

		assertPreconditionViolationFor(selector::getJavaClass)//
				.withMessage("Could not load class with name: org.example.BogusClass")//
				.withCauseInstanceOf(ClassNotFoundException.class);
	}

	@Test
	void preservesOriginalExceptionWhenTryingToLoadClassForParameterType() {
		var selector = new MethodSelector(null, TEST_CASE_NAME, "method", "int[], org.example.Bogus");

		assertThat(selector.getClassName()).isEqualTo(TEST_CASE_NAME);
		assertThat(selector.getMethodName()).isEqualTo("method");
		assertThat(selector.getParameterTypeNames()).isEqualTo("int[], org.example.Bogus");

		assertThatExceptionOfType(JUnitException.class)//
				.isThrownBy(selector::getJavaMethod)//
				.withMessage("Failed to load parameter type [org.example.Bogus] for method [method] in class [%s].",
					TEST_CASE_NAME)//
				.withCauseInstanceOf(ClassNotFoundException.class);
	}

	@Test
	void usesClassClassLoader() {
		var selector = new MethodSelector(getClass(), "usesClassClassLoader", "");

		assertThat(selector.getClassLoader()).isNotNull().isSameAs(getClass().getClassLoader());
	}

	private static class TestCase {

		@SuppressWarnings("unused")
		void method(int num, boolean flag) {
		}
	}

}
