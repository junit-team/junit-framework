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

import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.EngineDiscoveryResult;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * @since 6.1
 */
class DelegatingLauncherDiscoveryListener implements LauncherDiscoveryListener {

	private final LauncherDiscoveryListener delegate;

	DelegatingLauncherDiscoveryListener(LauncherDiscoveryListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
		delegate.launcherDiscoveryStarted(request);
	}

	@Override
	public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
		delegate.launcherDiscoveryFinished(request);
	}

	@Override
	public void engineDiscoveryStarted(UniqueId engineId) {
		delegate.engineDiscoveryStarted(engineId);
	}

	@Override
	public void engineDiscoveryFinished(UniqueId engineId, EngineDiscoveryResult result) {
		delegate.engineDiscoveryFinished(engineId, result);
	}

	@Override
	public void selectorProcessed(UniqueId engineId, DiscoverySelector selector, SelectorResolutionResult result) {
		delegate.selectorProcessed(engineId, selector, result);
	}

	@Override
	public void issueEncountered(UniqueId engineId, DiscoveryIssue issue) {
		delegate.issueEncountered(engineId, issue);
	}
}
