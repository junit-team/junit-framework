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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.STRICT_STUBS;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.DemoMethodTestDescriptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * @since 1.0
 */
@MockitoSettings(strictness = STRICT_STUBS)
class ExecutionListenerAdapterTests {

	final UniqueId uniqueId = UniqueId.root("method", "demoTestMethod");
	final TestDescriptor testDescriptor = createDemoMethodTestDescriptor();
	final TestIdentifier testIdentifier = TestIdentifier.from(testDescriptor);

	@Mock
	TestPlan testPlan;

	@Mock
	TestExecutionListener testExecutionListener;

	@AfterEach
	void verifyNoMoreInteractions() {
		Mockito.verifyNoMoreInteractions(testPlan, testExecutionListener);
	}

	@Test
	void dynamicTestRegistered() {
		var executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);

		executionListenerAdapter.dynamicTestRegistered(testDescriptor);

		verify(testExecutionListener).dynamicTestRegistered(testIdentifier);
		verify(testPlan).addInternal(testIdentifier);
	}

	@Test
	void executionStarted() {
		var executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);

		when(testPlan.getTestIdentifier(uniqueId)).thenReturn(testIdentifier);
		executionListenerAdapter.executionStarted(testDescriptor);

		verify(testExecutionListener).executionStarted(testIdentifier);
	}

	@Test
	void executionSkipped() {
		var executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);
		var reason = "skip reason";

		when(testPlan.getTestIdentifier(uniqueId)).thenReturn(testIdentifier);
		executionListenerAdapter.executionSkipped(testDescriptor, reason);

		verify(testExecutionListener).executionSkipped(testIdentifier, reason);
	}

	@Test
	void executionFinished() {
		var executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);
		var testExecutionResult = TestExecutionResult.successful();

		when(testPlan.getTestIdentifier(uniqueId)).thenReturn(testIdentifier);
		executionListenerAdapter.executionFinished(testDescriptor, testExecutionResult);

		verify(testExecutionListener).executionFinished(testIdentifier, testExecutionResult);
	}

	@Test
	void testReportingEntryPublished() {
		var executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);
		var entry = ReportEntry.from("one", "two");

		when(testPlan.getTestIdentifier(uniqueId)).thenReturn(testIdentifier);
		executionListenerAdapter.reportingEntryPublished(testDescriptor, entry);

		verify(testExecutionListener).reportingEntryPublished(testIdentifier, entry);
	}

	@Test
	void fileEntryPublished() {
		var executionListenerAdapter = new ExecutionListenerAdapter(testPlan, testExecutionListener);
		var entry = FileEntry.from(Path.of("entry.txt"), "application/txt");

		when(testPlan.getTestIdentifier(uniqueId)).thenReturn(testIdentifier);
		executionListenerAdapter.fileEntryPublished(testDescriptor, entry);

		verify(testExecutionListener).fileEntryPublished(testIdentifier, entry);
	}

	private TestDescriptor createDemoMethodTestDescriptor() {
		var demoTestMethod = ReflectionUtils.findMethod(ExecutionListenerAdapterTests.class,
			"demoTestMethod").orElseThrow();
		return new DemoMethodTestDescriptor(uniqueId, demoTestMethod);
	}

	//for reflection purposes only
	@SuppressWarnings("unused")
	void demoTestMethod() {
	}

}
