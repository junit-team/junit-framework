/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.LauncherStoreFacade;
import org.junit.jupiter.engine.execution.NamespaceAwareStore;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

/**
 * Tests for {@link LauncherStoreFacade}.
 *
 * @since 5.13
 */
class LauncherStoreFacadeTest {

	private NamespacedHierarchicalStore<Namespace> sessionScopedStore = new NamespacedHierarchicalStore<>(null);
	private NamespacedHierarchicalStore<Namespace> requestScopedStore = new NamespacedHierarchicalStore<>(
		sessionScopedStore);
	private ExtensionContext.Namespace extensionNamespace = ExtensionContext.Namespace.create("foo", "bar");

	@Test
	void createsInstanceSuccessfullyWithValidStore() {
		assertDoesNotThrow(() -> new LauncherStoreFacade(requestScopedStore));
	}

	@Test
	void throwsExceptionWhenRequestLevelStoreHasNoParent() {
		assertThrowsExactly(JUnitException.class, () -> new LauncherStoreFacade(sessionScopedStore), () -> {
			throw new JUnitException("Request-level store must have a parent");
		});
	}

	@Test
	void returnsRequestLevelStore() {
		LauncherStoreFacade facade = new LauncherStoreFacade(requestScopedStore);
		assertEquals(requestScopedStore, facade.getRequestScopedStore());
	}

	@Test
	void returnsNamespaceAwareStoreWithRequestLevelStore() {
		LauncherStoreFacade facade = new LauncherStoreFacade(requestScopedStore);
		ExtensionContext.Store store = facade.getRequestScopedStore(extensionNamespace);

		assertNotNull(store);
		assertInstanceOf(NamespaceAwareStore.class, store);
	}

	@Test
	void returnsNamespaceAwareStore() {
		LauncherStoreFacade facade = new LauncherStoreFacade(requestScopedStore);
		NamespaceAwareStore adapter = facade.getStoreAdapter(requestScopedStore, extensionNamespace);

		assertNotNull(adapter);
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void throwsExceptionWhenNamespaceIsNull() {
		LauncherStoreFacade facade = new LauncherStoreFacade(requestScopedStore);
		assertPreconditionViolationFor(() -> facade.getStoreAdapter(requestScopedStore, null));
	}

	@Test
	void returnsNamespaceAwareStoreWithGlobalNamespace() {
		requestScopedStore.put(Namespace.GLOBAL, "foo", "bar");

		LauncherStoreFacade facade = new LauncherStoreFacade(requestScopedStore);
		ExtensionContext.Store store = facade.getRequestScopedStore(ExtensionContext.Namespace.GLOBAL);

		assertEquals("bar", store.get("foo"));
	}
}
