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

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.reportEntry;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.platform.engine.CancellationToken;

class ExecutionCancellationTests extends AbstractJupiterTestEngineTests {

	@Test
	void canCancelExecutionWhileTestsAreRunning() {
		var testClass = TestCase.class;
		var cancellationToken = CancellationToken.create();

		TestCase.cancellationToken = cancellationToken;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(cancellationToken) //
				.execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("first"), started()), //
			event(test("first"), reportEntry(Map.of("cancelled", "true"))), //
			event(test("first"), finishedSuccessfully()), //
			event(test("second"), skippedWithReason("Test execution cancelled")), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@AfterEach
	void resetCancellationToken() {
		TestCase.cancellationToken = null;
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(OrderAnnotation.class)
	static class TestCase {

		static @Nullable CancellationToken cancellationToken;

		@Test
		@Order(1)
		void first() {
			requiredCancellationToken().cancel();
		}

		@AfterEach
		void afterEach(TestReporter reporter) {
			reporter.publishEntry("cancelled", String.valueOf(requiredCancellationToken().isCancellationRequested()));
		}

		@Test
		@Order(2)
		void second() {
			fail("should not be called");
		}

		private CancellationToken requiredCancellationToken() {
			return requireNonNull(cancellationToken);
		}
	}

}
