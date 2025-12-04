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

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.DemoMethodTestDescriptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryResult.EngineResultInfo;

/**
 * @since 1.0
 */
// TODO Test other adapter methods.
class ExecutionListenerAdapterTests {

	@Test
	void testReportingEntryPublished() {
		var testDescriptor = getSampleMethodTestDescriptor();

		var discoveryResult = new LauncherDiscoveryResult(
			Map.of(mock(), EngineResultInfo.completed(testDescriptor, DiscoveryIssueNotifier.NO_ISSUES)), mock(),
			dummyOutputDirectoryCreator());
		var testPlan = InternalTestPlan.from(discoveryResult);
		var testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());

		//not yet spyable with mockito? -> https://github.com/mockito/mockito/issues/146
		var testExecutionListener = new MockTestExecutionListener();
		var executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);

		var entry = ReportEntry.from("one", "two");
		executionListenerAdapter.reportingEntryPublished(testDescriptor, entry);

		assertThat(testExecutionListener.entry).isEqualTo(entry);
		assertThat(testExecutionListener.testIdentifier).isEqualTo(testIdentifier);
	}

	private TestDescriptor getSampleMethodTestDescriptor() {
		var localMethodNamedNothing = ReflectionUtils.findMethod(this.getClass(), "nothing",
			new Class<?>[0]).orElseThrow();
		return new DemoMethodTestDescriptor(UniqueId.root("method", "unique_id"), localMethodNamedNothing);
	}

	//for reflection purposes only
	void nothing() {
	}

	static class MockTestExecutionListener implements TestExecutionListener {

		@Nullable
		public TestIdentifier testIdentifier;

		@Nullable
		public ReportEntry entry;

		@Override
		public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
			this.testIdentifier = testIdentifier;
			this.entry = entry;
		}

	}

}
