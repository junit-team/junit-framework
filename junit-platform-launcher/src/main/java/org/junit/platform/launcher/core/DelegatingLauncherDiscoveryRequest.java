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

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * @since 1.13
 */
class DelegatingLauncherDiscoveryRequest implements LauncherDiscoveryRequest {

	private final LauncherDiscoveryRequest request;

	DelegatingLauncherDiscoveryRequest(LauncherDiscoveryRequest request) {
		this.request = request;
	}

	@Override
	public List<EngineFilter> getEngineFilters() {
		return this.request.getEngineFilters();
	}

	@Override
	public List<PostDiscoveryFilter> getPostDiscoveryFilters() {
		return this.request.getPostDiscoveryFilters();
	}

	@Override
	public LauncherDiscoveryListener getDiscoveryListener() {
		return this.request.getDiscoveryListener();
	}

	@Override
	public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
		return this.request.getSelectorsByType(selectorType);
	}

	@Override
	public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
		return this.request.getFiltersByType(filterType);
	}

	@Override
	public ConfigurationParameters getConfigurationParameters() {
		return this.request.getConfigurationParameters();
	}

	@SuppressWarnings("removal")
	@Override
	public org.junit.platform.engine.reporting.OutputDirectoryProvider getOutputDirectoryProvider() {
		return this.request.getOutputDirectoryProvider();
	}

	@Override
	public OutputDirectoryCreator getOutputDirectoryCreator() {
		return this.request.getOutputDirectoryCreator();
	}
}
