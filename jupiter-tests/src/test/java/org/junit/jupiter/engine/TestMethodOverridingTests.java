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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.engine.subpackage.SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * @since 6.0.1
 */
class TestMethodOverridingTests extends AbstractJupiterTestEngineTests {

	@Test
	void bothPackagePrivateTestMethodsAreDiscovered() {
		var results = discoverTestsForClass(DuplicateTestMethodDueToPackagePrivateVisibilityTestCase.class);
		var classDescriptor = getOnlyElement(results.getEngineDescriptor().getChildren());

		assertThat(classDescriptor.getChildren()).hasSize(2);

		var parentUniqueId = classDescriptor.getUniqueId();
		var inheritedMethodUniqueId = parentUniqueId.append("method",
			"org.junit.jupiter.engine.subpackage.SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase#"
					+ "test(org.junit.jupiter.api.TestInfo, org.junit.jupiter.api.TestReporter)");
		var declaredMethodUniqueId = parentUniqueId.append("method",
			"test(org.junit.jupiter.api.TestInfo, org.junit.jupiter.api.TestReporter)");

		assertThat(classDescriptor.getChildren()) //
				.extracting(TestDescriptor::getUniqueId) //
				.containsExactly(inheritedMethodUniqueId, declaredMethodUniqueId);

		results = discoverTests(selectUniqueId(inheritedMethodUniqueId));
		classDescriptor = getOnlyElement(results.getEngineDescriptor().getChildren());
		assertThat(classDescriptor.getChildren()) //
				.extracting(TestDescriptor::getUniqueId) //
				.containsExactly(inheritedMethodUniqueId);

		results = discoverTests(selectUniqueId(declaredMethodUniqueId));
		classDescriptor = getOnlyElement(results.getEngineDescriptor().getChildren());
		assertThat(classDescriptor.getChildren()) //
				.extracting(TestDescriptor::getUniqueId) //
				.containsExactly(declaredMethodUniqueId);
	}

	@Test
	void bothPackagePrivateTestMethodsAreExecuted() throws Exception {
		var results = executeTestsForClass(DuplicateTestMethodDueToPackagePrivateVisibilityTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
		assertThat(allReportEntries(results)) //
				.containsExactly(
					Map.of("invokedSuper",
						SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase.class.getDeclaredMethod(
							"test", TestInfo.class, TestReporter.class).toGenericString()),
					Map.of("invokedChild",
						DuplicateTestMethodDueToPackagePrivateVisibilityTestCase.class.getDeclaredMethod("test",
							TestInfo.class, TestReporter.class).toGenericString()));
	}

	private static Stream<Map<String, String>> allReportEntries(EngineExecutionResults results) {
		return results.allEvents().reportingEntryPublished() //
				.map(event -> event.getRequiredPayload(ReportEntry.class)) //
				.map(ReportEntry::getKeyValuePairs);
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class DuplicateTestMethodDueToPackagePrivateVisibilityTestCase
			extends SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase {

		// @Override
		@Test
		void test(TestInfo testInfo, TestReporter reporter) {
			reporter.publishEntry("invokedChild", testInfo.getTestMethod().orElseThrow().toGenericString());
		}
	}
}
