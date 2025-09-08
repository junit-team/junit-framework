/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.LauncherStoreFacade;
import org.junit.jupiter.engine.support.JupiterThrowableCollectorFactory;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.hierarchical.ForkJoinPoolHierarchicalTestExecutorService;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * The JUnit Jupiter {@link org.junit.platform.engine.TestEngine TestEngine}.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public final class JupiterTestEngine extends HierarchicalTestEngine<JupiterEngineExecutionContext> {

	@Override
	public String getId() {
		return JupiterEngineDescriptor.ENGINE_ID;
	}

	/**
	 * Returns {@code org.junit.jupiter} as the group ID.
	 */
	@Override
	public Optional<String> getGroupId() {
		return Optional.of("org.junit.jupiter");
	}

	/**
	 * Returns {@code junit-jupiter-engine} as the artifact ID.
	 */
	@Override
	public Optional<String> getArtifactId() {
		return Optional.of("junit-jupiter-engine");
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		DiscoveryIssueReporter issueReporter = DiscoveryIssueReporter.deduplicating(
			DiscoveryIssueReporter.forwarding(discoveryRequest.getDiscoveryListener(), uniqueId));
		JupiterConfiguration configuration = new CachingJupiterConfiguration(
			new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters(),
				discoveryRequest.getOutputDirectoryCreator(), issueReporter));
		JupiterEngineDescriptor engineDescriptor = new JupiterEngineDescriptor(uniqueId, configuration);
		DiscoverySelectorResolver.resolveSelectors(discoveryRequest, engineDescriptor, issueReporter);
		return engineDescriptor;
	}

	@Override
	protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
		JupiterConfiguration configuration = getJupiterConfiguration(request);
		if (configuration.isParallelExecutionEnabled()) {
			return new ForkJoinPoolHierarchicalTestExecutorService(new PrefixedConfigurationParameters(
				request.getConfigurationParameters(), Constants.PARALLEL_CONFIG_PREFIX));
		}
		return super.createExecutorService(request);
	}

	@Override
	protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
		return new JupiterEngineExecutionContext(request.getEngineExecutionListener(), getJupiterConfiguration(request),
			new LauncherStoreFacade(request.getStore()));
	}

	/**
	 * @since 5.4
	 */
	@Override
	protected ThrowableCollector.Factory createThrowableCollectorFactory(ExecutionRequest request) {
		return JupiterThrowableCollectorFactory::createThrowableCollector;
	}

	private JupiterConfiguration getJupiterConfiguration(ExecutionRequest request) {
		JupiterEngineDescriptor engineDescriptor = (JupiterEngineDescriptor) request.getRootTestDescriptor();
		return engineDescriptor.getConfiguration();
	}

}
