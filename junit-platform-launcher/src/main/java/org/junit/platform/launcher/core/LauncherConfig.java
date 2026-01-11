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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;

/**
 * {@code LauncherConfig} defines the configuration API for creating
 * {@link Launcher} instances via the {@link LauncherFactory}.
 *
 * <h2>Example</h2>
 *
 * <pre class="code">
 * LauncherConfig launcherConfig = LauncherConfig.builder()
 *   .enableTestEngineAutoRegistration(false)
 *   .enableTestExecutionListenerAutoRegistration(false)
 *   .addTestEngines(new CustomTestEngine())
 *   .addTestExecutionListeners(new CustomTestExecutionListener())
 *   .build();
 *
 * Launcher launcher = LauncherFactory.create(launcherConfig);
 * LauncherDiscoveryRequest discoveryRequest = ...
 * launcher.execute(discoveryRequest);
 * </pre>
 *
 * @since 1.3
 * @see #builder()
 * @see Launcher
 * @see LauncherFactory
 */
@API(status = STABLE, since = "1.7")
public interface LauncherConfig {

	/**
	 * The default {@code LauncherConfig} which uses automatic registration for
	 * test engines, supported listeners, and post-discovery filters.
	 */
	LauncherConfig DEFAULT = builder().build();

	/**
	 * Determine if test engines should be discovered at runtime using the
	 * {@link java.util.ServiceLoader ServiceLoader} mechanism and
	 * automatically registered.
	 *
	 * @return {@code true} if test engines should be automatically registered
	 */
	boolean isTestEngineAutoRegistrationEnabled();

	/**
	 * Determine if launcher session listeners should be discovered at runtime
	 * using the {@link java.util.ServiceLoader ServiceLoader} mechanism and
	 * automatically registered.
	 *
	 * @return {@code true} if launcher session listeners should be
	 * automatically registered
	 * @since 1.8
	 */
	@API(status = STABLE, since = "1.10")
	boolean isLauncherSessionListenerAutoRegistrationEnabled();

	/**
	 * Determine if launcher discovery listeners should be discovered at runtime
	 * using the {@link java.util.ServiceLoader ServiceLoader} mechanism and
	 * automatically registered.
	 *
	 * @return {@code true} if launcher discovery listeners should be
	 * automatically registered
	 * @since 1.8
	 */
	@API(status = STABLE, since = "1.10")
	boolean isLauncherDiscoveryListenerAutoRegistrationEnabled();

	/**
	 * Determine if test execution listeners should be discovered at runtime
	 * using the {@link java.util.ServiceLoader ServiceLoader} mechanism and
	 * automatically registered.
	 *
	 * @return {@code true} if test execution listeners should be automatically
	 * registered
	 */
	boolean isTestExecutionListenerAutoRegistrationEnabled();

	/**
	 * Determine if post discovery filters should be discovered at runtime
	 * using the {@link java.util.ServiceLoader ServiceLoader} mechanism and
	 * automatically registered.
	 *
	 * @return {@code true} if post discovery filters should be automatically
	 * registered
	 * @since 1.7
	 */
	@API(status = STABLE, since = "1.10")
	boolean isPostDiscoveryFilterAutoRegistrationEnabled();

	/**
	 * Get the collection of additional test engines that should be added to
	 * the {@link Launcher}.
	 *
	 * @return the collection of additional test engines; never {@code null} but
	 * potentially empty
	 */
	Collection<TestEngine> getAdditionalTestEngines();

	/**
	 * Get the collection of additional launcher session listeners that should
	 * be added to the {@link Launcher}.
	 *
	 * @return the collection of additional launcher session listeners; never
	 * {@code null} but potentially empty
	 * @since 1.8
	 */
	@API(status = STABLE, since = "1.10")
	Collection<LauncherSessionListener> getAdditionalLauncherSessionListeners();

	/**
	 * Get the collection of additional launcher discovery listeners that should
	 * be added to the {@link Launcher}.
	 *
	 * @return the collection of additional launcher discovery listeners; never
	 * {@code null} but potentially empty
	 * @since 1.8
	 */
	@API(status = STABLE, since = "1.10")
	Collection<LauncherDiscoveryListener> getAdditionalLauncherDiscoveryListeners();

	/**
	 * Get the collection of additional test execution listeners that should be
	 * added to the {@link Launcher}.
	 *
	 * @return the collection of additional test execution listeners; never
	 * {@code null} but potentially empty
	 */
	Collection<TestExecutionListener> getAdditionalTestExecutionListeners();

	/**
	 * Get the collection of additional post discovery filters that should be
	 * added to the {@link Launcher}.
	 *
	 * @return the collection of additional post discovery filters; never
	 * {@code null} but potentially empty
	 * @since 1.7
	 */
	@API(status = STABLE, since = "1.10")
	Collection<PostDiscoveryFilter> getAdditionalPostDiscoveryFilters();

	/**
	 * Create a new {@link LauncherConfig.Builder}.
	 *
	 * @return a new builder; never {@code null}
	 */
	static Builder builder() {
		return new Builder();
	}

	/**
	 * <em>Builder</em> API for {@link LauncherConfig}.
	 */
	class Builder {

		private boolean engineAutoRegistrationEnabled = true;
		private boolean launcherSessionListenerAutoRegistrationEnabled = true;
		private boolean launcherDiscoveryListenerAutoRegistrationEnabled = true;
		private boolean testExecutionListenerAutoRegistrationEnabled = true;
		private boolean postDiscoveryFilterAutoRegistrationEnabled = true;
		private final Collection<TestEngine> engines = new LinkedHashSet<>();
		private final Collection<LauncherSessionListener> sessionListeners = new LinkedHashSet<>();
		private final Collection<LauncherDiscoveryListener> discoveryListeners = new LinkedHashSet<>();
		private final Collection<TestExecutionListener> executionListeners = new LinkedHashSet<>();
		private final Collection<PostDiscoveryFilter> postDiscoveryFilters = new LinkedHashSet<>();

		private Builder() {
			/* no-op */
		}

		/**
		 * Configure the auto-registration flag for launcher session
		 * listeners.
		 *
		 * <p>Defaults to {@code true}.
		 *
		 * @param enabled {@code true} if launcher session listeners should be
		 * automatically registered
		 * @return this builder for method chaining
		 * @since 1.8
		 */
		@API(status = STABLE, since = "1.10")
		public Builder enableLauncherSessionListenerAutoRegistration(boolean enabled) {
			this.launcherSessionListenerAutoRegistrationEnabled = enabled;
			return this;
		}

		/**
		 * Configure the auto-registration flag for launcher discovery
		 * listeners.
		 *
		 * <p>Defaults to {@code true}.
		 *
		 * @param enabled {@code true} if launcher discovery listeners should be
		 * automatically registered
		 * @return this builder for method chaining
		 * @since 1.8
		 */
		@API(status = STABLE, since = "1.10")
		public Builder enableLauncherDiscoveryListenerAutoRegistration(boolean enabled) {
			this.launcherDiscoveryListenerAutoRegistrationEnabled = enabled;
			return this;
		}

		/**
		 * Configure the auto-registration flag for test execution listeners.
		 *
		 * <p>Defaults to {@code true}.
		 *
		 * @param enabled {@code true} if test execution listeners should be
		 * automatically registered
		 * @return this builder for method chaining
		 */
		public Builder enableTestExecutionListenerAutoRegistration(boolean enabled) {
			this.testExecutionListenerAutoRegistrationEnabled = enabled;
			return this;
		}

		/**
		 * Configure the auto-registration flag for test engines.
		 *
		 * <p>Defaults to {@code true}.
		 *
		 * @param enabled {@code true} if test engines should be automatically
		 * registered
		 * @return this builder for method chaining
		 */
		public Builder enableTestEngineAutoRegistration(boolean enabled) {
			this.engineAutoRegistrationEnabled = enabled;
			return this;
		}

		/**
		 * Configure the auto-registration flag for post discovery filters.
		 *
		 * <p>Defaults to {@code true}.
		 *
		 * @param enabled {@code true} if post discovery filters should be automatically
		 * registered
		 * @return this builder for method chaining
		 * @since 1.7
		 */
		@API(status = STABLE, since = "1.10")
		public Builder enablePostDiscoveryFilterAutoRegistration(boolean enabled) {
			this.postDiscoveryFilterAutoRegistrationEnabled = enabled;
			return this;
		}

		/**
		 * Add all of the supplied test engines to the configuration.
		 *
		 * @param engines additional test engines to register; never {@code null}
		 * or containing {@code null}
		 * @return this builder for method chaining
		 */
		public Builder addTestEngines(TestEngine... engines) {
			Preconditions.notNull(engines, "TestEngine array must not be null");
			Preconditions.containsNoNullElements(engines, "TestEngine array must not contain null elements");
			Collections.addAll(this.engines, engines);
			return this;
		}

		/**
		 * Add all of the supplied launcher session listeners to the configuration.
		 *
		 * @param listeners additional launcher session listeners to register;
		 * never {@code null} or containing {@code null}
		 * @return this builder for method chaining
		 */
		public Builder addLauncherSessionListeners(LauncherSessionListener... listeners) {
			Preconditions.notNull(listeners, "LauncherSessionListener array must not be null");
			Preconditions.containsNoNullElements(listeners,
				"LauncherSessionListener array must not contain null elements");
			Collections.addAll(this.sessionListeners, listeners);
			return this;
		}

		/**
		 * Add all of the supplied launcher discovery listeners to the configuration.
		 *
		 * @param listeners additional launcher discovery listeners to register;
		 * never {@code null} or containing {@code null}
		 * @return this builder for method chaining
		 */
		public Builder addLauncherDiscoveryListeners(LauncherDiscoveryListener... listeners) {
			Preconditions.notNull(listeners, "LauncherDiscoveryListener array must not be null");
			Preconditions.containsNoNullElements(listeners,
				"LauncherDiscoveryListener array must not contain null elements");
			Collections.addAll(this.discoveryListeners, listeners);
			return this;
		}

		/**
		 * Add all of the supplied test execution listeners to the configuration.
		 *
		 * @param listeners additional test execution listeners to register;
		 * never {@code null} or containing {@code null}
		 * @return this builder for method chaining
		 */
		public Builder addTestExecutionListeners(TestExecutionListener... listeners) {
			Preconditions.notNull(listeners, "TestExecutionListener array must not be null");
			Preconditions.containsNoNullElements(listeners,
				"TestExecutionListener array must not contain null elements");
			Collections.addAll(this.executionListeners, listeners);
			return this;
		}

		/**
		 * Add all of the supplied {@code filters} to the configuration.
		 *
		 * @param filters additional post discovery filters to register;
		 * never {@code null} or containing {@code null}
		 * @return this builder for method chaining
		 * @since 1.7
		 */
		@API(status = STABLE, since = "1.10")
		public Builder addPostDiscoveryFilters(PostDiscoveryFilter... filters) {
			Preconditions.notNull(filters, "PostDiscoveryFilter array must not be null");
			Preconditions.containsNoNullElements(filters, "PostDiscoveryFilter array must not contain null elements");
			Collections.addAll(this.postDiscoveryFilters, filters);
			return this;
		}

		/**
		 * Build the {@link LauncherConfig} that has been configured via this
		 * builder.
		 */
		public LauncherConfig build() {
			return new DefaultLauncherConfig(this.engineAutoRegistrationEnabled,
				this.launcherSessionListenerAutoRegistrationEnabled,
				this.launcherDiscoveryListenerAutoRegistrationEnabled,
				this.testExecutionListenerAutoRegistrationEnabled, this.postDiscoveryFilterAutoRegistrationEnabled,
				this.engines, this.sessionListeners, this.discoveryListeners, this.executionListeners,
				this.postDiscoveryFilters);
		}

	}

}
