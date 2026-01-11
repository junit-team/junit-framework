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

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * An {@code ExecutionListenerAdapter} adapts a {@link TestPlan} and a corresponding
 * {@link TestExecutionListener} to the {@link EngineExecutionListener} API.
 *
 * @since 1.0
 */
class ExecutionListenerAdapter implements EngineExecutionListener {

	private final TestPlan testPlan;
	private final TestExecutionListener testExecutionListener;

	ExecutionListenerAdapter(TestPlan testPlan, TestExecutionListener testExecutionListener) {
		this.testPlan = testPlan;
		this.testExecutionListener = testExecutionListener;
	}

	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		TestIdentifier testIdentifier = TestIdentifier.from(testDescriptor);
		this.testPlan.addInternal(testIdentifier);
		this.testExecutionListener.dynamicTestRegistered(testIdentifier);
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		this.testExecutionListener.executionStarted(getTestIdentifier(testDescriptor));
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		this.testExecutionListener.executionSkipped(getTestIdentifier(testDescriptor), reason);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		this.testExecutionListener.executionFinished(getTestIdentifier(testDescriptor), testExecutionResult);
	}

	@Override
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		this.testExecutionListener.reportingEntryPublished(getTestIdentifier(testDescriptor), entry);
	}

	@Override
	public void fileEntryPublished(TestDescriptor testDescriptor, FileEntry file) {
		this.testExecutionListener.fileEntryPublished(getTestIdentifier(testDescriptor), file);
	}

	private TestIdentifier getTestIdentifier(TestDescriptor testDescriptor) {
		return this.testPlan.getTestIdentifier(testDescriptor.getUniqueId());
	}

}
