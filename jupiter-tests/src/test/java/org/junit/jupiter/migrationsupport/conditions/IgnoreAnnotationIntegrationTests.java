/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.conditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.EnableJUnit4MigrationSupport;

/**
 * Empirical integration tests for JUnit 4's {@link Ignore @Ignore} support in
 * JUnit Jupiter, covering the {@link IgnoreCondition} and
 * {@link EnableJUnit4MigrationSupport @EnableJUnit4MigrationSupport}.
 *
 * @since 5.4
 * @see IgnoreConditionTests
 */
@SuppressWarnings("removal")
class IgnoreAnnotationIntegrationTests {

	@Nested
	@ExtendWith(IgnoreCondition.class)
	class ExplicitIgnoreConditionRegistration extends BaseNestedTestCase {
	}

	@Nested
	@EnableJUnit4MigrationSupport
	class ImplicitIgnoreConditionRegistration extends BaseNestedTestCase {
	}

	@TestInstance(PER_CLASS)
	private static abstract class BaseNestedTestCase {

		private static List<String> tests = new ArrayList<>();

		@BeforeAll
		void clearTracking() {
			tests.clear();
		}

		@AfterAll
		void verifyTracking() {
			assertThat(tests).containsExactly("notIgnored");
		}

		@BeforeEach
		void track(TestInfo testInfo) {
			tests.add(testInfo.getTestMethod().get().getName());
		}

		@Test
		@Ignore
		void ignored() {
			fail("This method should have been disabled via @Ignore");
		}

		@Test
		// @Ignore
		void notIgnored() {
			/* no-op */
		}

	}

}
