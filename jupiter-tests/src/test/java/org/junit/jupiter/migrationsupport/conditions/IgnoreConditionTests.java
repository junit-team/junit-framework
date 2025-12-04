/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.conditions;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests for JUnit 4's {@link Ignore @Ignore} support in JUnit
 * Jupiter provided by the {@link IgnoreCondition}.
 *
 * @since 5.4
 * @see IgnoreAnnotationIntegrationTests
 */
@SuppressWarnings("removal")
class IgnoreConditionTests {

	@Test
	void ignoredTestClassWithDefaultMessage() {
		Class<?> testClass = IgnoredClassWithDefaultMessageTestCase.class;

		// @formatter:off
		executeTestsForClass(testClass).allEvents().assertEventsMatchExactly(
			event(engine(), started()),
			event(container(testClass), skippedWithReason(testClass + " is disabled via @org.junit.Ignore")),
			event(engine(), finishedSuccessfully())
		);
		// @formatter:on
	}

	@Test
	void ignoredTestClassWithCustomMessage() {
		Class<?> testClass = IgnoredClassWithCustomMessageTestCase.class;

		// @formatter:off
		executeTestsForClass(testClass).allEvents().assertEventsMatchExactly(
			event(engine(), started()),
			event(container(testClass), skippedWithReason("Ignored Class")),
			event(engine(), finishedSuccessfully())
		);
		// @formatter:on
	}

	@Test
	void ignoredAndNotIgnoredTestMethods() {
		EngineExecutionResults executionResults = executeTestsForClass(IgnoredMethodsTestCase.class);
		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		// executionResults.allEvents().debug();
		// executionResults.allEvents().debug(System.err);

		// containers.debug();

		// tests.debug(System.err);
		// tests.debug();
		// tests.skipped().debug();
		// tests.started().debug();
		// tests.succeeded().debug();

		// executionResults.allEvents().executions().debug();
		// containers.executions().debug();
		// tests.executions().debug();

		executionResults.allEvents().executions().assertThatExecutions().hasSize(5);
		containers.executions().assertThatExecutions().hasSize(2);
		tests.executions().assertThatExecutions().hasSize(3);

		// @formatter:off
		// tests.debug().assertEventsMatchExactly(
		tests.assertEventsMatchExactly(
			event(test("ignoredWithCustomMessage"), skippedWithReason("Ignored Method")),
			event(test("notIgnored"), started()),
			event(test("notIgnored"), finishedSuccessfully()),
			event(test("ignoredWithDefaultMessage"), skippedWithReason(
				reason -> reason.endsWith("ignoredWithDefaultMessage() is disabled via @org.junit.Ignore")))
		);
		// @formatter:on
	}

	private EngineExecutionResults executeTestsForClass(Class<?> testClass) {
		return EngineTestKit.execute("junit-jupiter", request().selectors(selectClass(testClass)).build());
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(IgnoreCondition.class)
	@Ignore
	static class IgnoredClassWithDefaultMessageTestCase {

		@Test
		void ignoredBecauseClassIsIgnored() {
			/* no-op */
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(IgnoreCondition.class)
	@Ignore("Ignored Class")
	static class IgnoredClassWithCustomMessageTestCase {

		@Test
		void ignoredBecauseClassIsIgnored() {
			/* no-op */
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(IgnoreCondition.class)
	static class IgnoredMethodsTestCase {

		@Test
		void notIgnored() {
			/* no-op */
		}

		@Test
		@Ignore
		void ignoredWithDefaultMessage() {
			fail("This method should have been disabled via @Ignore");
		}

		@Test
		@Ignore("Ignored Method")
		void ignoredWithCustomMessage() {
			fail("This method should have been disabled via @Ignore");
		}
	}

}
