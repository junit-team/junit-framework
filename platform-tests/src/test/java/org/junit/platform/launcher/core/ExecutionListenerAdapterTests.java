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

import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.DemoMethodTestDescriptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherDiscoveryResult.EngineResultInfo;

/**
 * @since 1.0
 */
class ExecutionListenerAdapterTests {

	private TestDescriptor testDescriptor;
	private InternalTestPlan testPlan;
	private TestExecutionListener testExecutionListener;
	private ExecutionListenerAdapter executionListenerAdapter;

	@BeforeEach
	void setUp() {
		testDescriptor = getSampleMethodTestDescriptor();

		var discoveryResult = new LauncherDiscoveryResult(
			Map.of(mock(), EngineResultInfo.completed(testDescriptor, DiscoveryIssueNotifier.NO_ISSUES)), mock(),
			dummyOutputDirectoryCreator());
		testPlan = InternalTestPlan.from(discoveryResult);

		testExecutionListener = spy(TestExecutionListener.class);
		executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);
	}

	@Test
	void testReportingEntryPublished() {
		var testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());
		var entry = ReportEntry.from("one", "two");

		executionListenerAdapter.reportingEntryPublished(testDescriptor, entry);

		verify(testExecutionListener).reportingEntryPublished(testIdentifier, entry);
	}

	@Test
	void testDynamicTestRegistered() {
		var testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());

		executionListenerAdapter.dynamicTestRegistered(testDescriptor);

		verify(testExecutionListener).dynamicTestRegistered(testIdentifier);
	}

	@Test
	void testExecutionStarted() {
		var testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());

		executionListenerAdapter.executionStarted(testDescriptor);

		verify(testExecutionListener).executionStarted(testIdentifier);
	}

	@Test
	void testExecutionSkipped() {
		var testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());
		var reason = "Test skipped due to condition";

		executionListenerAdapter.executionSkipped(testDescriptor, reason);

		verify(testExecutionListener).executionSkipped(testIdentifier, reason);
	}

	@Test
	void testExecutionFinished() {
		var testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());
		var result = mock(TestExecutionResult.class);

		executionListenerAdapter.executionFinished(testDescriptor, result);

		verify(testExecutionListener).executionFinished(testIdentifier, result);
	}

	@Test
	void testFileEntryPublished() {
		var testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());
		var fileEntry = mock(FileEntry.class);

		executionListenerAdapter.fileEntryPublished(testDescriptor, fileEntry);

		verify(testExecutionListener).fileEntryPublished(testIdentifier, fileEntry);
	}

	private TestDescriptor getSampleMethodTestDescriptor() {
		var localMethodNamedNothing = ReflectionUtils.findMethod(this.getClass(), "nothing",
			new Class<?>[0]).orElseThrow();
		return new DemoMethodTestDescriptor(UniqueId.root("method", "unique_id"), localMethodNamedNothing);
	}

	//for reflection purposes only
	void nothing() {
	}

}
