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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * A lock for a one or more resources.
 *
 * @since 1.3
 * @see HierarchicalTestExecutorService.TestTask#getResourceLock()
 */
@API(status = STABLE, since = "1.10")
public interface ResourceLock extends AutoCloseable {

	/**
	 * Try to acquire this resource lock, without blocking.
	 *
	 * @return {@code true} if the lock was acquired and {@code false} otherwise
	 * @since 6.1
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	default boolean tryAcquire() {
		return false;
	}

	/**
	 * Acquire this resource lock, potentially blocking.
	 *
	 * @return this lock so it can easily be used in a try-with-resources
	 * statement.
	 * @throws InterruptedException if the calling thread is interrupted
	 * while waiting to acquire this lock
	 */
	ResourceLock acquire() throws InterruptedException;

	/**
	 * Release this resource lock.
	 */
	void release();

	@Override
	default void close() {
		release();
	}

	/**
	 * {@return the exclusive resources this lock represents}
	 */
	List<ExclusiveResource> getResources();

	/**
	 * {@return whether this lock requires exclusiveness}
	 */
	boolean isExclusive();

	/**
	 * {@return whether the given lock is compatible with this lock}
	 * @param other the other lock to check for compatibility
	 */
	default boolean isCompatible(ResourceLock other) {

		List<ExclusiveResource> ownResources = this.getResources();
		List<ExclusiveResource> otherResources = other.getResources();

		if (ownResources.isEmpty() || otherResources.isEmpty()) {
			return true;
		}

		// Whenever there's a READ_WRITE lock, it's incompatible with any other lock
		// because we guarantee that all children will have exclusive access to the
		// resource in question. In practice, whenever a READ_WRITE lock is present,
		// NodeTreeWalker will force all children to run in the same thread so that
		// it should never attempt to steal work from another thread, and we shouldn't
		// actually reach this point.
		// The global read lock (which is always on direct children of the engine node)
		// needs special treatment so that it is compatible with the first write lock
		// (which may be on a test method).
		boolean isGlobalReadLock = ownResources.size() == 1
				&& ExclusiveResource.GLOBAL_READ.equals(ownResources.get(0));
		if ((!isGlobalReadLock && other.isExclusive()) || this.isExclusive()) {
			return false;
		}

		Optional<ExclusiveResource> potentiallyDeadlockCausingAdditionalResource = otherResources.stream() //
				.filter(resource -> !ownResources.contains(resource)) //
				.findFirst() //
				.filter(resource -> ExclusiveResource.COMPARATOR.compare(resource,
					ownResources.get(ownResources.size() - 1)) < 0);

		return potentiallyDeadlockCausingAdditionalResource.isEmpty();
	}
}
