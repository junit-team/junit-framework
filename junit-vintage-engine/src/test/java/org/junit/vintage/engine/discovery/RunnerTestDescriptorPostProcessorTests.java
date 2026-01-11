/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.mockito.Mockito.mock;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.vintage.engine.VintageUniqueIdBuilder;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.samples.junit4.IgnoredJUnit4TestCase;
import org.junit.vintage.engine.samples.junit4.IgnoredJUnit4TestCaseWithNotFilterableRunner;
import org.junit.vintage.engine.samples.junit4.NotFilterableRunner;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithFiveTestMethods;

/**
 * Tests for {@link RunnerTestDescriptorPostProcessor}.
 *
 * @since 5.5
 */
@TrackLogRecords
class RunnerTestDescriptorPostProcessorTests {

	@Test
	void doesNotLogAnythingForFilterableRunner(LogRecordListener listener) {
		resolve(selectMethod(PlainJUnit4TestCaseWithFiveTestMethods.class, "successfulTest"));

		assertThat(listener.stream(RunnerTestDescriptor.class)).isEmpty();
	}

	@Test
	void doesNotLogAnythingForNonFilterableRunnerIfNoFiltersAreToBeApplied(LogRecordListener listener) {
		resolve(selectClass(IgnoredJUnit4TestCase.class));

		assertThat(listener.stream(RunnerTestDescriptor.class)).isEmpty();
	}

	@Test
	void logsWarningOnNonFilterableRunner(LogRecordListener listener) {
		Class<?> testClass = IgnoredJUnit4TestCaseWithNotFilterableRunner.class;

		resolve(selectMethod(testClass, "someTest"));

		// @formatter:off
		assertThat(listener.stream(RunnerTestDescriptor.class, Level.WARNING).map(LogRecord::getMessage))
				.containsOnlyOnce("Runner " + NotFilterableRunner.class.getName()
						+ " (used on class " + testClass.getName() + ") does not support filtering"
						+ " and will therefore be run completely.");
		// @formatter:on
	}

	private void resolve(DiscoverySelector selector) {
		var request = LauncherDiscoveryRequestBuilder.request().selectors(selector).listeners(
			mock(LauncherDiscoveryListener.class)).build();
		TestDescriptor engineDescriptor = new VintageDiscoverer().discover(request, VintageUniqueIdBuilder.engineId());
		var runnerTestDescriptor = (RunnerTestDescriptor) getOnlyElement(engineDescriptor.getChildren());
		new RunnerTestDescriptorPostProcessor().applyFiltersAndCreateDescendants(runnerTestDescriptor);
	}

}
