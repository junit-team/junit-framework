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

import org.junit.Assume;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests that verify support for failed assumptions in the
 * {@link JupiterTestEngine}.
 *
 * @since 5.4
 */
class FailedAssumptionsTests extends AbstractJupiterTestEngineTests {

	@Test
	void testAbortedExceptionInBeforeAll() {
		EngineExecutionResults results = executeTestsForClass(TestAbortedExceptionInBeforeAllTestCase.class);

		results.containerEvents().assertStatistics(stats -> stats.aborted(1));
		results.testEvents().assertStatistics(stats -> stats.started(0));
	}

	@Test
	void assumptionViolatedExceptionInBeforeAll() {
		EngineExecutionResults results = executeTestsForClass(AssumptionViolatedExceptionInBeforeAllTestCase.class);

		results.containerEvents().assertStatistics(stats -> stats.aborted(1));
		results.testEvents().assertStatistics(stats -> stats.started(0));
	}

	// -------------------------------------------------------------------

	static class TestAbortedExceptionInBeforeAllTestCase {

		@BeforeAll
		static void beforeAll() {
			Assumptions.abort();
		}

		@Test
		void test() {
		}
	}

	static class AssumptionViolatedExceptionInBeforeAllTestCase {

		@SuppressWarnings("DataFlowIssue")
		@BeforeAll
		static void beforeAll() {
			Assume.assumeTrue(false);
		}

		@Test
		void test() {
		}
	}

}
