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

import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.launcher.LauncherConstants.MEMORY_CLEANUP_EXCLUDED_ENGINES_PROPERTY_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestPlan;
import org.mockito.verification.VerificationMode;

class MemoryCleanupListenerTests {

	EngineExecutionListener delegate = mock();
	TestPlan testPlan = mock();
	Map<String, String> configParams = new HashMap<>();

	@Test
	void removesNonExcludedEngineDescriptorFromHierarchy() {
		assertRemovedFromEngine(UniqueId.forEngine("junit-jupiter").append("class", "TestClass"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "junit-vintage", " junit-vintage", "junit-vintage " })
	void doesNotRemoveExcludedEngineDescriptorFromHierarchy(String excludes) {
		configParams.put(MEMORY_CLEANUP_EXCLUDED_ENGINES_PROPERTY_NAME, excludes);

		assertNotRemovedFromEngine(UniqueId.forEngine("junit-vintage").append("class", "TestClass"));
	}

	@Test
	void doesNotRemoveExcludedEngineWithWhitespace() {
		configParams.put(MEMORY_CLEANUP_EXCLUDED_ENGINES_PROPERTY_NAME, " junit-vintage , junit-jupiter ");

		assertNotRemovedFromEngine(UniqueId.forEngine("junit-vintage").append("class", "TestClass"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "junit-vintage,junit-jupiter", " junit-vintage , junit-jupiter " })
	void excludesMultipleEngines(String excludes) {
		configParams.put(MEMORY_CLEANUP_EXCLUDED_ENGINES_PROPERTY_NAME, excludes);

		assertNotRemovedFromEngine(UniqueId.forEngine("junit-vintage").append("class", "TestClass"));
		assertNotRemovedFromEngine(UniqueId.forEngine("junit-jupiter").append("class", "TestClass"));
		assertRemovedFromEngine(UniqueId.forEngine("custom-engine").append("class", "TestClass"));
	}

	@Test
	void findsClosestEngineIdFromNestedDescriptor() {
		configParams.put(MEMORY_CLEANUP_EXCLUDED_ENGINES_PROPERTY_NAME, "junit-vintage");

		var suiteEngineUniqueId = UniqueId.forEngine("junit-suite");
		var vintageEngineUniqueId = suiteEngineUniqueId.append("engine", "junit-vintage");
		var testUniqueId = vintageEngineUniqueId.append("class", "TestClass");

		assertNotRemovedFromEngine(testUniqueId);
	}

	@Test
	void executionSkippedAlsoPerformsCleanup() {
		assertRemovedFromEngine(UniqueId.forEngine("junit-jupiter").append("class", "TestClass"),
			(listener, testDescriptor) -> listener.executionSkipped(testDescriptor, "reason"));
	}

	@Test
	void doesNotRemoveRootDescriptor() {
		var uniqueId = UniqueId.forEngine("junit-jupiter");
		assertRemoveFromHierarchyCalled(uniqueId, true, never());
	}

	private void assertRemovedFromEngine(UniqueId uniqueId) {
		assertRemovedFromEngine(uniqueId,
			(listener, testDescriptor) -> listener.executionFinished(testDescriptor, successful()));
	}

	private void assertRemovedFromEngine(UniqueId uniqueId, BiConsumer<EngineExecutionListener, TestDescriptor> call) {
		assertRemoveFromHierarchyCalled(uniqueId, false, times(1), call);
	}

	private void assertNotRemovedFromEngine(UniqueId uniqueId) {
		assertRemoveFromHierarchyCalled(uniqueId, false, never());
	}

	private void assertRemoveFromHierarchyCalled(UniqueId uniqueId, boolean root, VerificationMode mode) {
		assertRemoveFromHierarchyCalled(uniqueId, root, mode,
			(listener, testDescriptor) -> listener.executionFinished(testDescriptor, successful()));
	}

	private void assertRemoveFromHierarchyCalled(UniqueId uniqueId, boolean root, VerificationMode mode,
			BiConsumer<EngineExecutionListener, TestDescriptor> call) {
		var testDescriptor = mock(TestDescriptor.class);
		when(testDescriptor.getUniqueId()).thenReturn(uniqueId);
		when(testDescriptor.isRoot()).thenReturn(root);

		ConfigurationParameters parameters = mock();
		when(parameters.get(anyString(), any())).thenCallRealMethod();
		when(parameters.get(anyString())).thenAnswer(invocation -> {
			String key = (String) invocation.getArguments()[0];
			return Optional.ofNullable(configParams.get(key));
		});

		when(testPlan.getConfigurationParameters()).thenReturn(parameters);

		var listener = new MemoryCleanupListener(delegate, testPlan);
		call.accept(listener, testDescriptor);

		verify(testDescriptor, mode).removeFromHierarchy();
		verify(testPlan).removeInternal(uniqueId);
	}

}
