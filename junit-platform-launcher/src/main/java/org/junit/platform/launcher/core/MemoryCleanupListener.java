/*
 * Copyright 2026 the original author or authors.
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
import org.junit.platform.launcher.TestPlan;

/**
 * @since 6.1
 */
class MemoryCleanupListener extends DelegatingEngineExecutionListener {

	private final TestPlan testPlan;

	MemoryCleanupListener(EngineExecutionListener delegate, TestPlan testPlan) {
		super(delegate);
		this.testPlan = testPlan;
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		super.executionSkipped(testDescriptor, reason);
		cleanUp(testDescriptor);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		super.executionFinished(testDescriptor, testExecutionResult);
		cleanUp(testDescriptor);
	}

	private void cleanUp(TestDescriptor testDescriptor) {
		testPlan.removeInternal(testDescriptor.getUniqueId());
		if (!testDescriptor.isRoot()) {
			testDescriptor.removeFromHierarchy();
		}
	}
}
