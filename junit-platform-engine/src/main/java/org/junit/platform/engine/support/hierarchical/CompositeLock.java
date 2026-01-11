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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * @since 1.3
 */
class CompositeLock implements ResourceLock {

	private final List<ExclusiveResource> resources;
	private final List<Lock> locks;
	private final boolean exclusive;

	CompositeLock(List<ExclusiveResource> resources, List<Lock> locks) {
		Preconditions.condition(resources.size() == locks.size(), "Resources and locks must have the same size");
		this.resources = List.copyOf(resources);
		this.locks = Preconditions.notEmpty(locks, "Locks must not be empty");
		this.exclusive = resources.stream().anyMatch(
			resource -> resource.getLockMode() == ExclusiveResource.LockMode.READ_WRITE);
	}

	@Override
	public List<ExclusiveResource> getResources() {
		return resources;
	}

	// for tests only
	List<Lock> getLocks() {
		return this.locks;
	}

	@Override
	public boolean tryAcquire() {
		List<Lock> acquiredLocks = new ArrayList<>(this.locks.size());
		for (Lock lock : this.locks) {
			if (lock.tryLock()) {
				acquiredLocks.add(lock);
			}
			else {
				break;
			}
		}
		if (acquiredLocks.size() == this.locks.size()) {
			return true;
		}
		else {
			release(acquiredLocks);
			return false;
		}
	}

	@Override
	public ResourceLock acquire() throws InterruptedException {
		ForkJoinPool.managedBlock(new CompositeLockManagedBlocker());
		return this;
	}

	private void acquireAllLocks() throws InterruptedException {
		List<Lock> acquiredLocks = new ArrayList<>(this.locks.size());
		try {
			for (Lock lock : this.locks) {
				lock.lockInterruptibly();
				acquiredLocks.add(lock);
			}
		}
		catch (InterruptedException e) {
			release(acquiredLocks);
			throw e;
		}
	}

	@Override
	public void release() {
		release(this.locks);
	}

	private void release(List<Lock> acquiredLocks) {
		for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
			acquiredLocks.get(i).unlock();
		}
	}

	@Override
	public boolean isExclusive() {
		return exclusive;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("resources", resources) //
				.toString();
	}

	private class CompositeLockManagedBlocker implements ForkJoinPool.ManagedBlocker {

		private volatile boolean acquired;

		@Override
		public boolean block() throws InterruptedException {
			if (!this.acquired) {
				acquireAllLocks();
				this.acquired = true;
			}
			return true;
		}

		@Override
		public boolean isReleasable() {
			return this.acquired;
		}

	}

}
