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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 6.0
 */
final class DefaultLauncherExecutionRequest implements LauncherExecutionRequest {

	private final @Nullable LauncherDiscoveryRequest discoveryRequest;
	private final @Nullable TestPlan testPlan;
	private final List<? extends TestExecutionListener> executionListeners;
	private final CancellationToken cancellationToken;

	DefaultLauncherExecutionRequest(@Nullable LauncherDiscoveryRequest discoveryRequest, @Nullable TestPlan testPlan,
			Collection<? extends TestExecutionListener> executionListeners, CancellationToken cancellationToken) {
		this.discoveryRequest = discoveryRequest;
		this.testPlan = testPlan;
		this.executionListeners = List.copyOf(executionListeners);
		this.cancellationToken = cancellationToken;
	}

	@Override
	public Optional<LauncherDiscoveryRequest> getDiscoveryRequest() {
		return Optional.ofNullable(discoveryRequest);
	}

	@Override
	public Optional<TestPlan> getTestPlan() {
		return Optional.ofNullable(testPlan);
	}

	@Override
	public Collection<? extends TestExecutionListener> getAdditionalTestExecutionListeners() {
		return executionListeners;
	}

	@Override
	public CancellationToken getCancellationToken() {
		return cancellationToken;
	}

}
