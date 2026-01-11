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

import org.junit.platform.engine.CancellationToken;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

/**
 * @since 6.0
 */
class CancellationTokenAwareRunNotifier extends RunNotifier {

	private final CancellationToken cancellationToken;

	CancellationTokenAwareRunNotifier(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
	}

	@Override
	public void fireTestStarted(Description description) throws StoppedByUserException {
		if (cancellationToken.isCancellationRequested()) {
			pleaseStop();
		}
		super.fireTestStarted(description);
	}
}
