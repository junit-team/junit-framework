/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.fakes;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

public class FaultyTestEngines {

	public static TestEngineStub createEngineThatCannotResolveAnything(String engineId) {
		return new TestEngineStub(engineId) {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				discoveryRequest.getSelectorsByType(DiscoverySelector.class) //
						.forEach(selector -> discoveryRequest.getDiscoveryListener().selectorProcessed(uniqueId,
							selector, SelectorResolutionResult.unresolved()));
				return new EngineDescriptor(uniqueId, "Some Engine");
			}

			@Override
			public void execute(ExecutionRequest request) {
				var listener = request.getEngineExecutionListener();
				var rootTestDescriptor = request.getRootTestDescriptor();
				listener.executionStarted(rootTestDescriptor);
				listener.executionFinished(rootTestDescriptor, TestExecutionResult.successful());
			}
		};
	}

	public static TestEngineStub createEngineThatFailsToResolveAnything(String engineId, Throwable rootCause) {
		return new TestEngineStub(engineId) {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				discoveryRequest.getSelectorsByType(DiscoverySelector.class) //
						.forEach(selector -> discoveryRequest.getDiscoveryListener().selectorProcessed(uniqueId,
							selector, SelectorResolutionResult.failed(rootCause)));
				return new EngineDescriptor(uniqueId, "Some Engine");
			}

			@Override
			public void execute(ExecutionRequest request) {
				var listener = request.getEngineExecutionListener();
				var rootTestDescriptor = request.getRootTestDescriptor();
				listener.executionStarted(rootTestDescriptor);
				listener.executionFinished(rootTestDescriptor, TestExecutionResult.successful());
			}
		};
	}

	private FaultyTestEngines() {
	}
}
