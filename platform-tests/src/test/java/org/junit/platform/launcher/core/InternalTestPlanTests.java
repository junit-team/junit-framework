/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.launcher.core.LauncherDiscoveryResult.EngineResultInfo;

public class InternalTestPlanTests {

	private final ConfigurationParameters configParams = mock();

	private final EngineDescriptor engineDescriptor = new EngineDescriptor(UniqueId.forEngine("foo"), "Foo");

	@Test
	void doesNotContainTestsForEmptyContainers() {
		engineDescriptor.addChild(
			new AbstractTestDescriptor(engineDescriptor.getUniqueId().append("test", "bar"), "Bar") {
				@Override
				public Type getType() {
					return Type.CONTAINER;
				}
			});

		var testPlan = InternalTestPlan.from(createLauncherDiscoveryResult(
			EngineResultInfo.completed(engineDescriptor, DiscoveryIssueNotifier.NO_ISSUES)));

		assertThat(testPlan.containsTests()).as("contains tests").isFalse();
	}

	@Test
	void containsTestsForTests() {
		engineDescriptor.addChild(
			new AbstractTestDescriptor(engineDescriptor.getUniqueId().append("test", "bar"), "Bar") {
				@Override
				public Type getType() {
					return Type.TEST;
				}
			});

		var testPlan = InternalTestPlan.from(createLauncherDiscoveryResult(
			EngineResultInfo.completed(engineDescriptor, DiscoveryIssueNotifier.NO_ISSUES)));

		assertThat(testPlan.containsTests()).as("contains tests").isTrue();
	}

	@Test
	void containsTestsForContainersThatMayRegisterTests() {
		engineDescriptor.addChild(
			new AbstractTestDescriptor(engineDescriptor.getUniqueId().append("test", "bar"), "Bar") {
				@Override
				public Type getType() {
					return Type.CONTAINER;
				}

				@Override
				public boolean mayRegisterTests() {
					return true;
				}
			});

		var testPlan = InternalTestPlan.from(createLauncherDiscoveryResult(
			EngineResultInfo.completed(engineDescriptor, DiscoveryIssueNotifier.NO_ISSUES)));

		assertThat(testPlan.containsTests()).as("contains tests").isTrue();
	}

	@Test
	void containsTestsForEnginesWithDiscoveryError() {
		var testPlan = InternalTestPlan.from(createLauncherDiscoveryResult(
			EngineResultInfo.errored(engineDescriptor, DiscoveryIssueNotifier.NO_ISSUES, new RuntimeException())));

		assertThat(testPlan.containsTests()).as("contains tests").isTrue();
	}

	@Test
	void containsTestsForEnginesWithCriticalDiscoveryIssues() {
		var testPlan = InternalTestPlan.from(createLauncherDiscoveryResult(EngineResultInfo.completed(engineDescriptor,
			DiscoveryIssueNotifier.from(Severity.ERROR, List.of(DiscoveryIssue.create(Severity.ERROR, "error"))))));

		assertThat(testPlan.containsTests()).as("contains tests").isTrue();
	}

	@Test
	void doesNotContainTestsForEnginesWithNonCriticalDiscoveryIssues() {
		var testPlan = InternalTestPlan.from(createLauncherDiscoveryResult(EngineResultInfo.completed(engineDescriptor,
			DiscoveryIssueNotifier.from(Severity.ERROR, List.of(DiscoveryIssue.create(Severity.WARNING, "warning"))))));

		assertThat(testPlan.containsTests()).as("contains tests").isFalse();
	}

	private LauncherDiscoveryResult createLauncherDiscoveryResult(EngineResultInfo result) {
		var testEngineResults = Map.of(mock(TestEngine.class), result);
		return new LauncherDiscoveryResult(testEngineResults, configParams, dummyOutputDirectoryCreator());
	}

}
