/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

/**
 * @since 1.3.1
 */
class NodeExecutionAdvisor {

	private final Map<TestDescriptor, ExecutionMode> forcedDescendantExecutionModeByTestDescriptor = new HashMap<>();
	private final Map<TestDescriptor, ResourceLock> resourceLocksByTestDescriptor = new HashMap<>();

	void forceDescendantExecutionMode(TestDescriptor testDescriptor, ExecutionMode executionMode) {
		forcedDescendantExecutionModeByTestDescriptor.put(testDescriptor, executionMode);
	}

	void useResourceLock(TestDescriptor testDescriptor, ResourceLock resourceLock) {
		resourceLocksByTestDescriptor.put(testDescriptor, resourceLock);
	}

	void removeResourceLock(TestDescriptor testDescriptor) {
		resourceLocksByTestDescriptor.remove(testDescriptor);
	}

	Optional<ExecutionMode> getForcedExecutionMode(TestDescriptor testDescriptor) {
		return testDescriptor.getParent().flatMap(this::lookupExecutionModeForcedByAncestor);
	}

	private Optional<ExecutionMode> lookupExecutionModeForcedByAncestor(TestDescriptor testDescriptor) {
		ExecutionMode value = forcedDescendantExecutionModeByTestDescriptor.get(testDescriptor);
		if (value != null) {
			return Optional.of(value);
		}
		return testDescriptor.getParent().flatMap(this::lookupExecutionModeForcedByAncestor);
	}

	ResourceLock getResourceLock(TestDescriptor testDescriptor) {
		return resourceLocksByTestDescriptor.getOrDefault(testDescriptor, NopLock.INSTANCE);
	}
}
