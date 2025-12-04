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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests for {@link TestInstance @TestInstance} lifecycle
 * configuration support, not to be confused with {@link TestInstanceLifecycleTests}.
 *
 * <p>Specifically, this class tests custom lifecycle configuration specified
 * via {@code @TestInstance} as well as via {@link ConfigurationParameters}
 * supplied to the {@link Launcher} or via a JVM system property.
 *
 * @since 5.0
 * @see TestInstanceLifecycleTests
 */
class TestInstanceLifecycleConfigurationTests extends AbstractJupiterTestEngineTests {

	private static final String KEY = Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;

	private static final List<String> methodsInvoked = new ArrayList<>();

	@BeforeEach
	@AfterEach
	void reset() {
		methodsInvoked.clear();
		System.clearProperty(KEY);
	}

	@Test
	void instancePerMethodUsingStandardDefaultConfiguration() {
		performAssertions(AssumedInstancePerTestMethodTestCase.class, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaExplicitAnnotationDeclaration() {
		performAssertions(ExplicitInstancePerClassTestCase.class, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaSystemProperty() {
		Class<?> testClass = AssumedInstancePerClassTestCase.class;

		// Should fail by default...
		performAssertions(testClass, 1, 1, 0);

		// Should pass with the system property set
		System.setProperty(KEY, PER_CLASS.name());
		performAssertions(testClass, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaConfigParam() {
		Class<?> testClass = AssumedInstancePerClassTestCase.class;

		// Should fail by default...
		performAssertions(testClass, 1, 1, 0);

		// Should pass with the config param
		performAssertions(testClass, Map.of(KEY, PER_CLASS.name()), 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaConfigParamThatOverridesSystemProperty() {
		Class<?> testClass = AssumedInstancePerClassTestCase.class;

		// Should fail with system property
		System.setProperty(KEY, PER_METHOD.name());
		performAssertions(testClass, 1, 1, 0);

		// Should pass with the config param
		performAssertions(testClass, Map.of(KEY, PER_CLASS.name()), 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerMethodConfiguredViaExplicitAnnotationDeclarationThatOverridesSystemProperty() {
		System.setProperty(KEY, PER_CLASS.name());
		performAssertions(ExplicitInstancePerTestMethodTestCase.class, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerMethodConfiguredViaExplicitAnnotationDeclarationThatOverridesConfigParam() {
		Class<?> testClass = ExplicitInstancePerTestMethodTestCase.class;
		performAssertions(testClass, Map.of(KEY, PER_CLASS.name()), 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	private void performAssertions(Class<?> testClass, int containers, int containersFailed, int tests,
			String... methods) {

		performAssertions(testClass, Map.of(), containers, containersFailed, tests, methods);
	}

	private void performAssertions(Class<?> testClass, Map<String, String> configParams, int numContainers,
			int numFailedContainers, int numTests, String... methods) {

		// @formatter:off
		EngineExecutionResults executionResults = executeTests(
			request()
				.selectors(selectClass(testClass))
				.configurationParameters(configParams)
				.configurationParameter(DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME, "execution")
				.build()
		);
		// @formatter:on

		executionResults.containerEvents().assertStatistics(//
			stats -> stats.started(numContainers).finished(numContainers).failed(numFailedContainers));
		executionResults.testEvents().assertStatistics(//
			stats -> stats.started(numTests).finished(numTests));

		assertEquals(Arrays.asList(methods), methodsInvoked);
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestInstance(PER_METHOD)
	static class ExplicitInstancePerTestMethodTestCase {

		@BeforeAll
		static void beforeAll() {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@AfterAll
		static void afterAllStatic() {
			methodsInvoked.add("afterAll");
		}

	}

	/**
	 * "per-method" lifecycle mode is assumed since the {@code @BeforeAll} and
	 * {@code @AfterAll} methods are static, even though there is no explicit
	 * {@code @TestInstance} declaration.
	 */
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class AssumedInstancePerTestMethodTestCase {

		@BeforeAll
		static void beforeAll() {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@AfterAll
		static void afterAllStatic() {
			methodsInvoked.add("afterAll");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestInstance(PER_CLASS)
	static class ExplicitInstancePerClassTestCase {

		@BeforeAll
		void beforeAll(TestInfo testInfo) {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@AfterAll
		void afterAll(TestInfo testInfo) {
			methodsInvoked.add("afterAll");
		}

	}

	/**
	 * "per-class" lifecycle mode is assumed since the {@code @BeforeAll} and
	 * {@code @AfterAll} methods are non-static, even though there is no
	 * explicit {@code @TestInstance} declaration.
	 */
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class AssumedInstancePerClassTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@BeforeAll
		void beforeAll(TestInfo testInfo) {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@SuppressWarnings("JUnitMalformedDeclaration")
		@AfterAll
		void afterAll(TestInfo testInfo) {
			methodsInvoked.add("afterAll");
		}

	}

}
