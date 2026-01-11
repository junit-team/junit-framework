/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

/**
 * The JUnit Platform Suite {@link org.junit.platform.engine.TestEngine TestEngine}.
 *
 * @since 1.8
 */
@API(status = INTERNAL, since = "1.8")
public final class SuiteTestEngine implements TestEngine {

	@Override
	public String getId() {
		return SuiteEngineDescriptor.ENGINE_ID;
	}

	/**
	 * Returns {@code org.junit.platform} as the group ID.
	 */
	@Override
	public Optional<String> getGroupId() {
		return Optional.of("org.junit.platform");
	}

	/**
	 * Returns {@code junit-platform-suite-engine} as the artifact ID.
	 */
	@Override
	public Optional<String> getArtifactId() {
		return Optional.of("junit-platform-suite-engine");
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		SuiteEngineDescriptor engineDescriptor = new SuiteEngineDescriptor(uniqueId);
		new DiscoverySelectorResolver().resolveSelectors(discoveryRequest, engineDescriptor);
		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		SuiteEngineDescriptor suiteEngineDescriptor = (SuiteEngineDescriptor) request.getRootTestDescriptor();
		EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();
		NamespacedHierarchicalStore<Namespace> requestLevelStore = request.getStore();
		CancellationToken cancellationToken = request.getCancellationToken();

		engineExecutionListener.executionStarted(suiteEngineDescriptor);

		// @formatter:off
		suiteEngineDescriptor.getChildren()
				.stream()
				.map(SuiteTestDescriptor.class::cast)
				.forEach(suiteTestDescriptor -> suiteTestDescriptor.execute(engineExecutionListener, requestLevelStore, cancellationToken));
		// @formatter:on
		engineExecutionListener.executionFinished(suiteEngineDescriptor, TestExecutionResult.successful());
	}

}
