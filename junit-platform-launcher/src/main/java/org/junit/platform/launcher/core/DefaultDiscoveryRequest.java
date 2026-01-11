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

import java.util.List;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * {@code DefaultDiscoveryRequest} is the default implementation of the
 * {@link EngineDiscoveryRequest} and {@link LauncherDiscoveryRequest} APIs.
 *
 * @since 1.0
 */
final class DefaultDiscoveryRequest implements LauncherDiscoveryRequest {

	// Selectors provided to the engines to be used for discovering tests
	private final List<DiscoverySelector> selectors;

	// Filters based on engines
	private final List<EngineFilter> engineFilters;

	// Discovery filters are handed through to all engines to be applied during discovery.
	private final List<DiscoveryFilter<?>> discoveryFilters;

	// Descriptor filters are applied by the launcher itself after engines have performed discovery.
	private final List<PostDiscoveryFilter> postDiscoveryFilters;

	// Configuration parameters can be used to provide custom configuration to engines, e.g. for extensions
	private final LauncherConfigurationParameters configurationParameters;

	// Listener for test discovery that may abort on errors.
	private final LauncherDiscoveryListener discoveryListener;

	private final OutputDirectoryCreator outputDirectoryCreator;

	DefaultDiscoveryRequest(List<DiscoverySelector> selectors, List<EngineFilter> engineFilters,
			List<DiscoveryFilter<?>> discoveryFilters, List<PostDiscoveryFilter> postDiscoveryFilters,
			LauncherConfigurationParameters configurationParameters, LauncherDiscoveryListener discoveryListener,
			OutputDirectoryCreator outputDirectoryCreator) {
		this.selectors = List.copyOf(selectors);
		this.engineFilters = List.copyOf(engineFilters);
		this.discoveryFilters = List.copyOf(discoveryFilters);
		this.postDiscoveryFilters = List.copyOf(postDiscoveryFilters);
		this.configurationParameters = configurationParameters;
		this.discoveryListener = discoveryListener;
		this.outputDirectoryCreator = outputDirectoryCreator;
	}

	@Override
	public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
		Preconditions.notNull(selectorType, "selectorType must not be null");
		return this.selectors.stream().filter(selectorType::isInstance).map(selectorType::cast).toList();
	}

	@Override
	public List<EngineFilter> getEngineFilters() {
		return this.engineFilters;
	}

	@Override
	public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
		Preconditions.notNull(filterType, "filterType must not be null");
		return this.discoveryFilters.stream().filter(filterType::isInstance).map(filterType::cast).toList();
	}

	@Override
	public List<PostDiscoveryFilter> getPostDiscoveryFilters() {
		return this.postDiscoveryFilters;
	}

	@Override
	public ConfigurationParameters getConfigurationParameters() {
		return this.configurationParameters;
	}

	@Override
	public LauncherDiscoveryListener getDiscoveryListener() {
		return this.discoveryListener;
	}

	@Override
	public OutputDirectoryCreator getOutputDirectoryCreator() {
		return this.outputDirectoryCreator;
	}

}
