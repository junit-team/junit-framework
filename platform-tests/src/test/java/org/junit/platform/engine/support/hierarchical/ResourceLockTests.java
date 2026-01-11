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
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ_WRITE;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;

class ResourceLockTests {

	@Test
	void nopLocks() {
		assertCompatible(nopLock(), nopLock());
		assertCompatible(nopLock(), singleLock(anyReadOnlyResource()));
		assertCompatible(nopLock(), compositeLock(anyReadOnlyResource()));
	}

	@Test
	void readOnlySingleLocks() {
		ExclusiveResource bR = readOnlyResource("b");

		assertCompatible(singleLock(bR), nopLock());
		assertCompatible(singleLock(bR), singleLock(bR));
		assertIncompatible(singleLock(bR), singleLock(readWriteResource("b")), "read-write conflict");
		assertIncompatible(singleLock(bR), singleLock(readOnlyResource("a")), "lock acquisition order");
		assertCompatible(singleLock(bR), singleLock(readOnlyResource("c")));
		assertIncompatible(singleLock(bR), singleLock(GLOBAL_READ), "lock acquisition order");
		assertIncompatible(singleLock(bR), singleLock(GLOBAL_READ_WRITE), "lock acquisition order");
		assertCompatible(singleLock(bR), compositeLock(bR, readOnlyResource("c")));
		assertIncompatible(singleLock(bR), compositeLock(readOnlyResource("a1"), readOnlyResource("a2"), bR),
			"lock acquisition order");
	}

	@Test
	void readWriteSingleLocks() {
		ExclusiveResource bRW = readWriteResource("b");

		assertCompatible(singleLock(bRW), nopLock());
		assertIncompatible(singleLock(bRW), singleLock(bRW), "isolation guarantees");
		assertIncompatible(singleLock(bRW), compositeLock(bRW), "isolation guarantees");
		assertIncompatible(singleLock(bRW), singleLock(readOnlyResource("a")), "lock acquisition order");
		assertIncompatible(singleLock(bRW), singleLock(readOnlyResource("b")), "isolation guarantees");
		assertIncompatible(singleLock(bRW), singleLock(readOnlyResource("c")), "isolation guarantees");
		assertIncompatible(singleLock(bRW), singleLock(GLOBAL_READ), "lock acquisition order");
		assertIncompatible(singleLock(bRW), singleLock(GLOBAL_READ_WRITE), "lock acquisition order");
		assertIncompatible(singleLock(bRW), compositeLock(bRW, readOnlyResource("c")), "isolation guarantees");
		assertIncompatible(singleLock(bRW), compositeLock(readOnlyResource("a1"), readOnlyResource("a2"), bRW),
			"lock acquisition order");
	}

	@Test
	void globalReadLock() {
		assertCompatible(singleLock(GLOBAL_READ), nopLock());
		assertCompatible(singleLock(GLOBAL_READ), singleLock(GLOBAL_READ));
		assertCompatible(singleLock(GLOBAL_READ), singleLock(anyReadOnlyResource()));
		assertCompatible(singleLock(GLOBAL_READ), singleLock(anyReadWriteResource()));
		assertIncompatible(singleLock(GLOBAL_READ), singleLock(GLOBAL_READ_WRITE), "read-write conflict");
	}

	@Test
	void readOnlyCompositeLocks() {
		ExclusiveResource bR = readOnlyResource("b");

		assertCompatible(compositeLock(bR), nopLock());
		assertCompatible(compositeLock(bR), singleLock(bR));
		assertCompatible(compositeLock(bR), compositeLock(bR));
		assertIncompatible(compositeLock(bR), singleLock(GLOBAL_READ), "lock acquisition order");
		assertIncompatible(compositeLock(bR), singleLock(GLOBAL_READ_WRITE), "lock acquisition order");
		assertIncompatible(compositeLock(bR), compositeLock(readOnlyResource("a")), "lock acquisition order");
		assertCompatible(compositeLock(bR), compositeLock(readOnlyResource("c")));
		assertIncompatible(compositeLock(bR), compositeLock(readWriteResource("b")), "read-write conflict");
		assertIncompatible(compositeLock(bR), compositeLock(bR, readWriteResource("b")), "read-write conflict");
	}

	@Test
	void readWriteCompositeLocks() {
		ExclusiveResource bRW = readWriteResource("b");

		assertCompatible(compositeLock(bRW), nopLock());
		assertIncompatible(compositeLock(bRW), singleLock(bRW), "isolation guarantees");
		assertIncompatible(compositeLock(bRW), compositeLock(bRW), "isolation guarantees");
		assertIncompatible(compositeLock(bRW), singleLock(readOnlyResource("a")), "lock acquisition order");
		assertIncompatible(compositeLock(bRW), singleLock(readOnlyResource("b")), "isolation guarantees");
		assertIncompatible(compositeLock(bRW), singleLock(readOnlyResource("c")), "isolation guarantees");
		assertIncompatible(compositeLock(bRW), singleLock(GLOBAL_READ), "lock acquisition order");
		assertIncompatible(compositeLock(bRW), singleLock(GLOBAL_READ_WRITE), "lock acquisition order");
		assertIncompatible(compositeLock(bRW), compositeLock(readOnlyResource("a")), "lock acquisition order");
		assertIncompatible(compositeLock(bRW), compositeLock(readOnlyResource("b"), readOnlyResource("c")),
			"isolation guarantees");
	}

	private static void assertCompatible(ResourceLock first, ResourceLock second) {
		assertTrue(first.isCompatible(second),
			"Expected locks to be compatible:\n(1) %s\n(2) %s".formatted(first, second));
	}

	private static void assertIncompatible(ResourceLock first, ResourceLock second, String reason) {
		assertFalse(first.isCompatible(second),
			"Expected locks to be incompatible due to %s:\n(1) %s\n(2) %s".formatted(reason, first, second));
	}

	private static ResourceLock nopLock() {
		return NopLock.INSTANCE;
	}

	private static SingleLock singleLock(ExclusiveResource resource) {
		return new SingleLock(resource, anyLock());
	}

	private static CompositeLock compositeLock(ExclusiveResource... resources) {
		return new CompositeLock(List.of(resources), Arrays.stream(resources).map(__ -> anyLock()).toList());
	}

	private static ExclusiveResource anyReadOnlyResource() {
		return readOnlyResource("key");
	}

	private static ExclusiveResource anyReadWriteResource() {
		return readWriteResource("key");
	}

	private static ExclusiveResource readOnlyResource(String key) {
		return new ExclusiveResource(key, LockMode.READ);
	}

	private static ExclusiveResource readWriteResource(String key) {
		return new ExclusiveResource(key, LockMode.READ_WRITE);
	}

	private static Lock anyLock() {
		return new ReentrantLock();
	}
}
