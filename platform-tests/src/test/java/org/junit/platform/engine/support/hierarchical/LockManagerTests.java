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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_KEY;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode.READ;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode.READ_WRITE;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;

/**
 * @since 1.3
 */
class LockManagerTests {

	private final LockManager lockManager = new LockManager();

	@Test
	void returnsNopLockWithoutExclusiveResources() {
		Collection<ExclusiveResource> resources = Set.of();

		var locks = getLocks(resources, NopLock.class);

		assertThat(locks).isEmpty();
	}

	@Test
	void returnsSingleLockForSingleExclusiveResource() {
		Collection<ExclusiveResource> resources = Set.of(new ExclusiveResource("foo", READ));

		var locks = getLocks(resources, SingleLock.class);

		assertThat(locks).hasSize(1);
		assertThat(locks.getFirst()).isInstanceOf(ReadLock.class);
	}

	@Test
	void returnsCompositeLockForMultipleDifferentExclusiveResources() {
		Collection<ExclusiveResource> resources = List.of( //
			new ExclusiveResource("a", READ), //
			new ExclusiveResource("b", READ_WRITE));

		var locks = getLocks(resources, CompositeLock.class);

		assertThat(locks).hasSize(2);
		assertThat(locks.get(0)).isInstanceOf(ReadLock.class);
		assertThat(locks.get(1)).isInstanceOf(WriteLock.class);
	}

	@Test
	void reusesSameLockForExclusiveResourceWithSameKey() {
		Collection<ExclusiveResource> resources = Set.of(new ExclusiveResource("foo", READ));

		var locks1 = getLocks(resources, SingleLock.class);
		var locks2 = getLocks(resources, SingleLock.class);

		assertThat(locks1).hasSize(1);
		assertThat(locks2).hasSize(1);
		assertThat(locks1.getFirst()).isSameAs(locks2.getFirst());
	}

	@Test
	void returnsWriteLockForExclusiveResourceWithBothLockModes() {
		Collection<ExclusiveResource> resources = List.of( //
			new ExclusiveResource("bar", READ), //
			new ExclusiveResource("foo", READ), //
			new ExclusiveResource("foo", READ_WRITE), //
			new ExclusiveResource("bar", READ_WRITE));

		var locks = getLocks(resources, CompositeLock.class);

		assertThat(locks).hasSize(2);
		assertThat(locks.get(0)).isInstanceOf(WriteLock.class);
		assertThat(locks.get(1)).isInstanceOf(WriteLock.class);
	}

	@ParameterizedTest
	@EnumSource
	void globalLockComesFirst(LockMode globalLockMode) {
		Collection<ExclusiveResource> resources = List.of( //
			new ExclusiveResource("___foo", READ), //
			new ExclusiveResource("foo", READ_WRITE), //
			new ExclusiveResource(GLOBAL_KEY, globalLockMode), //
			new ExclusiveResource("bar", READ_WRITE));

		var locks = getLocks(resources, CompositeLock.class);

		assertThat(locks).hasSize(4);
		assertThat(locks.get(0)).isEqualTo(getSingleLock(GLOBAL_KEY, globalLockMode));
		assertThat(locks.get(1)).isEqualTo(getSingleLock("___foo", READ));
		assertThat(locks.get(2)).isEqualTo(getSingleLock("bar", READ_WRITE));
		assertThat(locks.get(3)).isEqualTo(getSingleLock("foo", READ_WRITE));
	}

	@Test
	void usesSingleInstanceForGlobalReadLock() {
		var lock = lockManager.getLockForResources(List.of(ExclusiveResource.GLOBAL_READ));

		assertThat(lock) //
				.isInstanceOf(SingleLock.class) //
				.isSameAs(lockManager.getLockForResource(ExclusiveResource.GLOBAL_READ));
	}

	@Test
	void usesSingleInstanceForGlobalReadWriteLock() {
		var lock = lockManager.getLockForResources(List.of(ExclusiveResource.GLOBAL_READ_WRITE));

		assertThat(lock) //
				.isInstanceOf(SingleLock.class) //
				.isSameAs(lockManager.getLockForResource(ExclusiveResource.GLOBAL_READ_WRITE));
	}

	private Lock getSingleLock(String key, LockMode lockMode) {
		return getLocks(Set.of(new ExclusiveResource(key, lockMode)), SingleLock.class).getFirst();
	}

	private List<Lock> getLocks(Collection<ExclusiveResource> resources, Class<? extends ResourceLock> type) {
		var lock = lockManager.getLockForResources(resources);
		assertThat(lock).isInstanceOf(type);
		return ResourceLockSupport.getLocks(lock);
	}

}
