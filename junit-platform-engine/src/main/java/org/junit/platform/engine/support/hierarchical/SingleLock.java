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

import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;

import org.junit.platform.commons.util.ToStringBuilder;

/**
 * @since 1.3
 */
class SingleLock implements ResourceLock {

	private final List<ExclusiveResource> resources;
	private final Lock lock;

	SingleLock(ExclusiveResource resource, Lock lock) {
		this.resources = List.of(resource);
		this.lock = lock;
	}

	@Override
	public List<ExclusiveResource> getResources() {
		return resources;
	}

	// for tests only
	Lock getLock() {
		return this.lock;
	}

	@Override
	public ResourceLock acquire() throws InterruptedException {
		ForkJoinPool.managedBlock(new SingleLockManagedBlocker());
		return this;
	}

	@Override
	public void release() {
		this.lock.unlock();
	}

	@Override
	public boolean isExclusive() {
		return resources.get(0).getLockMode() == ExclusiveResource.LockMode.READ_WRITE;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("resource", getOnlyElement(resources)) //
				.toString();
	}

	private class SingleLockManagedBlocker implements ForkJoinPool.ManagedBlocker {

		private volatile boolean acquired;

		@Override
		public boolean block() throws InterruptedException {
			if (!this.acquired) {
				SingleLock.this.lock.lockInterruptibly();
				this.acquired = true;
			}
			return true;
		}

		@Override
		public boolean isReleasable() {
			return this.acquired || (this.acquired = SingleLock.this.lock.tryLock());
		}

	}

}
