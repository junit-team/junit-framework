/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.logging.Level.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.EngineFilter.excludeEngines;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestEngine;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * @since 1.0
 */
class DefaultLauncherEngineFilterTests {

	private static final Runnable noOp = () -> {
	};

	@Test
	void launcherWillNotExecuteEnginesIfNotIncludedByAnEngineFilter() {
		var firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		var secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		var launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		var testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(includeEngines("first"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(1);
		var rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueIdObject())).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("first"))).hasSize(1);
	}

	@Test
	void launcherWillExecuteAllEnginesExplicitlyIncludedViaSingleEngineFilter() {
		var firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		var secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		var launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		var testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(includeEngines("first", "second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(2);
	}

	@Test
	void launcherWillNotExecuteEnginesExplicitlyIncludedViaMultipleCompetingEngineFilters() {
		var firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		var secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		var launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		var testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(includeEngines("first"), includeEngines("second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).isEmpty();
	}

	@Test
	void launcherWillNotExecuteEnginesExplicitlyExcludedByAnEngineFilter() {
		var firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		var secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		var launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		var testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(excludeEngines("second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(1);
		var rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueIdObject())).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("first"))).hasSize(1);
	}

	@Test
	void launcherWillExecuteEnginesHonoringBothIncludeAndExcludeEngineFilters() {
		var firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		var secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);
		var thirdEngine = new DemoHierarchicalTestEngine("third");
		TestDescriptor test3 = thirdEngine.addTest("test3", noOp);

		var launcher = createLauncher(firstEngine, secondEngine, thirdEngine);

		// @formatter:off
		var testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()), selectUniqueId(test3.getUniqueId()))
				.filters(includeEngines("first", "second"), excludeEngines("second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(1);
		var rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueIdObject())).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("first"))).hasSize(1);
	}

	@Test
	void launcherThrowsExceptionWhenNoEngineMatchesIncludeEngineFilter(@TrackLogRecords LogRecordListener log) {
		var engine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = engine.addTest("test1", noOp);
		LauncherDiscoveryRequest request = request() //
				.selectors(selectUniqueId(test1.getUniqueId())) //
				.filters(includeEngines("second")) //
				.build();

		var launcher = createLauncher(engine);
		var exception = assertThrows(JUnitException.class, () -> launcher.discover(request));

		assertThat(exception.getMessage()) //
				.startsWith("No TestEngine ID matched the following include EngineFilters: [second].") //
				.contains("Please fix/remove the filter or add the engine.") //
				.contains("Registered TestEngines:\n- first (") //
				.endsWith("Registered EngineFilters:\n- EngineFilter that includes engines with IDs [second]");
		assertThat(log.stream(WARNING)).isEmpty();
	}

	@Test
	void launcherThrowsExceptionWhenNoEngineMatchesIdInIncludeEngineFilter(@TrackLogRecords LogRecordListener log) {
		var engine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = engine.addTest("test1", noOp);
		LauncherDiscoveryRequest request = request() //
				.selectors(selectUniqueId(test1.getUniqueId())) //
				.filters(includeEngines("first", "second")) //
				.build();

		var launcher = createLauncher(engine);
		var exception = assertThrows(JUnitException.class, () -> launcher.discover(request));

		assertThat(exception.getMessage()) //
				.startsWith("No TestEngine ID matched the following include EngineFilters: [second].") //
				.contains("Please fix/remove the filter or add the engine.") //
				.contains("Registered TestEngines:\n- first (") //
				.endsWith("Registered EngineFilters:\n- EngineFilter that includes engines with IDs [first, second]");
		assertThat(log.stream(WARNING)).isEmpty();
	}

	@Test
	void launcherWillLogWarningWhenAllEnginesWereExcluded(@TrackLogRecords LogRecordListener log) {
		var engine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test = engine.addTest("test1", noOp);

		var launcher = createLauncher(engine);

		// @formatter:off
		var testPlan = launcher.discover(
				request()
						.selectors(selectUniqueId(test.getUniqueId()))
						.filters(excludeEngines("first"))
						.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).isEmpty();
		assertThat(log.stream(WARNING)).hasSize(1);
		assertThat(log.stream(WARNING).findAny().orElseThrow().getMessage()) //
				.startsWith("All TestEngines were excluded by EngineFilters.") //
				.contains("Registered TestEngines:\n- first (") //
				.endsWith("Registered EngineFilters:\n- EngineFilter that excludes engines with IDs [first]");
	}
}
