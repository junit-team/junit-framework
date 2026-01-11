/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import org.junit.platform.commons.util.Preconditions;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.vintage.engine.descriptor.RunnerDecorator;

/**
 * Decorator for Runners that will be ignored completely.
 *
 * <p>Contrary to {@link org.junit.internal.builders.IgnoredClassRunner}, this
 * runner returns a complete description including all children.
 *
 * @since 5.1
 */
class IgnoringRunnerDecorator extends Runner implements RunnerDecorator {

	protected final Runner runner;

	IgnoringRunnerDecorator(Runner runner) {
		this.runner = Preconditions.notNull(runner, "Runner must not be null");
	}

	@Override
	public Description getDescription() {
		return runner.getDescription();
	}

	@Override
	public void run(RunNotifier notifier) {
		notifier.fireTestIgnored(getDescription());
	}

	@Override
	public Runner getDecoratedRunner() {
		return runner;
	}
}
