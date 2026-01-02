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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests for support of overloaded test methods in conjunction with
 * the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class OverloadedTestMethodTests extends AbstractJupiterTestEngineTests {

	@Test
	void executeTestCaseWithOverloadedMethodsAndThenRerunOnlyOneOfTheMethodsSelectedByUniqueId() {
		Events tests = executeTestsForClass(TestCase.class).testEvents();

		tests.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

		Optional<Event> first = tests.succeeded().filter(
			event -> event.getTestDescriptor().getUniqueId().toString().contains(TestInfo.class.getName())).findFirst();
		assertTrue(first.isPresent());
		TestIdentifier testIdentifier = TestIdentifier.from(first.get().getTestDescriptor());
		UniqueId uniqueId = testIdentifier.getUniqueIdObject();

		tests = executeTests(selectUniqueId(uniqueId)).testEvents();

		tests.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		first = tests.succeeded().filter(
			event -> event.getTestDescriptor().getUniqueId().toString().contains(TestInfo.class.getName())).findFirst();
		assertTrue(first.isPresent());
	}

	@Test
	void executeTestCaseWithOverloadedMethodsWithSingleMethodThatAcceptsArgumentsSelectedByFullyQualifiedMethodName() {
		String fqmn = TestCase.class.getName() + "#test(" + TestInfo.class.getName() + ")";
		Events tests = executeTests(selectMethod(fqmn)).testEvents();

		tests.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		Optional<Event> first = tests.succeeded().stream().filter(
			event -> event.getTestDescriptor().getUniqueId().toString().contains(TestInfo.class.getName())).findFirst();
		assertTrue(first.isPresent());
	}

	static class TestCase {

		@Test
		void test() {
		}

		@Test
		void test(TestInfo testInfo) {
		}

	}

}
