/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.engine.Constants.DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.util.SetSystemProperty;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.extension.sub.AlwaysDisabledCondition;
import org.junit.jupiter.engine.extension.sub.AnotherAlwaysDisabledCondition;
import org.junit.jupiter.engine.extension.sub.SystemPropertyCondition;
import org.junit.jupiter.engine.extension.sub.SystemPropertyCondition.SystemProperty;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests that verify support for the {@link ExecutionCondition}
 * extension point in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
@SetSystemProperty(key = ExecutionConditionTests.FOO, value = ExecutionConditionTests.BAR)
class ExecutionConditionTests extends AbstractJupiterTestEngineTests {

	static final String FOO = "DisabledTests.foo";
	static final String BAR = "DisabledTests.bar";
	private static final String BOGUS = "DisabledTests.bogus";
	private static final String DEACTIVATE = "*AnotherAlwaysDisable*, org.junit.jupiter.engine.extension.sub.AlwaysDisable*";

	@Test
	void conditionWorksOnContainer() {
		EngineExecutionResults executionResults = executeTestsForClass(TestCaseWithExecutionConditionOnClass.class);

		executionResults.containerEvents().assertStatistics(stats -> stats.skipped(1));
		executionResults.testEvents().assertStatistics(stats -> stats.started(0));
	}

	@Test
	void conditionWorksOnTest() {
		Events tests = executeTestsForClass(TestCaseWithExecutionConditionOnMethods.class).testEvents();

		tests.assertStatistics(stats -> stats.started(2).succeeded(2).skipped(3));
	}

	@Test
	void overrideConditionsUsingFullyQualifiedClassName() {
		String deactivatePattern = SystemPropertyCondition.class.getName() + "," + DEACTIVATE;
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	@Test
	void overrideConditionsUsingStar() {
		// "*" should deactivate DisabledCondition and SystemPropertyCondition
		String deactivatePattern = "*";
		assertExecutionConditionOverride(deactivatePattern, 2, 2);
		assertExecutionConditionOverride(deactivatePattern, 5, 2, 3);
	}

	@Test
	void overrideConditionsUsingStarPlusSimpleClassName() {
		// DisabledCondition should remain activated
		String deactivatePattern = "*" + SystemPropertyCondition.class.getSimpleName() + ", " + DEACTIVATE;
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	@Test
	void overrideConditionsUsingPackageNamePlusDotStar() {
		// DisabledCondition should remain activated
		String deactivatePattern = DEACTIVATE + ", " + SystemPropertyCondition.class.getPackage().getName() + ".*";
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	@Test
	void overrideConditionsUsingMultipleWildcards() {
		// DisabledCondition should remain activated
		String deactivatePattern = "org.junit.jupiter.*.System*Condition" + "," + DEACTIVATE;
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	@Test
	void deactivateAllConditions() {
		// DisabledCondition should remain activated
		String deactivatePattern = "org.junit.jupiter.*.System*Condition" + ", " + DEACTIVATE;
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	private void assertExecutionConditionOverride(String deactivatePattern, int testStartedCount, int testFailedCount) {
		// @formatter:off
		LauncherDiscoveryRequest request = request()
				.selectors(selectClass(TestCaseWithExecutionConditionOnClass.class))
				.configurationParameter(DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME, deactivatePattern)
				.build();
		// @formatter:on

		EngineExecutionResults executionResults = executeTests(request);
		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		containers.assertStatistics(stats -> stats.skipped(0).started(2));
		tests.assertStatistics(stats -> stats.started(testStartedCount).failed(testFailedCount));
	}

	private void assertExecutionConditionOverride(String deactivatePattern, int started, int succeeded, int failed) {
		// @formatter:off
		LauncherDiscoveryRequest request = request()
				.selectors(selectClass(TestCaseWithExecutionConditionOnMethods.class))
				.configurationParameter(DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME, deactivatePattern)
				.build();
		// @formatter:on

		executeTests(request).testEvents().assertStatistics(
			stats -> stats.started(started).succeeded(succeeded).failed(failed));
	}

	// -------------------------------------------------------------------

	@SystemProperty(key = FOO, value = BOGUS)
	@DeactivatedConditions
	static class TestCaseWithExecutionConditionOnClass {

		@Test
		void disabledTest() {
			fail("this should be disabled");
		}

		@Test
		@Disabled
		void atDisabledTest() {
			fail("this should be @Disabled");
		}
	}

	static class TestCaseWithExecutionConditionOnMethods {

		@Test
		void enabledTest() {
		}

		@Test
		@Disabled
		@DeactivatedConditions
		void atDisabledTest() {
			fail("this should be @Disabled");
		}

		@Test
		@SystemProperty(key = FOO, value = BAR)
		void systemPropertyEnabledTest() {
		}

		@Test
		@DeactivatedConditions
		@SystemProperty(key = FOO, value = BOGUS)
		void systemPropertyWithIncorrectValueTest() {
			fail("this should be disabled");
		}

		@Test
		@DeactivatedConditions
		@SystemProperty(key = BOGUS, value = "doesn't matter")
		void systemPropertyNotSetTest() {
			fail("this should be disabled");
		}

	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith({ AlwaysDisabledCondition.class, AnotherAlwaysDisabledCondition.class })
	@interface DeactivatedConditions {
	}

}
