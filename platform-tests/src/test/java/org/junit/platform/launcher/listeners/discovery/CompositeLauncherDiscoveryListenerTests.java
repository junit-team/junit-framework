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

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.EngineDiscoveryResult;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.mockito.InOrder;

class CompositeLauncherDiscoveryListenerTests {

	@Test
	void callsListenersInReverseOrderForFinishedEvents() {
		var firstListener = mock(LauncherDiscoveryListener.class, "firstListener");
		var secondListener = mock(LauncherDiscoveryListener.class, "secondListener");

		var launcherDiscoveryRequest = mock(LauncherDiscoveryRequest.class);
		var engineId = UniqueId.forEngine("engine");
		var engineDiscoveryResult = EngineDiscoveryResult.successful();
		var selector = selectUniqueId(engineId);
		var selectorResolutionResult = SelectorResolutionResult.resolved();
		var discoveryIssue = DiscoveryIssue.create(Severity.WARNING, "message");

		var composite = new CompositeLauncherDiscoveryListener(List.of(firstListener, secondListener));
		composite.launcherDiscoveryStarted(launcherDiscoveryRequest);
		composite.engineDiscoveryStarted(engineId);
		composite.selectorProcessed(engineId, selector, selectorResolutionResult);
		composite.issueEncountered(engineId, discoveryIssue);
		composite.engineDiscoveryFinished(engineId, engineDiscoveryResult);
		composite.launcherDiscoveryFinished(launcherDiscoveryRequest);

		InOrder inOrder = inOrder(firstListener, secondListener);

		inOrder.verify(firstListener).launcherDiscoveryStarted(launcherDiscoveryRequest);
		inOrder.verify(secondListener).launcherDiscoveryStarted(launcherDiscoveryRequest);
		inOrder.verify(firstListener).engineDiscoveryStarted(engineId);
		inOrder.verify(secondListener).engineDiscoveryStarted(engineId);

		inOrder.verify(firstListener).selectorProcessed(engineId, selector, selectorResolutionResult);
		inOrder.verify(secondListener).selectorProcessed(engineId, selector, selectorResolutionResult);
		inOrder.verify(firstListener).issueEncountered(engineId, discoveryIssue);
		inOrder.verify(secondListener).issueEncountered(engineId, discoveryIssue);

		inOrder.verify(secondListener).engineDiscoveryFinished(engineId, engineDiscoveryResult);
		inOrder.verify(firstListener).engineDiscoveryFinished(engineId, engineDiscoveryResult);
		inOrder.verify(secondListener).launcherDiscoveryFinished(launcherDiscoveryRequest);
		inOrder.verify(firstListener).launcherDiscoveryFinished(launcherDiscoveryRequest);
	}
}
