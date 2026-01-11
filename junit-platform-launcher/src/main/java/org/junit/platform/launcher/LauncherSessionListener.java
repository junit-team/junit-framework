/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * Register an implementation of this interface to be notified when a
 * {@link LauncherSession} is opened and closed.
 *
 * <p>A {@code LauncherSessionListener} can be registered programmatically with
 * the {@link LauncherConfig.Builder#addLauncherSessionListeners LauncherConfig}
 * passed to the
 * {@link LauncherFactory#openSession(LauncherConfig) LauncherFactory} or
 * automatically via Java's {@link java.util.ServiceLoader ServiceLoader}
 * mechanism.
 *
 * <p>All methods in this class have empty <em>default</em> implementations.
 * Subclasses may therefore override one or more of these methods to be notified
 * of the selected events.
 *
 * <p>The methods declared in this interface are called by the {@link Launcher}
 * or {@link LauncherSession} created via the {@link LauncherFactory}.
 *
 * @since 1.8
 * @see LauncherSession
 * @see LauncherConfig.Builder#addLauncherSessionListeners
 * @see LauncherFactory
 */
@API(status = STABLE, since = "1.10")
public interface LauncherSessionListener {

	/**
	 * No-op implementation of {@code LauncherSessionListener}
	 */
	LauncherSessionListener NOOP = new LauncherSessionListener() {
	};

	/**
	 * Called when a launcher session was opened.
	 *
	 * @param session the opened session
	 */
	default void launcherSessionOpened(LauncherSession session) {
	}

	/**
	 * Called when a launcher session was closed.
	 *
	 * @param session the closed session
	 */
	default void launcherSessionClosed(LauncherSession session) {
	}

}
