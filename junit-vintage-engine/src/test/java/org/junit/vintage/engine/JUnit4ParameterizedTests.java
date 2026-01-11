/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.vintage.engine.samples.junit4.JUnit4ParameterizedTestCase;

/**
 * @since 4.12
 */
class JUnit4ParameterizedTests {

	private final Map<TestExecutionResult.Status, Integer> callCounts = new HashMap<>();

	@Test
	void selectingWholeParameterizedClassRunsTestsWithAllValues() {
		executeTests(selectClass(JUnit4ParameterizedTestCase.class));

		Map<TestExecutionResult.Status, Integer> expectedCallCounts = new HashMap<>();
		expectedCallCounts.put(SUCCESSFUL, 3);
		expectedCallCounts.put(FAILED, 9);

		assertEquals(expectedCallCounts, callCounts);
	}

	@Test
	void selectingOneTestFromParameterizedClassRunsWithAllValues() {
		executeTests(selectMethod(JUnit4ParameterizedTestCase.class, "test1"));

		assertEquals(Map.of(FAILED, 3), callCounts);
	}

	private void executeTests(DiscoverySelector selector) {
		var launcher = LauncherFactory.create();
		launcher.registerTestExecutionListeners(new StatusTrackingListener());

		// @formatter:off
		launcher.execute(
			request()
				.selectors(selector)
				.filters(includeEngines("junit-vintage"))
				.enableImplicitConfigurationParameters(false)
				.forExecution()
				.build()
		);
		// @formatter:on
	}

	private class StatusTrackingListener implements TestExecutionListener {

		@Override
		public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
			if (identifier.isTest()) {
				callCounts.merge(result.getStatus(), 1, Integer::sum);
			}
		}
	}

}
