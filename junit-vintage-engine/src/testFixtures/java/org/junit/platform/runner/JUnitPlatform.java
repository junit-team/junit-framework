/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.runner;

import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

/**
 * Dummy Runner class mimicking the one from the discontinued
 * {@code junit-platform-runner} module.
 */
public class JUnitPlatform extends ParentRunner<Void> {

	public JUnitPlatform(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected List<Void> getChildren() {
		return List.of();
	}

	@Override
	protected Description describeChild(Void child) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void runChild(Void child, RunNotifier notifier) {
		throw new UnsupportedOperationException();
	}
}
