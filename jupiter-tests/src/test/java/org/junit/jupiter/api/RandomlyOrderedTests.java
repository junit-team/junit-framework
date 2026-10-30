/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Constants.DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

/**
 * @since 5.8
 */
class RandomlyOrderedTests {

	private static final Set<String> callSequence = Collections.synchronizedSet(new LinkedHashSet<>());

	@Test
	void randomSeedForClassAndMethodOrderingIsDeterministic(@TrackLogRecords LogRecordListener listener) {
		IntStream.range(0, 20).forEach(i -> {
			callSequence.clear();
			listener.clear();
			var tests = executeTests(1618034);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));
			assertThat(callSequence).containsExactlyInAnyOrder("B_TestCase#b", "B_TestCase#c", "B_TestCase#a",
				"C_TestCase#b", "C_TestCase#c", "C_TestCase#a", "A_TestCase#b", "A_TestCase#c", "A_TestCase#a");
			assertThat(listener.stream(Level.CONFIG).map(LogRecord::getMessage)) //
					.filteredOn(message -> message.startsWith("Using seed [1618034]")) //
					.hasSize(1);
		});
	}

	@Test
	void usedSeedIsLoggedOncePerLauncherSession(@TrackLogRecords LogRecordListener listener) {
		var config = LauncherConfig.builder() //
				.enableTestEngineAutoRegistration(false) //
				.addTestEngines(new JupiterTestEngine()) //
				.build();

		try (var session = LauncherFactory.openSession(config)) {
			var launcher = session.getLauncher();
			launcher.discover(discoveryRequest(null));
			launcher.discover(discoveryRequest(null));
			launcher.discover(discoveryRequest("42"));
			launcher.discover(discoveryRequest("42"));
		}

		assertThat(listener.stream(Level.CONFIG).map(LogRecord::getMessage)) //
				.filteredOn(message -> message.startsWith("Using seed [")) //
				.satisfiesExactly( //
					message -> assertThat(message).startsWith(
						"Using seed [" + RandomOrdererUtils.DEFAULT_SEED + "] for ordering classes/methods."), //
					message -> assertThat(message).startsWith("Using seed [42] for ordering classes/methods."));
	}

	private static LauncherDiscoveryRequest discoveryRequest(@Nullable String randomSeed) {
		var builder = LauncherDiscoveryRequestBuilder.request() //
				.configurationParameter(DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME, ClassOrderer.Random.class.getName()) //
				.configurationParameter(DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME, MethodOrderer.Random.class.getName()) //
				.selectors(selectClasses(A_TestCase.class, B_TestCase.class, C_TestCase.class));
		if (randomSeed != null) {
			builder.configurationParameter(MethodOrderer.Random.RANDOM_SEED_PROPERTY_NAME, randomSeed);
		}
		return builder.build();
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
