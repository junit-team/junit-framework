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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStoreException;

/**
 * Unit tests for {@link NamespaceAwareStore} and {@link NamespacedHierarchicalStore}.
 *
 * @since 5.5
 * @see ExtensionContextStoreConcurrencyTests
 * @see ExtensionValuesStoreTests
 */
class ExtensionContextStoreTests {

	private static final String KEY = "key";
	private static final String VALUE = "value";

	private final NamespacedHierarchicalStore<Namespace> parentStore = new NamespacedHierarchicalStore<>(null);
	private final NamespacedHierarchicalStore<Namespace> localStore = new NamespacedHierarchicalStore<>(parentStore);
	private final Store store = new NamespaceAwareStore(localStore, Namespace.GLOBAL);

	@Test
	void getOrDefaultWithNoValuePresent() {
		assertThat(store.get(KEY)).isNull();

		assertThat(store.getOrDefault(KEY, boolean.class, true)).isTrue();
		assertThat(store.getOrDefault(KEY, String.class, VALUE)).isEqualTo(VALUE);
	}

	@Test
	void getOrDefaultRequestingIncompatibleType() {
		localStore.put(Namespace.GLOBAL, KEY, VALUE);
		assertThat(store.get(KEY)).isEqualTo(VALUE);

		Exception exception = assertThrows(ExtensionContextException.class,
			() -> store.getOrDefault(KEY, boolean.class, true));
		assertThat(exception) //
				.hasMessageContaining("is not of required type") //
				.hasCauseInstanceOf(NamespacedHierarchicalStoreException.class) //
				.hasStackTraceContaining(NamespacedHierarchicalStore.class.getName());
	}

	@Test
	void getOrDefaultWithValueInLocalStore() {
		localStore.put(Namespace.GLOBAL, KEY, VALUE);
		assertThat(store.get(KEY)).isEqualTo(VALUE);

		assertThat(store.getOrDefault(KEY, String.class, VALUE)).isEqualTo(VALUE);
	}

	@Test
	void getOrDefaultWithValueInParentStore() {
		parentStore.put(Namespace.GLOBAL, KEY, VALUE);
		assertThat(store.get(KEY)).isEqualTo(VALUE);

		assertThat(store.getOrDefault(KEY, String.class, VALUE)).isEqualTo(VALUE);
	}

	@SuppressWarnings("deprecation")
	@Test
	void getOrComputeIfAbsentWithFailingCreator() {
		var invocations = new AtomicInteger();

		var e1 = assertThrows(RuntimeException.class, () -> store.getOrComputeIfAbsent(KEY, __ -> {
			invocations.incrementAndGet();
			throw new RuntimeException();
		}));
		var e2 = assertThrows(RuntimeException.class, () -> store.get(KEY));
		assertSame(e1, e2);

		assertDoesNotThrow(localStore::close);
		assertThat(invocations).hasValue(1);
	}

	@Test
	void computeIfAbsentWithFailingCreator() {
		assertThrows(RuntimeException.class, () -> store.computeIfAbsent(KEY, __ -> {
			throw new RuntimeException();
		}));
		assertNull(store.get(KEY));
		assertDoesNotThrow(localStore::close);
	}

}
