/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;

/**
 * Unit tests for {@link LauncherConfig} and {@link LauncherConfig.Builder}.
 *
 * @since 1.3
 */
class LauncherConfigTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void preconditions() {
		assertPreconditionViolationFor(() -> LauncherConfig.builder().addTestEngines((TestEngine[]) null));
		assertPreconditionViolationFor(
			() -> LauncherConfig.builder().addTestExecutionListeners((TestExecutionListener[]) null));

		TestEngine engine = new TestEngineStub();
		var listener = new TestExecutionListener() {
		};
		assertPreconditionViolationFor(() -> LauncherConfig.builder().addTestEngines(engine, engine, null));
		assertPreconditionViolationFor(
			() -> LauncherConfig.builder().addTestExecutionListeners(listener, listener, null));
	}

	@Test
	void defaultConfig() {
		var config = LauncherConfig.DEFAULT;

		assertTrue(config.isTestEngineAutoRegistrationEnabled(),
			"Test engine auto-registration should be enabled by default");
		assertTrue(config.isLauncherDiscoveryListenerAutoRegistrationEnabled(),
			"Launcher discovery listener auto-registration should be enabled by default");
		assertTrue(config.isTestExecutionListenerAutoRegistrationEnabled(),
			"Test execution listener auto-registration should be enabled by default");
		assertTrue(config.isPostDiscoveryFilterAutoRegistrationEnabled(),
			"Post-discovery filter auto-registration should be enabled by default");

		assertThat(config.getAdditionalTestEngines()).isEmpty();

		assertThat(config.getAdditionalTestExecutionListeners()).isEmpty();
	}

	@Test
	void disableTestEngineAutoRegistration() {
		var config = LauncherConfig.builder().enableTestEngineAutoRegistration(false).build();

		assertFalse(config.isTestEngineAutoRegistrationEnabled());
	}

	@Test
	void disableLauncherDiscoveryListenerAutoRegistration() {
		var config = LauncherConfig.builder().enableLauncherDiscoveryListenerAutoRegistration(false).build();

		assertFalse(config.isLauncherDiscoveryListenerAutoRegistrationEnabled());
	}

	@Test
	void disableTestExecutionListenerAutoRegistration() {
		var config = LauncherConfig.builder().enableTestExecutionListenerAutoRegistration(false).build();

		assertFalse(config.isTestExecutionListenerAutoRegistrationEnabled());
	}

	@Test
	void disablePostDiscoveryFilterAutoRegistration() {
		var config = LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build();

		assertFalse(config.isPostDiscoveryFilterAutoRegistrationEnabled());
	}

	@Test
	void addTestEngines() {
		TestEngine first = new TestEngineStub();
		TestEngine second = new TestEngineStub();

		var config = LauncherConfig.builder().addTestEngines(first, second).build();

		assertThat(config.getAdditionalTestEngines()).containsOnly(first, second);
	}

	@Test
	void addLauncherSessionListeners() {
		var first = new LauncherSessionListener() {
		};
		var second = new LauncherSessionListener() {
		};

		var config = LauncherConfig.builder().addLauncherSessionListeners(first, second).build();

		assertThat(config.getAdditionalLauncherSessionListeners()).containsOnly(first, second);
	}

	@Test
	void addLauncherDiscoveryListeners() {
		var first = new LauncherDiscoveryListener() {
		};
		var second = new LauncherDiscoveryListener() {
		};

		var config = LauncherConfig.builder().addLauncherDiscoveryListeners(first, second).build();

		assertThat(config.getAdditionalLauncherDiscoveryListeners()).containsOnly(first, second);
	}

	@Test
	void addTestExecutionListeners() {
		var first = new TestExecutionListener() {
		};
		var second = new TestExecutionListener() {
		};

		var config = LauncherConfig.builder().addTestExecutionListeners(first, second).build();

		assertThat(config.getAdditionalTestExecutionListeners()).containsOnly(first, second);
	}

}
