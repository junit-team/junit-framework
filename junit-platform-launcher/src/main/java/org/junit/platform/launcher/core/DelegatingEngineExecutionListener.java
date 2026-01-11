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

/**
 * @since 1.6
 */
class DelegatingEngineExecutionListener implements EngineExecutionListener {

	private final EngineExecutionListener delegate;

	DelegatingEngineExecutionListener(EngineExecutionListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		delegate.dynamicTestRegistered(testDescriptor);
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		delegate.executionSkipped(testDescriptor, reason);
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		delegate.executionStarted(testDescriptor);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		delegate.executionFinished(testDescriptor, testExecutionResult);
	}

	@Override
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		delegate.reportingEntryPublished(testDescriptor, entry);
	}

	@Override
	public void fileEntryPublished(TestDescriptor testDescriptor, FileEntry file) {
		delegate.fileEntryPublished(testDescriptor, file);
	}
}
