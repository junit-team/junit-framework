/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.TestDescriptor;

/**
 * {@code EngineDiscoveryResults} represents the results of test discovery
 * by a {@link org.junit.platform.engine.TestEngine TestEngine} on the JUnit
 * Platform and provides access to the {@link TestDescriptor} of the engine
 * and any {@link DiscoveryIssue DiscoveryIssues} that were encountered.
 *
 * @since 1.13
 */
@API(status = EXPERIMENTAL, since = "6.0")
public class EngineDiscoveryResults {

	private final TestDescriptor engineDescriptor;
	private final List<DiscoveryIssue> discoveryIssues;

	EngineDiscoveryResults(TestDescriptor engineDescriptor, List<DiscoveryIssue> discoveryIssues) {
		this.engineDescriptor = Preconditions.notNull(engineDescriptor, "Engine descriptor must not be null");
		this.discoveryIssues = List.copyOf(
			Preconditions.notNull(discoveryIssues, "Discovery issues list must not be null"));
		Preconditions.containsNoNullElements(discoveryIssues, "Discovery issues list must not contain null elements");
	}

	/**
	 * {@return the root {@link TestDescriptor} of the engine}
	 */
	public TestDescriptor getEngineDescriptor() {
		return engineDescriptor;
	}

	/**
	 * {@return the issues that were encountered during discovery}
	 */
	public List<DiscoveryIssue> getDiscoveryIssues() {
		return discoveryIssues;
	}

}
