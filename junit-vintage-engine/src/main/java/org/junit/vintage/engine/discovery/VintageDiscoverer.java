/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.platform.commons.support.scanning.ClassFilter;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.vintage.engine.Constants;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.descriptor.VintageEngineDescriptor;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class VintageDiscoverer {

	private static final IsPotentialJUnit4TestClass isPotentialJUnit4TestClass = new IsPotentialJUnit4TestClass();

	// @formatter:off
	private static final EngineDiscoveryRequestResolver<TestDescriptor> resolver = EngineDiscoveryRequestResolver.builder()
			.addClassContainerSelectorResolver(isPotentialJUnit4TestClass)
			.addSelectorResolver(context -> new ClassSelectorResolver(ClassFilter.of(context.getClassNameFilter(), isPotentialJUnit4TestClass)))
			.addSelectorResolver(new MethodSelectorResolver())
			.build();
	// @formatter:on

	public VintageEngineDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		VintageEngineDescriptor engineDescriptor = new VintageEngineDescriptor(uniqueId);
		resolver.resolve(discoveryRequest, engineDescriptor);
		RunnerTestDescriptorPostProcessor postProcessor = new RunnerTestDescriptorPostProcessor();
		for (TestDescriptor testDescriptor : engineDescriptor.getChildren()) {
			RunnerTestDescriptor runnerTestDescriptor = (RunnerTestDescriptor) testDescriptor;
			postProcessor.applyFiltersAndCreateDescendants(runnerTestDescriptor);
		}
		if (isDiscoveryIssueReportingEnabled(discoveryRequest) && !engineDescriptor.getChildren().isEmpty()) {
			var issue = DiscoveryIssue.create(DiscoveryIssue.Severity.INFO, //
				"The JUnit Vintage engine is deprecated and should only be " //
						+ "used temporarily while migrating tests to JUnit Jupiter or another testing " //
						+ "framework with native JUnit Platform support.");
			discoveryRequest.getDiscoveryListener().issueEncountered(uniqueId, issue);
		}
		return engineDescriptor;
	}

	@SuppressWarnings("deprecation")
	private static boolean isDiscoveryIssueReportingEnabled(EngineDiscoveryRequest discoveryRequest) {
		return discoveryRequest.getConfigurationParameters() //
				.getBoolean(Constants.DISCOVERY_ISSUE_REPORTING_ENABLED_PROPERTY_NAME) //
				.orElse(true);
	}

}
