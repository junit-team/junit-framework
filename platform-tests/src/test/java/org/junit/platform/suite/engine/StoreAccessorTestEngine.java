/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.fakes.TestEngineStub;

public class StoreAccessorTestEngine extends TestEngineStub {

	public static final String KEY = "key";

	public StoreAccessorTestEngine() {
		super("store-accessor");
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
		var store = request.getSessionScopedStore();
		var namespace = Namespace.create("suite-forwarding");
		var oldValue = store.get(namespace, KEY);
		store.put(namespace, KEY, oldValue + " (modified)");
		return super.discover(request, uniqueId);
	}
}
