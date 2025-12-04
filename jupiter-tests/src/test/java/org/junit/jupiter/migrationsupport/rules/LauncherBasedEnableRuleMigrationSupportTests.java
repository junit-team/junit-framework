/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExternalResource;
import org.junit.rules.Verifier;

@SuppressWarnings("removal")
class LauncherBasedEnableRuleMigrationSupportTests {

	@Test
	void enableRuleMigrationSupportAnnotationWorksForBothRuleTypes() {
		Events tests = executeTestsForClass(EnableRuleMigrationSupportWithBothRuleTypesTestCase.class);

		tests.assertStatistics(stats -> stats.started(1).succeeded(1).aborted(0).failed(0));

		assertTrue(EnableRuleMigrationSupportWithBothRuleTypesTestCase.afterOfRule1WasExecuted,
			"after of rule 1 executed?");
		assertTrue(EnableRuleMigrationSupportWithBothRuleTypesTestCase.beforeOfRule2WasExecuted,
			"before of rule 2 executed?");
		assertTrue(EnableRuleMigrationSupportWithBothRuleTypesTestCase.afterOfRule2WasExecuted,
			"before of rule 2 executed?");
	}

	@Test
	void verifierSupportForErrorCollectorFieldFailsTheTest() {
		Events tests = executeTestsForClass(VerifierSupportForErrorCollectorTestCase.class);

		tests.assertStatistics(stats -> stats.started(1).succeeded(0).aborted(0).failed(1));

		assertTrue(VerifierSupportForErrorCollectorTestCase.survivedBothErrors, "after of rule 1 executed?");
	}

	private Events executeTestsForClass(Class<?> testClass) {
		return EngineTestKit.execute("junit-jupiter", request().selectors(selectClass(testClass)).build()).testEvents();
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@EnableRuleMigrationSupport
	static class EnableRuleMigrationSupportWithBothRuleTypesTestCase {

		static boolean afterOfRule1WasExecuted = false;

		static boolean beforeOfRule2WasExecuted = false;
		static boolean afterOfRule2WasExecuted = false;

		@Rule
		public Verifier verifier1 = new Verifier() {

			@Override
			protected void verify() {
				afterOfRule1WasExecuted = true;
			}
		};

		private ExternalResource resource2 = new ExternalResource() {
			@Override
			protected void before() {
				beforeOfRule2WasExecuted = true;
			}

			@Override
			protected void after() {
				afterOfRule2WasExecuted = true;
			}
		};

		@Rule
		public ExternalResource getResource2() {
			return resource2;
		}

		@Test
		void beforeMethodOfBothRule2WasExecuted() {
			assertTrue(beforeOfRule2WasExecuted);
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(VerifierSupport.class)
	static class VerifierSupportForErrorCollectorTestCase {

		static boolean survivedBothErrors = false;

		@Rule
		public ErrorCollector collector = new ErrorCollector();

		@Test
		void addingTwoThrowablesToErrorCollectorFailsLate() {
			collector.addError(new Throwable("first thing went wrong"));
			collector.addError(new Throwable("second thing went wrong"));

			survivedBothErrors = true;
		}

	}

}
