/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * {@code EngineDiscoveryListener} defines the API which enables a {@link TestEngine}
 * to publish information about events that occur during test discovery.
 *
 * <p>All methods in this interface have empty <em>default</em> implementations.
 * Concrete implementations may therefore override one or more of these methods
 * to be notified of the selected events.
 *
 * @since 1.6
 * @see EngineDiscoveryRequest#getDiscoveryListener()
 * @see org.junit.platform.launcher.LauncherDiscoveryListener
 */
@API(status = STABLE, since = "1.10")
public interface EngineDiscoveryListener {

	/**
	 * No-op implementation of {@code EngineDiscoveryListener}
	 */
	EngineDiscoveryListener NOOP = new EngineDiscoveryListener() {
	};

	/**
	 * Must be called after a discovery selector has been processed by a test
	 * engine.
	 *
	 * <p>Exceptions thrown by implementations of this method will cause test
	 * discovery of the current engine to be aborted.
	 *
	 * @param engineId the unique ID of the engine descriptor
	 * @param selector the processed selector
	 * @param result the resolution result of the supplied engine and selector
	 * @see SelectorResolutionResult
	 */
	default void selectorProcessed(UniqueId engineId, DiscoverySelector selector, SelectorResolutionResult result) {
	}

	/**
	 * Called when the engine with the supplied {@code engineId} encountered an
	 * issue during test discovery.
	 *
	 * @param engineId the unique ID of the engine descriptor
	 * @param issue the encountered issue
	 * @since 1.13
	 * @see DiscoveryIssue
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	default void issueEncountered(UniqueId engineId, DiscoveryIssue issue) {
	}

}
