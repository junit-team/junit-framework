/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;

import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * @since 6.0
 */
class FailFastListener implements TestExecutionListener {

	private final CancellationToken cancellationToken;

	FailFastListener(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testExecutionResult.getStatus() == FAILED) {
			cancellationToken.cancel();
		}
	}
}
