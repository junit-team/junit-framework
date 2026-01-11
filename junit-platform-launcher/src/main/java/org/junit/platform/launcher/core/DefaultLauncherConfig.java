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

import static java.util.Collections.unmodifiableCollection;

import java.util.Collection;

import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;

/**
 * Default implementation of the {@link LauncherConfig} API.
 *
 * @since 1.3
 */
class DefaultLauncherConfig implements LauncherConfig {

	private final boolean testEngineAutoRegistrationEnabled;
	private final boolean launcherSessionListenerAutoRegistrationEnabled;
	private final boolean launcherDiscoveryListenerAutoRegistrationEnabled;
	private final boolean testExecutionListenerAutoRegistrationEnabled;
	private final boolean postDiscoveryFilterAutoRegistrationEnabled;
	private final Collection<TestEngine> additionalTestEngines;
	private final Collection<LauncherSessionListener> additionalLauncherSessionListeners;
	private final Collection<LauncherDiscoveryListener> additionalLauncherDiscoveryListeners;
	private final Collection<TestExecutionListener> additionalTestExecutionListeners;
	private final Collection<PostDiscoveryFilter> additionalPostDiscoveryFilters;

	DefaultLauncherConfig(boolean testEngineAutoRegistrationEnabled,
			boolean launcherSessionListenerAutoRegistrationEnabled,
			boolean launcherDiscoveryListenerAutoRegistrationEnabled,
			boolean testExecutionListenerAutoRegistrationEnabled, boolean postDiscoveryFilterAutoRegistrationEnabled,
			Collection<TestEngine> additionalTestEngines,
			Collection<LauncherSessionListener> additionalLauncherSessionListeners,
			Collection<LauncherDiscoveryListener> additionalLauncherDiscoveryListeners,
			Collection<TestExecutionListener> additionalTestExecutionListeners,
			Collection<PostDiscoveryFilter> additionalPostDiscoveryFilters) {
		this.launcherSessionListenerAutoRegistrationEnabled = launcherSessionListenerAutoRegistrationEnabled;
		this.launcherDiscoveryListenerAutoRegistrationEnabled = launcherDiscoveryListenerAutoRegistrationEnabled;
		this.testExecutionListenerAutoRegistrationEnabled = testExecutionListenerAutoRegistrationEnabled;
		this.testEngineAutoRegistrationEnabled = testEngineAutoRegistrationEnabled;
		this.postDiscoveryFilterAutoRegistrationEnabled = postDiscoveryFilterAutoRegistrationEnabled;
		this.additionalTestEngines = unmodifiableCollection(additionalTestEngines);
		this.additionalLauncherSessionListeners = unmodifiableCollection(additionalLauncherSessionListeners);
		this.additionalLauncherDiscoveryListeners = unmodifiableCollection(additionalLauncherDiscoveryListeners);
		this.additionalTestExecutionListeners = unmodifiableCollection(additionalTestExecutionListeners);
		this.additionalPostDiscoveryFilters = unmodifiableCollection(additionalPostDiscoveryFilters);
	}

	@Override
	public boolean isTestEngineAutoRegistrationEnabled() {
		return this.testEngineAutoRegistrationEnabled;
	}

	@Override
	public boolean isLauncherSessionListenerAutoRegistrationEnabled() {
		return launcherSessionListenerAutoRegistrationEnabled;
	}

	@Override
	public boolean isLauncherDiscoveryListenerAutoRegistrationEnabled() {
		return launcherDiscoveryListenerAutoRegistrationEnabled;
	}

	@Override
	public boolean isTestExecutionListenerAutoRegistrationEnabled() {
		return this.testExecutionListenerAutoRegistrationEnabled;
	}

	@Override
	public boolean isPostDiscoveryFilterAutoRegistrationEnabled() {
		return this.postDiscoveryFilterAutoRegistrationEnabled;
	}

	@Override
	public Collection<TestEngine> getAdditionalTestEngines() {
		return this.additionalTestEngines;
	}

	@Override
	public Collection<LauncherSessionListener> getAdditionalLauncherSessionListeners() {
		return additionalLauncherSessionListeners;
	}

	@Override
	public Collection<LauncherDiscoveryListener> getAdditionalLauncherDiscoveryListeners() {
		return additionalLauncherDiscoveryListeners;
	}

	@Override
	public Collection<TestExecutionListener> getAdditionalTestExecutionListeners() {
		return this.additionalTestExecutionListeners;
	}

	@Override
	public Collection<PostDiscoveryFilter> getAdditionalPostDiscoveryFilters() {
		return this.additionalPostDiscoveryFilters;
	}

}
