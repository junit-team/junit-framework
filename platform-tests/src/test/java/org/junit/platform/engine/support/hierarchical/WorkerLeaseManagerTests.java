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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.hierarchical.WorkerThreadPoolHierarchicalTestExecutorService.WorkerLeaseManager;

class WorkerLeaseManagerTests {

	@Test
	void releasingIsIdempotent() {
		var released = new AtomicInteger();
		var manager = new WorkerLeaseManager(1, __ -> released.incrementAndGet());

		var lease = manager.tryAcquire();
		assertThat(lease).isNotNull();

		lease.close();
		assertThat(released.get()).isEqualTo(1);

		lease.close();
		assertThat(released.get()).isEqualTo(1);
	}

	@Test
	void leaseCanBeReacquired() throws Exception {
		var released = new AtomicInteger();
		var manager = new WorkerLeaseManager(1, __ -> released.incrementAndGet());

		var lease = manager.tryAcquire();
		assertThat(lease).isNotNull();

		lease.close();
		assertThat(released.get()).isEqualTo(1);

		lease.reacquire();
		assertThat(released.get()).isEqualTo(1);

		lease.close();
		assertThat(released.get()).isEqualTo(2);
	}
}
