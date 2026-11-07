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

import org.junit.jupiter.api.DiscoveryContext;
import org.junit.jupiter.api.store.Namespace;
import org.junit.jupiter.api.store.Store;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.NamespaceAwareStore;

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
	public Store getStore(Namespace namespace) {
		return NamespaceAwareStore.createAdapter(this.configuration.getSessionScopedStore(), namespace);
	}

}
