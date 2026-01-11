/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import org.junit.runner.notification.RunNotifier;

/**
 * @since 4.12
 */
public class ExceptionThrowingRunner extends ConfigurableRunner {

	public ExceptionThrowingRunner(Class<?> testClass) {
		super(testClass);
	}

	@Override
	public void run(RunNotifier notifier) {
		throw new RuntimeException("Simulated exception in custom runner for " + testClass.getName());
	}

}
