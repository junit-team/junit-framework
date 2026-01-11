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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ_WRITE;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode.READ;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @since 1.3
 */
class LockManager {

	private final Map<String, ReadWriteLock> locksByKey = new ConcurrentHashMap<>();
	private final SingleLock globalReadLock;
	private final SingleLock globalReadWriteLock;

	LockManager() {
		globalReadLock = new SingleLock(GLOBAL_READ, toLock(GLOBAL_READ));
		globalReadWriteLock = new SingleLock(GLOBAL_READ_WRITE, toLock(GLOBAL_READ_WRITE));
	}

	ResourceLock getLockForResources(Collection<ExclusiveResource> resources) {
		return toResourceLock(toDistinctSortedResources(resources));
	}

	ResourceLock getLockForResource(ExclusiveResource resource) {
		return toResourceLock(List.of(resource));
	}

	private List<ExclusiveResource> toDistinctSortedResources(Collection<ExclusiveResource> resources) {
		if (resources.isEmpty()) {
			return emptyList();
		}
		if (resources.size() == 1) {
			return List.of(getOnlyElement(resources));
		}
		// @formatter:off
		Map<String, List<ExclusiveResource>> resourcesByKey = resources.stream()
				.sorted(ExclusiveResource.COMPARATOR)
				.distinct()
				.collect(groupingBy(ExclusiveResource::getKey, LinkedHashMap::new, toList()));

		return resourcesByKey.values().stream()
				.map(resourcesWithSameKey -> resourcesWithSameKey.get(0))
				.toList();
		// @formatter:on
	}

	private ResourceLock toResourceLock(List<ExclusiveResource> resources) {
		return switch (resources.size()) {
			case 0 -> NopLock.INSTANCE;
			case 1 -> toSingleLock(getOnlyElement(resources));
			default -> new CompositeLock(resources, toLocks(resources));
		};
	}

	private SingleLock toSingleLock(ExclusiveResource resource) {
		if (GLOBAL_READ.equals(resource)) {
			return globalReadLock;
		}
		if (GLOBAL_READ_WRITE.equals(resource)) {
			return globalReadWriteLock;
		}
		return new SingleLock(resource, toLock(resource));
	}

	private List<Lock> toLocks(List<ExclusiveResource> resources) {
		return resources.stream().map(this::toLock).toList();
	}

	private Lock toLock(ExclusiveResource resource) {
		ReadWriteLock lock = this.locksByKey.computeIfAbsent(resource.getKey(), key -> new ReentrantReadWriteLock());
		return resource.getLockMode() == READ ? lock.readLock() : lock.writeLock();
	}

}
