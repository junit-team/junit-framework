/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.junit.platform.engine.support.store.NamespacedHierarchicalStore.CloseAction.closeAutoCloseables;

import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

public class NamespacedHierarchicalStoreProviders {

	public static NamespacedHierarchicalStore<Namespace> dummyNamespacedHierarchicalStore() {
		return new NamespacedHierarchicalStore<>(dummyNamespacedHierarchicalStoreWithNoParent(), closeAutoCloseables());
	}

	public static NamespacedHierarchicalStore<Namespace> dummyNamespacedHierarchicalStoreWithNoParent() {
		return new NamespacedHierarchicalStore<>(null, closeAutoCloseables());
	}

	private NamespacedHierarchicalStoreProviders() {
	}
}
