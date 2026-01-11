/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.FunctionUtils.where;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;

/**
 * Integration tests that verify proper handling of invalid configuration for
 * lifecycle methods in conjunction with the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class InvalidLifecycleMethodConfigurationTests extends AbstractJupiterTestEngineTests {

	@Test
	void executeValidTestCaseAlongsideTestCaseWithInvalidNonStaticBeforeAllDeclaration() {
		assertReportsError(TestCaseWithInvalidNonStaticBeforeAllMethod.class, BeforeAll.class);
	}

	@Test
	void executeValidTestCaseAlongsideTestCaseWithInvalidNonStaticAfterAllDeclaration() {
		assertReportsError(TestCaseWithInvalidNonStaticAfterAllMethod.class, AfterAll.class);
	}

	@Test
	void executeValidTestCaseAlongsideTestCaseWithInvalidStaticBeforeEachDeclaration() {
		assertReportsError(TestCaseWithInvalidStaticBeforeEachMethod.class, BeforeEach.class);
	}

	@Test
	void executeValidTestCaseAlongsideTestCaseWithInvalidStaticAfterEachDeclaration() {
		assertReportsError(TestCaseWithInvalidStaticAfterEachMethod.class, AfterEach.class);
	}

	private void assertReportsError(Class<?> invalidTestClass, Class<? extends Annotation> annotationType) {
		var results = discoverTestsForClass(invalidTestClass);

		assertThat(results.getDiscoveryIssues()) //
				.filteredOn(where(DiscoveryIssue::severity, isEqual(Severity.ERROR))) //
				.extracting(DiscoveryIssue::message) //
				.asString().contains("@%s method".formatted(annotationType.getSimpleName()));
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCaseWithInvalidNonStaticBeforeAllMethod {

		// must be static
		@SuppressWarnings("unused")
		@BeforeAll
		void beforeAll() {
		}

		@Test
		void test() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCaseWithInvalidNonStaticAfterAllMethod {

		// must be static
		@SuppressWarnings("unused")
		@AfterAll
		void afterAll() {
		}

		@Test
		void test() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCaseWithInvalidStaticBeforeEachMethod {

		// must NOT be static
		@SuppressWarnings("unused")
		@BeforeEach
		static void beforeEach() {
		}

		@Test
		void test() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCaseWithInvalidStaticAfterEachMethod {

		// must NOT be static
		@SuppressWarnings("unused")
		@AfterEach
		static void afterEach() {
		}

		@Test
		void test() {
		}
	}

}
