/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DiscoveryContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

/**
 * @since 6.2
 */
class DefaultDiscoveryContext implements DiscoveryContext {

	private final JupiterConfiguration configuration;

	DefaultDiscoveryContext(JupiterConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Optional<String> getConfigurationParameter(String key) {
		return this.configuration.getRawConfigurationParameter(key);
	}

	@Override
	public StoreAccessor getStoreAccessor(Object... namespaceParts) {
		return new DefaultStoreAccessor(this.configuration.getSessionScopedStore(), Namespace.create(namespaceParts));
	}

	private record DefaultStoreAccessor(NamespacedHierarchicalStore<Namespace> store, Namespace namespace)
			implements StoreAccessor {

		@Override
		public @Nullable Object get(Object key) {
			return store.get(namespace, key);
		}

		@Override
		public void put(Object key, @Nullable Object value) {
			store.put(namespace, key, value);
		}
	}
}
