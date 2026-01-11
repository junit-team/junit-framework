/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners.discovery;

import static org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder;

import java.util.List;

import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.EngineDiscoveryResult;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * @since 1.6
 * @see LauncherDiscoveryListeners#composite(List)
 */
class CompositeLauncherDiscoveryListener implements LauncherDiscoveryListener {

	private final List<LauncherDiscoveryListener> listeners;

	CompositeLauncherDiscoveryListener(List<LauncherDiscoveryListener> listeners) {
		this.listeners = List.copyOf(listeners);
	}

	@Override
	public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
		listeners.forEach(delegate -> delegate.launcherDiscoveryStarted(request));
	}

	@Override
	public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
		forEachInReverseOrder(listeners, delegate -> delegate.launcherDiscoveryFinished(request));
	}

	@Override
	public void engineDiscoveryStarted(UniqueId engineId) {
		listeners.forEach(delegate -> delegate.engineDiscoveryStarted(engineId));
	}

	@Override
	public void engineDiscoveryFinished(UniqueId engineId, EngineDiscoveryResult result) {
		forEachInReverseOrder(listeners, delegate -> delegate.engineDiscoveryFinished(engineId, result));
	}

	@Override
	public void selectorProcessed(UniqueId engineId, DiscoverySelector selector, SelectorResolutionResult result) {
		listeners.forEach(delegate -> delegate.selectorProcessed(engineId, selector, result));
	}

	@Override
	public void issueEncountered(UniqueId engineId, DiscoveryIssue issue) {
		listeners.forEach(delegate -> delegate.issueEncountered(engineId, issue));
	}

}
