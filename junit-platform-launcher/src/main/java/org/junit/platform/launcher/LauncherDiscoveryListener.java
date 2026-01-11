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
import org.junit.platform.engine.EngineDiscoveryListener;
import org.junit.platform.engine.UniqueId;

/**
 * Register a concrete implementation of this interface with a
 * {@link org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder} or
 * {@link Launcher} to be notified of events that occur during test discovery.
 *
 * <p>All methods in this interface have empty <em>default</em> implementations.
 * Concrete implementations may therefore override one or more of these methods
 * to be notified of the selected events.
 *
 * <p>JUnit provides default implementations that are created via the factory
 * methods in
 * {@link org.junit.platform.launcher.listeners.discovery.LauncherDiscoveryListeners}.
 *
 * <p>The methods declared in this interface are called by the {@link Launcher}
 * created via the {@link org.junit.platform.launcher.core.LauncherFactory}
 * during test discovery.
 *
 * @since 1.6
 * @see org.junit.platform.launcher.listeners.discovery.LauncherDiscoveryListeners
 * @see LauncherDiscoveryRequest#getDiscoveryListener()
 * @see org.junit.platform.launcher.core.LauncherConfig.Builder#addLauncherDiscoveryListeners
 */
@API(status = STABLE, since = "1.11")
public interface LauncherDiscoveryListener extends EngineDiscoveryListener {

	/**
	 * No-op implementation of {@code LauncherDiscoveryListener}
	 */
	LauncherDiscoveryListener NOOP = new LauncherDiscoveryListener() {
	};

	/**
	 * Called when test discovery is about to be started.
	 *
	 * @param request the request for which discovery is being started
	 * @since 1.8
	 */
	@API(status = STABLE, since = "1.10")
	default void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
	}

	/**
	 * Called when test discovery has finished.
	 *
	 * @param request the request for which discovery has finished
	 * @since 1.8
	 */
	@API(status = STABLE, since = "1.10")
	default void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
	}

	/**
	 * Called when test discovery is about to be started for an engine.
	 *
	 * @param engineId the unique ID of the engine descriptor
	 */
	default void engineDiscoveryStarted(UniqueId engineId) {
	}

	/**
	 * Called when test discovery has finished for an engine.
	 *
	 * <p>Exceptions thrown by implementations of this method will cause the
	 * complete test discovery to be aborted.
	 *
	 * @param engineId the unique ID of the engine descriptor
	 * @param result the discovery result of the supplied engine
	 * @see EngineDiscoveryResult
	 */
	default void engineDiscoveryFinished(UniqueId engineId, EngineDiscoveryResult result) {
	}

}
