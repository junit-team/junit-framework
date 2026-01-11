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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;

/**
 * @since 1.3
 */
class SingleLockTests {

	@Test
	@SuppressWarnings("resource")
	void acquire() throws Exception {
		var lock = new ReentrantLock();

		new SingleLock(anyResource(), lock).acquire();

		assertTrue(lock.isLocked());
	}

	@Test
	@SuppressWarnings("resource")
	void release() throws Exception {
		var lock = new ReentrantLock();

		new SingleLock(anyResource(), lock).acquire().close();

		assertFalse(lock.isLocked());
	}

	@Test
	@SuppressWarnings("resource")
	void tryAcquireAndRelease() {
		var lock = new ReentrantLock();

		var singleLock = new SingleLock(anyResource(), lock);

		singleLock.tryAcquire();
		assertTrue(lock.isLocked());

		singleLock.release();
		assertFalse(lock.isLocked());
	}

	private static ExclusiveResource anyResource() {
		return new ExclusiveResource("key", LockMode.READ);
	}

}
