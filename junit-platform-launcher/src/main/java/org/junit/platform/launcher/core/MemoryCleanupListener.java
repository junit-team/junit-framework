/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.platform.launcher.LauncherConstants.MEMORY_CLEANUP_EXCLUDED_ENGINES_PROPERTY_NAME;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 6.1
 */
class MemoryCleanupListener extends DelegatingEngineExecutionListener {

	private final TestPlan testPlan;
	private final Set<String> excludedEngineIds;

	MemoryCleanupListener(EngineExecutionListener delegate, TestPlan testPlan) {
		super(delegate);
		this.testPlan = testPlan;
		this.excludedEngineIds = parseExcludedEngines(testPlan.getConfigurationParameters());
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		super.executionSkipped(testDescriptor, reason);
		cleanUp(testDescriptor);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		super.executionFinished(testDescriptor, testExecutionResult);
		cleanUp(testDescriptor);
	}

	private Set<String> parseExcludedEngines(ConfigurationParameters parameters) {
		return parameters.get(MEMORY_CLEANUP_EXCLUDED_ENGINES_PROPERTY_NAME, MemoryCleanupListener::parseEngineIds) //
				.orElse(Set.of());
	}

	private static Set<String> parseEngineIds(String value) {
		return Arrays.stream(value.split(",")) //
				.map(String::strip) //
				.filter(s -> !s.isEmpty()) //
				.collect(toUnmodifiableSet());
	}

	private void cleanUp(TestDescriptor testDescriptor) {
		testPlan.removeInternal(testDescriptor.getUniqueId());
		if (!testDescriptor.isRoot()) {
			// Find the closest engine ID in the descriptor hierarchy
			Optional<String> engineId = findClosestEngineId(testDescriptor.getUniqueId());
			// Only call removeFromHierarchy() if the engine is not excluded
			if (engineId.isEmpty() || !excludedEngineIds.contains(engineId.get())) {
				testDescriptor.removeFromHierarchy();
			}
		}
	}

	/**
	 * Find the closest engine ID in the test descriptor's unique ID hierarchy.
	 *
	 * <p>Traverses the segments of the descriptor's unique ID from last to first,
	 * looking for a segment of type "engine".
	 */
	private static Optional<String> findClosestEngineId(UniqueId uniqueId) {
		var segments = uniqueId.getSegments();
		for (int i = segments.size() - 1; i >= 0; i--) {
			var segment = segments.get(i);
			if (segment.isEngine()) {
				return Optional.of(segment.getValue());
			}
		}
		return Optional.empty();
	}
}
