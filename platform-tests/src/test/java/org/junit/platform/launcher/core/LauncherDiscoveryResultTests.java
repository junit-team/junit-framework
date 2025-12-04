/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.core.LauncherDiscoveryResult.EngineResultInfo;

/**
 * Unit tests for {@link LauncherDiscoveryResult}.
 */
class LauncherDiscoveryResultTests {

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/4862">GitHub issue #4862</>
	 */
	@Test
	void withRetainedEnginesMaintainsOriginalTestEngineRegistrationOrder() {
		var engine1 = new DemoEngine("Engine 1");
		var engine2 = new DemoEngine("Engine 2");
		var engine3 = new DemoEngine("Engine 3");
		var engine4 = new DemoEngine("Engine 4");

		@SuppressWarnings("serial")
		Map<TestEngine, EngineResultInfo> engineResults = new LinkedHashMap<>() {
			{
				put(engine1, new DemoEngineResultInfo(true));
				put(engine2, new DemoEngineResultInfo(false));
				put(engine3, new DemoEngineResultInfo(false));
				put(engine4, new DemoEngineResultInfo(true));
			}
		};
		assertThat(engineResults.keySet()).containsExactly(engine1, engine2, engine3, engine4);

		LauncherDiscoveryResult discoveryResult = new LauncherDiscoveryResult(engineResults, mock(), mock());
		assertThat(discoveryResult.getTestEngines()).containsExactly(engine1, engine2, engine3, engine4);

		LauncherDiscoveryResult prunedDiscoveryResult = discoveryResult.withRetainedEngines(TestDescriptor::isTest);
		assertThat(prunedDiscoveryResult.getTestEngines()).containsExactly(engine1, engine4);
	}

	private record DemoEngine(String id) implements TestEngine {

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
			throw new UnsupportedOperationException("discover");
		}

		@Override
		public void execute(ExecutionRequest request) {
			throw new UnsupportedOperationException("execute");
		}

		@Override
		public String toString() {
			return getId();
		}
	}

	private static class DemoEngineResultInfo extends EngineResultInfo {

		DemoEngineResultInfo(boolean isTest) {
			super(createRootDescriptor(isTest), mock(), null);
		}

		private static TestDescriptor createRootDescriptor(boolean isTest) {
			TestDescriptor rootDescriptor = mock();
			when(rootDescriptor.isTest()).thenReturn(isTest);
			return rootDescriptor;
		}
	}

}
