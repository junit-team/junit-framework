/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners.session;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.mockito.InOrder;

class CompositeLauncherSessionListenerTests {

	@Test
	void callsListenersInReverseOrderForClosedEvents() {
		var firstListener = mock(LauncherSessionListener.class, "firstListener");
		var secondListener = mock(LauncherSessionListener.class, "secondListener");

		var launcherSession = mock(LauncherSession.class);

		var composite = new CompositeLauncherSessionListener(List.of(firstListener, secondListener));
		composite.launcherSessionOpened(launcherSession);
		composite.launcherSessionClosed(launcherSession);

		InOrder inOrder = inOrder(firstListener, secondListener);
		inOrder.verify(firstListener).launcherSessionOpened(launcherSession);
		inOrder.verify(secondListener).launcherSessionOpened(launcherSession);
		inOrder.verify(secondListener).launcherSessionClosed(launcherSession);
		inOrder.verify(firstListener).launcherSessionClosed(launcherSession);
	}
}
