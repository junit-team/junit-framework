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
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.reportEntry;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;

import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.platform.engine.CancellationToken;

class ExecutionCancellationTests extends AbstractJupiterTestEngineTests {

	@BeforeEach
	void initializeCancellationToken() {
		TestCase.cancellationToken = CancellationToken.create();
	}

	@AfterEach
	void resetCancellationToken() {
		TestCase.cancellationToken = null;
	}

	@Test
	void canCancelExecutionWhileTestClassIsRunning() {
		var testClass = RegularTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(TestCase.requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(1).finished(1).skipped(1));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("first"), started()), //
			event(test("first"), reportEntry(Map.of("cancelled", "true"))), //
			event(test("first"), finishedSuccessfully()), //
			event(test("second"), skippedWithReason("Execution cancelled")), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void canCancelExecutionWhileDynamicTestsAreRunning() {
		var testClass = DynamicTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(TestCase.requiredCancellationToken()) //
				.execute();

		results.containerEvents().assertStatistics(stats -> stats.skipped(1));
		results.testEvents().assertStatistics(stats -> stats.started(1).finished(1).skipped(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container("testFactory"), started()), //
			event(dynamicTestRegistered("#1"), displayName("first")), //
			event(test("#1"), started()), //
			event(test("#1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("#2"), displayName("container")), //
			event(container("#2"), skippedWithReason("Execution cancelled")), //
			event(container("testFactory"), finishedSuccessfully()), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	static class TestCase {

		static @Nullable CancellationToken cancellationToken;

		static CancellationToken requiredCancellationToken() {
			return requireNonNull(cancellationToken);
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(OrderAnnotation.class)
	static class RegularTestCase extends TestCase {

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
	}

	static class DynamicTestCase extends TestCase {

		@TestFactory
		Stream<DynamicNode> testFactory() {
			return Stream.of( //
				dynamicTest("first", () -> requiredCancellationToken().cancel()), //
				dynamicContainer("container", Stream.of( //
					dynamicTest("second", () -> fail("should not be called")) //
				)) //
			);
		}
	}

}
