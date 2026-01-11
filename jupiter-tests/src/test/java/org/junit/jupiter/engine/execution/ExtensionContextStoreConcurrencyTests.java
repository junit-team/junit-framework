/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

/**
 * Concurrency tests for {@link NamespaceAwareStore} and {@link NamespacedHierarchicalStore}.
 *
 * @since 5.0
 */
class ExtensionContextStoreConcurrencyTests {

	private final AtomicInteger count = new AtomicInteger();

	@Test
	void concurrentAccessToDefaultStoreWithoutParentStore() {
		// Run the actual test 100 times "for good measure".
		IntStream.range(1, 100).forEach(i -> {
			Store store = reset();
			// Simulate 100 extensions interacting concurrently with the Store.
			IntStream.range(1, 100).parallel().forEach(j -> store.computeIfAbsent("key", this::newValue));
			assertEquals(1, count.get(), () -> "number of times newValue() was invoked in run #" + i);
		});
	}

	private String newValue(String key) {
		count.incrementAndGet();
		return "value";
	}

	private Store reset() {
		count.set(0);
		return new NamespaceAwareStore(new NamespacedHierarchicalStore<>(null), Namespace.GLOBAL);
	}

}
