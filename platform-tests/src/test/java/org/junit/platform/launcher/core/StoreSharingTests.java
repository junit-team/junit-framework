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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.fakes.TestEngineSpy;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherExecutionRequest;

/**
 * @since 5.13
 */
class StoreSharingTests {

	@Test
	void twoDummyEnginesUseRequestLevelStore() {
		TestEngineSpy engineWriter = new TestEngineSpy("Writer") {
			@Override
			public void execute(ExecutionRequest request) {
				request.getStore().put(Namespace.GLOBAL, "sharedKey", "Hello from Writer");
				super.execute(request);
			}
		};

		TestEngineStub engineReader = new TestEngineStub("Reader") {
			@Override
			public void execute(ExecutionRequest request) {
				Object value = request.getStore().get(Namespace.GLOBAL, "sharedKey");
				assertEquals("Hello from Writer", value);
				super.execute(request);
			}
		};

		ExecutionRequest request = mock(ExecutionRequest.class);
		when(request.getStore()).thenReturn(NamespacedHierarchicalStoreProviders.dummyNamespacedHierarchicalStore());

		Launcher launcher = LauncherFactory.create( //
			LauncherConfig.builder() //
					.addTestEngines(engineWriter, engineReader) //
					.build());

		LauncherExecutionRequest discoveryRequest = LauncherDiscoveryRequestBuilder //
				.request() //
				.forExecution() //
				.build();

		launcher.execute(discoveryRequest);
	}
}
