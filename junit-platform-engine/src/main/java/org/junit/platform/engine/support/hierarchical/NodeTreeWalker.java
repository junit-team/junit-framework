/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ_WRITE;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;
import static org.junit.platform.engine.support.hierarchical.NodeUtils.asNode;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;

/**
 * @since 1.3
 */
class NodeTreeWalker {

	private final LockManager lockManager;
	private final ResourceLock globalReadLock;
	private final ResourceLock globalReadWriteLock;

	NodeTreeWalker() {
		this(new LockManager());
	}

	NodeTreeWalker(LockManager lockManager) {
		this.lockManager = lockManager;
		this.globalReadLock = lockManager.getLockForResource(GLOBAL_READ);
		this.globalReadWriteLock = lockManager.getLockForResource(GLOBAL_READ_WRITE);
	}

	NodeExecutionAdvisor walk(TestDescriptor rootDescriptor) {
		Preconditions.condition(getExclusiveResources(rootDescriptor).isEmpty(),
			"Engine descriptor must not declare exclusive resources");
		NodeExecutionAdvisor advisor = new NodeExecutionAdvisor();
		rootDescriptor.getChildren().forEach(child -> walk(nullUnlessRequiresGlobalReadLock(child), child, advisor));
		return advisor;
	}

	private void walk(@Nullable TestDescriptor globalLockDescriptor, TestDescriptor testDescriptor,
			NodeExecutionAdvisor advisor) {

		if (globalLockDescriptor != null && advisor.getResourceLock(globalLockDescriptor) == globalReadWriteLock) {
			// Global read-write lock is already being enforced, so no additional locks are needed
			return;
		}

		Set<ExclusiveResource> exclusiveResources = getExclusiveResources(testDescriptor);
		if (exclusiveResources.isEmpty()) {
			if (globalLockDescriptor != null && globalLockDescriptor.equals(testDescriptor)) {
				advisor.useResourceLock(globalLockDescriptor, globalReadLock);
			}
			testDescriptor.getChildren().forEach(child -> {
				var newGlobalLockDescriptor = globalLockDescriptor == null //
						? nullUnlessRequiresGlobalReadLock(child) //
						: globalLockDescriptor;
				walk(newGlobalLockDescriptor, child, advisor);
			});
		}
		else {
			Preconditions.notNull(globalLockDescriptor,
				() -> "Node requiring exclusive resources must also require global read lock: " + testDescriptor);

			Set<ExclusiveResource> allResources = new HashSet<>(exclusiveResources);
			if (isReadOnly(allResources)) {
				doForChildrenRecursively(testDescriptor, child -> allResources.addAll(getExclusiveResources(child)));
				if (!isReadOnly(allResources)) {
					forceDescendantExecutionModeRecursively(advisor, testDescriptor);
				}
			}
			else {
				advisor.forceDescendantExecutionMode(testDescriptor, SAME_THREAD);
				doForChildrenRecursively(testDescriptor, child -> {
					allResources.addAll(getExclusiveResources(child));
					advisor.forceDescendantExecutionMode(child, SAME_THREAD);
				});
			}
			if (allResources.contains(GLOBAL_READ_WRITE)) {
				advisor.forceDescendantExecutionMode(globalLockDescriptor, SAME_THREAD);
				doForChildrenRecursively(globalLockDescriptor, child -> {
					advisor.forceDescendantExecutionMode(child, SAME_THREAD);
					// Remove any locks that may have been set for siblings or their descendants
					advisor.removeResourceLock(child);
				});
				advisor.useResourceLock(globalLockDescriptor, globalReadWriteLock);
			}
			else {
				if (globalLockDescriptor.equals(testDescriptor)) {
					allResources.add(GLOBAL_READ);
				}
				else {
					allResources.remove(GLOBAL_READ);
				}
				advisor.useResourceLock(testDescriptor, lockManager.getLockForResources(allResources));
			}
		}
	}

	private void forceDescendantExecutionModeRecursively(NodeExecutionAdvisor advisor, TestDescriptor testDescriptor) {
		advisor.forceDescendantExecutionMode(testDescriptor, SAME_THREAD);
		doForChildrenRecursively(testDescriptor, child -> advisor.forceDescendantExecutionMode(child, SAME_THREAD));
	}

	private boolean isReadOnly(Set<ExclusiveResource> exclusiveResources) {
		return exclusiveResources.stream().allMatch(it -> it.getLockMode() == ExclusiveResource.LockMode.READ);
	}

	private Set<ExclusiveResource> getExclusiveResources(TestDescriptor testDescriptor) {
		return asNode(testDescriptor).getExclusiveResources();
	}

	private static @Nullable TestDescriptor nullUnlessRequiresGlobalReadLock(TestDescriptor testDescriptor) {
		return asNode(testDescriptor).isGlobalReadLockRequired() ? testDescriptor : null;
	}

	private void doForChildrenRecursively(TestDescriptor parent, Consumer<TestDescriptor> consumer) {
		parent.getChildren().forEach(child -> {
			consumer.accept(child);
			doForChildrenRecursively(child, consumer);
		});
	}

}
