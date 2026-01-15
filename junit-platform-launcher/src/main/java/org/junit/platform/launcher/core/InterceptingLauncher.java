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

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.10
 */
class InterceptingLauncher extends DelegatingLauncher {

	private final LauncherInterceptor interceptor;

	InterceptingLauncher(Launcher delegate, LauncherInterceptor interceptor) {
		super(delegate);
		this.interceptor = interceptor;
	}

	@Override
	public TestPlan discover(LauncherDiscoveryRequest launcherDiscoveryRequest) {
		Preconditions.notNull(launcherDiscoveryRequest, "DiscoveryRequest must not be null");
		return interceptor.intercept(() -> super.discover(launcherDiscoveryRequest));
	}

	@Override
	public void execute(LauncherDiscoveryRequest launcherDiscoveryRequest, TestExecutionListener... listeners) {
		Preconditions.notNull(launcherDiscoveryRequest, "DiscoveryRequest must not be null");
		interceptor.<@Nullable Object> intercept(() -> {
			super.execute(launcherDiscoveryRequest, listeners);
			return null;
		});
	}

	@Override
	public void execute(TestPlan testPlan, TestExecutionListener... listeners) {
		Preconditions.notNull(testPlan, "TestPlan must not be null");
		interceptor.<@Nullable Object> intercept(() -> {
			super.execute(testPlan, listeners);
			return null;
		});
	}

	@Override
	public void execute(LauncherExecutionRequest launcherExecutionRequest) {
		Preconditions.notNull(launcherExecutionRequest, "ExecutionRequest must not be null");
		interceptor.<@Nullable Object> intercept(() -> {
			super.execute(launcherExecutionRequest);
			return null;
		});
	}
}
