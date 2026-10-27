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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.launcher.LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_DEFAULT;
import static org.junit.platform.launcher.LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME;

import java.util.Locale;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;

/**
 * The {@link LauncherPhase phase} during which the {@link org.junit.platform.launcher.Launcher Launcher}
 * should report discovery issues.
 *
 * @since 6.2
 */
@API(status = INTERNAL, since = "6.2")
public enum DiscoveryIssueFailurePhase {

	/**
	 * Discovery issues are reported during the discovery phase.
	 */
	DISCOVERY,

	/**
	 * Discovery issues are reported during the execution phase.
	 */
	EXECUTION,

	/**
	 * Discovery issues are reported during the discovery phase if
	 * {@link org.junit.platform.launcher.Launcher#discover(LauncherDiscoveryRequest) Launcher.discover}
	 * is called, and during the execution phase if
	 * {@link org.junit.platform.launcher.Launcher#execute(LauncherExecutionRequest) Launcher.execute}
	 * is called.
	 */
	AUTOMATIC;

	static DiscoveryIssueFailurePhase getDiscoveryIssueFailurePhase(ConfigurationParameters configurationParameters) {
		Function<String, @Nullable DiscoveryIssueFailurePhase> stringLauncherPhaseFunction = value -> {
			try {
				return parseValue(value);
			}
			catch (Exception e) {
				throw new JUnitException(
					"Invalid DiscoveryIssueFailurePhase '%s' set via the '%s' configuration parameter.".formatted(value,
						DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME));
			}
		};
		return configurationParameters.get(DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME,
			stringLauncherPhaseFunction).orElseGet(() -> parseValue(DISCOVERY_ISSUE_FAILURE_PHASE_DEFAULT));
	}

	private static DiscoveryIssueFailurePhase parseValue(String value) {
		return DiscoveryIssueFailurePhase.valueOf(value.toUpperCase(Locale.ROOT));
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
