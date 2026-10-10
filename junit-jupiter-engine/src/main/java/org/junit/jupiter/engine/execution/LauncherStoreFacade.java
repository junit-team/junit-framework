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

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

@API(status = INTERNAL, since = "5.14")
public class LauncherStoreFacade {

	private final NamespacedHierarchicalStore<Namespace> requestScopedStore;
	private final NamespacedHierarchicalStore<Namespace> sessionScopedStore;

	public LauncherStoreFacade(NamespacedHierarchicalStore<Namespace> requestScopedStore) {
		this.requestScopedStore = requestScopedStore;
		this.sessionScopedStore = requestScopedStore.getParent().orElseThrow(
			() -> new JUnitException("Request-level store must have a parent"));
	}

	public NamespacedHierarchicalStore<Namespace> getRequestScopedStore() {
		return this.requestScopedStore;
	}

	public ExtensionContext.Store getRequestScopedStore(ExtensionContext.Namespace namespace) {
		return getStoreAdapter(this.requestScopedStore, namespace);
	}

	public ExtensionContext.Store getSessionScopedStore(ExtensionContext.Namespace namespace) {
		return getStoreAdapter(this.sessionScopedStore, namespace);
	}

	public NamespaceAwareStore getStoreAdapter(NamespacedHierarchicalStore<Namespace> valuesStore,
			ExtensionContext.Namespace namespace) {
		Preconditions.notNull(namespace, "Namespace must not be null");
		return new NamespaceAwareStore(valuesStore, convert(namespace));
	}

	private Namespace convert(ExtensionContext.Namespace namespace) {
		return namespace.equals(ExtensionContext.Namespace.GLOBAL) //
				? Namespace.GLOBAL //
				: Namespace.create(namespace.getParts());
	}
}
