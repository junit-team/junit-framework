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

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.10
 */
class DelegatingLauncher implements Launcher {

	protected Launcher delegate;

	DelegatingLauncher(Launcher delegate) {
		this.delegate = delegate;
	}

	@Override
	public void registerLauncherDiscoveryListeners(LauncherDiscoveryListener... listeners) {
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		delegate.registerLauncherDiscoveryListeners(listeners);
	}

	@Override
	public void registerTestExecutionListeners(TestExecutionListener... listeners) {
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		delegate.registerTestExecutionListeners(listeners);
	}

	@Override
	public TestPlan discover(LauncherDiscoveryRequest discoveryRequest) {
		Preconditions.notNull(discoveryRequest, "discoveryRequest must not be null");
		return delegate.discover(discoveryRequest);
	}

	@Override
	public void execute(LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {
		Preconditions.notNull(discoveryRequest, "discoveryRequest must not be null");
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		delegate.execute(discoveryRequest, listeners);
	}

	@Override
	public void execute(TestPlan testPlan, TestExecutionListener... listeners) {
		Preconditions.notNull(testPlan, "testPlan must not be null");
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		delegate.execute(testPlan, listeners);
	}

	@Override
	public void execute(LauncherExecutionRequest executionRequest) {
		Preconditions.notNull(executionRequest, "executionRequest must not be null");
		delegate.execute(executionRequest);
	}
}
