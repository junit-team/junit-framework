/*
 * Copyright 2015-2025 the original author or authors.
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

	private final NamespacedHierarchicalStore<Namespace> requestLevelStore;
	private final NamespacedHierarchicalStore<Namespace> sessionLevelStore;

	public LauncherStoreFacade(NamespacedHierarchicalStore<Namespace> requestLevelStore) {
		this.requestLevelStore = requestLevelStore;
		this.sessionLevelStore = requestLevelStore.getParent().orElseThrow(
			() -> new JUnitException("Request-level store must have a parent"));
	}

	public NamespacedHierarchicalStore<Namespace> getRequestLevelStore() {
		return this.requestLevelStore;
	}

	public ExtensionContext.Store getRequestLevelStore(ExtensionContext.Namespace namespace) {
		return getStoreAdapter(this.requestLevelStore, namespace);
	}

	public ExtensionContext.Store getSessionLevelStore(ExtensionContext.Namespace namespace) {
		return getStoreAdapter(this.sessionLevelStore, namespace);
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
