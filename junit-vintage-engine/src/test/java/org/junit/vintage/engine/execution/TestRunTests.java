/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.runner.Description.createTestDescription;
import static org.junit.vintage.engine.VintageUniqueIdBuilder.engineId;
import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.SEGMENT_TYPE_DYNAMIC;
import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.SEGMENT_TYPE_RUNNER;

import org.junit.jupiter.api.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.descriptor.VintageTestDescriptor;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithSingleTestWhichFails;

/**
 * @since 4.12
 */
class TestRunTests {

	@Test
	void returnsEmptyOptionalForUnknownDescriptions() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;
		var runnerId = engineId().append(SEGMENT_TYPE_RUNNER, testClass.getName());
		var runnerTestDescriptor = new RunnerTestDescriptor(runnerId, testClass, new BlockJUnit4ClassRunner(testClass),
			false);
		var unknownDescription = createTestDescription(testClass, "dynamicTest");

		var testRun = new TestRun(runnerTestDescriptor);
		var testDescriptor = testRun.lookupNextTestDescriptor(unknownDescription);

		assertThat(testDescriptor).isEmpty();
	}

	@Test
	void registersDynamicTestDescriptors() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;
		var runnerId = engineId().append(SEGMENT_TYPE_RUNNER, testClass.getName());
		var runnerTestDescriptor = new RunnerTestDescriptor(runnerId, testClass, new BlockJUnit4ClassRunner(testClass),
			false);
		var dynamicTestId = runnerId.append(SEGMENT_TYPE_DYNAMIC, "dynamicTest");
		var dynamicDescription = createTestDescription(testClass, "dynamicTest");
		var dynamicTestDescriptor = new VintageTestDescriptor(dynamicTestId, dynamicDescription, null);

		var testRun = new TestRun(runnerTestDescriptor);
		testRun.registerDynamicTest(dynamicTestDescriptor);

		assertThat(testRun.lookupNextTestDescriptor(dynamicDescription)).contains(dynamicTestDescriptor);
		assertTrue(testRun.isDescendantOfRunnerTestDescriptor(dynamicTestDescriptor));
	}

}
