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

import static org.junit.platform.engine.SelectorResolutionResult.Status.FAILED;
import static org.junit.platform.engine.SelectorResolutionResult.Status.UNRESOLVED;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.DirectorySource;
import org.junit.platform.engine.support.descriptor.FilePosition;
import org.junit.platform.engine.support.descriptor.FileSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.descriptor.PackageSource;
import org.junit.platform.engine.support.descriptor.UriSource;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.LauncherDiscoveryListener;

class DiscoveryIssueCollector implements LauncherDiscoveryListener {

	private static final Logger logger = LoggerFactory.getLogger(DiscoveryIssueCollector.class);

	final List<DiscoveryIssue> issues = new ArrayList<>();
	private final ConfigurationParameters configurationParameters;

	DiscoveryIssueCollector(ConfigurationParameters configurationParameters) {
		this.configurationParameters = configurationParameters;
	}

	@Override
	public void engineDiscoveryStarted(UniqueId engineId) {
		this.issues.clear();
	}

	@Override
	public void selectorProcessed(UniqueId engineId, DiscoverySelector selector, SelectorResolutionResult result) {
		if (result.getStatus() == FAILED) {
			this.issues.add(DiscoveryIssue.builder(Severity.ERROR, selector + " resolution failed") //
					.cause(result.getThrowable()) //
					.source(toSourceSafely(selector)) //
					.build());
		}
		else if (result.getStatus() == UNRESOLVED && selector instanceof UniqueIdSelector uniqueIdSelector) {
			UniqueId uniqueId = uniqueIdSelector.getUniqueId();
			if (uniqueId.hasPrefix(engineId)) {
				this.issues.add(DiscoveryIssue.create(Severity.ERROR, selector + " could not be resolved"));
			}
		}
	}

	private static @Nullable TestSource toSourceSafely(DiscoverySelector selector) {
		try {
			return toSource(selector);
		}
		catch (Exception e) {
			logger.error(e, () -> "Failed to convert DiscoverySelector [%s] into TestSource".formatted(selector));
			return null;
		}
	}

	private static @Nullable TestSource toSource(DiscoverySelector selector) {
		if (selector instanceof ClassSelector classSelector) {
			return ClassSource.from(classSelector.getClassName());
		}
		if (selector instanceof MethodSelector methodSelector) {
			return MethodSource.from(methodSelector.getClassName(), methodSelector.getMethodName(),
				methodSelector.getParameterTypeNames());
		}
		if (selector instanceof ClasspathResourceSelector resourceSelector) {
			String resourceName = resourceSelector.getClasspathResourceName();
			return resourceSelector.getPosition() //
					.map(DiscoveryIssueCollector::convert) //
					.map(position -> ClasspathResourceSource.from(resourceName, position)) //
					.orElseGet(() -> ClasspathResourceSource.from(resourceName));
		}
		if (selector instanceof PackageSelector packageSelector) {
			return PackageSource.from(packageSelector.getPackageName());
		}
		if (selector instanceof FileSelector fileSelector) {
			return fileSelector.getPosition() //
					.map(DiscoveryIssueCollector::convert) //
					.map(position -> FileSource.from(fileSelector.getFile(), position)) //
					.orElseGet(() -> FileSource.from(fileSelector.getFile()));
		}
		if (selector instanceof DirectorySelector directorySelector) {
			return DirectorySource.from(directorySelector.getDirectory());
		}
		if (selector instanceof UriSelector uriSelector) {
			return UriSource.from(uriSelector.getUri());
		}
		return null;
	}

	private static FilePosition convert(org.junit.platform.engine.discovery.FilePosition position) {
		return position.getColumn() //
				.map(column -> FilePosition.from(position.getLine(), column)) //
				.orElseGet(() -> FilePosition.from(position.getLine()));
	}

	@Override
	public void issueEncountered(UniqueId engineId, DiscoveryIssue issue) {
		this.issues.add(issue);
	}

	DiscoveryIssueNotifier toNotifier() {
		if (this.issues.isEmpty()) {
			return DiscoveryIssueNotifier.NO_ISSUES;
		}
		return DiscoveryIssueNotifier.from(getCriticalSeverity(), this.issues);
	}

	private Severity getCriticalSeverity() {
		Severity defaultValue = Severity.ERROR;
		return this.configurationParameters //
				.get(LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, value -> {
					try {
						return Severity.valueOf(value.toUpperCase(Locale.ROOT));
					}
					catch (Exception e) {
						logger.warn(() -> """
								Invalid DiscoveryIssue.Severity '%s' set via the '%s' configuration parameter. \
								Falling back to the %s default value.""".formatted(value,
							LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, defaultValue));
						return defaultValue;
					}
				}) //
				.orElse(defaultValue);
	}
}
