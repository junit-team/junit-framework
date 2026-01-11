/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners.session;

import static org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder;

import java.util.List;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * @since 1.8
 * @see LauncherSessionListeners#composite(List)
 */
class CompositeLauncherSessionListener implements LauncherSessionListener {

	private final List<LauncherSessionListener> listeners;

	CompositeLauncherSessionListener(List<LauncherSessionListener> listeners) {
		this.listeners = List.copyOf(listeners);
	}

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		listeners.forEach(delegate -> delegate.launcherSessionOpened(session));
	}

	@Override
	public void launcherSessionClosed(LauncherSession session) {
		forEachInReverseOrder(listeners, delegate -> delegate.launcherSessionClosed(session));
	}
}
