/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.test;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests that verify support for {@link Disabled @Disabled} in the
 * {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class DisabledTests extends AbstractJupiterTestEngineTests {

	@Test
	void executeTestsWithDisabledTestClass() {
		EngineExecutionResults results = executeTestsForClass(DisabledTestClassTestCase.class);

		results.containerEvents().assertStatistics(stats -> stats.skipped(1));
		results.testEvents().assertStatistics(stats -> stats.started(0));
	}

	@Test
	void executeTestsWithDisabledTestMethods() throws Exception {
		String methodName = "disabledTest";
		Method method = DisabledTestMethodsTestCase.class.getDeclaredMethod(methodName);

		executeTestsForClass(DisabledTestMethodsTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.skipped(1).started(1).finished(1).aborted(0).succeeded(1).failed(0))//
				.skipped().assertEventsMatchExactly(
					event(test(methodName), skippedWithReason(method + " is @Disabled")));
	}

	// -------------------------------------------------------------------

	@Disabled
	static class DisabledTestClassTestCase {

		@Test
		void disabledTest() {
			fail("this should be @Disabled");
		}
	}

	static class DisabledTestMethodsTestCase {

		@Test
		void enabledTest() {
		}

		@Test
		@Disabled
		void disabledTest() {
			fail("this should be @Disabled");
		}

	}

}
