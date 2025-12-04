/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

/**
 * @since 5.8
 */
class RandomlyOrderedTests {

	private static final Set<String> callSequence = Collections.synchronizedSet(new LinkedHashSet<>());

	@Test
	void randomSeedForClassAndMethodOrderingIsDeterministic() {
		IntStream.range(0, 20).forEach(i -> {
			callSequence.clear();
			var tests = executeTests(1618034);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));
			assertThat(callSequence).containsExactlyInAnyOrder("B_TestCase#b", "B_TestCase#c", "B_TestCase#a",
				"C_TestCase#b", "C_TestCase#c", "C_TestCase#a", "A_TestCase#b", "A_TestCase#c", "A_TestCase#a");
		});
	}

	private Events executeTests(@SuppressWarnings("SameParameterValue") long randomSeed) {
		// @formatter:off
		return EngineTestKit
				.engine("junit-jupiter")
				.configurationParameter(DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME, ClassOrderer.Random.class.getName())
				.configurationParameter(DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME, MethodOrderer.Random.class.getName())
				.configurationParameter(MethodOrderer.Random.RANDOM_SEED_PROPERTY_NAME, String.valueOf(randomSeed))
				.selectors(selectClasses(A_TestCase.class, B_TestCase.class, C_TestCase.class))
				.execute()
				.testEvents();
		// @formatter:on
	}

	abstract static class BaseTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			var testClass = testInfo.getTestClass().orElseThrow();
			var testMethod = testInfo.getTestMethod().orElseThrow();

			callSequence.add(testClass.getSimpleName() + "#" + testMethod.getName());
		}

		@Test
		void a() {
		}

		@Test
		void b() {
		}

		@Test
		void c() {
		}
	}

	@SuppressWarnings("NewClassNamingConvention")
	static class A_TestCase extends BaseTestCase {
	}

	@SuppressWarnings("NewClassNamingConvention")
	static class B_TestCase extends BaseTestCase {
	}

	@SuppressWarnings("NewClassNamingConvention")
	static class C_TestCase extends BaseTestCase {
	}

}
