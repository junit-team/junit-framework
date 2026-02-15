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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.LauncherDiscoveryListener;

class DiscoveryIssueCollector implements LauncherDiscoveryListener {

	final List<DiscoveryIssue> issues = new ArrayList<>();
	private final Severity criticalSeverity;

	DiscoveryIssueCollector(ConfigurationParameters configurationParameters) {
		this.criticalSeverity = getCriticalSeverity(configurationParameters);
	}

	@Override
	public void engineDiscoveryStarted(UniqueId engineId) {
		this.issues.clear();
	}

	@Override
	public void issueEncountered(UniqueId engineId, DiscoveryIssue issue) {
		this.issues.add(issue);
	}

	DiscoveryIssueNotifier toNotifier() {
		if (this.issues.isEmpty()) {
			return DiscoveryIssueNotifier.NO_ISSUES;
		}
		return DiscoveryIssueNotifier.from(criticalSeverity, this.issues);
	}

	private static Severity getCriticalSeverity(ConfigurationParameters configurationParameters) {
		return configurationParameters //
				.get(LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, value -> {
					try {
						return Severity.valueOf(value.toUpperCase(Locale.ROOT));
					}
					catch (Exception e) {
						throw new JUnitException(
							"Invalid DiscoveryIssue.Severity '%s' set via the '%s' configuration parameter.".formatted(
								value, LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME));
					}
				}) //
				.orElse(Severity.ERROR);
	}
}
