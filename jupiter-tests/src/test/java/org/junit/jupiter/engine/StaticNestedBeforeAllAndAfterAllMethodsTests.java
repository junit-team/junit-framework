/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Integration tests that verify support for {@code static} {@link BeforeAll} and
 * {@link AfterAll} methods in {@link Nested} tests.
 *
 * @since 5.9
 * @see BeforeAllAndAfterAllComposedAnnotationTests
 */
class StaticNestedBeforeAllAndAfterAllMethodsTests extends AbstractJupiterTestEngineTests {

	private static final List<String> methodsInvoked = new ArrayList<>();

	@DisplayName("static @BeforeAll and @AfterAll methods in @Nested test class")
	@Test
	void staticBeforeAllAndAfterAllMethodsInNestedTestClass() {
		executeTestsForClass(TestCase.class).testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));

		assertThat(methodsInvoked).containsExactly(//
			"@BeforeAll: top-level", //
			"@Test: top-level", //
			"@BeforeAll: nested", //
			"@Test: nested", //
			"@AfterAll: nested", //
			"@AfterAll: top-level"//
		);
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase {

		@BeforeAll
		static void beforeAll() {
			methodsInvoked.add("@BeforeAll: top-level");
		}

		@Test
		void test() {
			methodsInvoked.add("@Test: top-level");
		}

		@AfterAll
		static void afterAll() {
			methodsInvoked.add("@AfterAll: top-level");
		}

		@Nested
		// Lifecycle.PER_METHOD is the default, but we declare it here in order
		// to be very explicit about what we are testing, namely static lifecycle
		// methods in an inner class WITHOUT Lifecycle.PER_CLASS semantics.
		@TestInstance(Lifecycle.PER_METHOD)
		class NestedTestCase {

			@BeforeAll
			static void beforeAllInner() {
				methodsInvoked.add("@BeforeAll: nested");
			}

			@Test
			void test() {
				methodsInvoked.add("@Test: nested");
			}

			@AfterAll
			static void afterAllInner() {
				methodsInvoked.add("@AfterAll: nested");
			}

		}

	}

}
