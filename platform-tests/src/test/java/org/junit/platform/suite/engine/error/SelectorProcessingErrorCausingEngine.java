/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.error;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.fakes.TestEngineStub;

public class SelectorProcessingErrorCausingEngine extends TestEngineStub {

	@Override
	public String getId() {
		return "selector-error-engine";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		var engineDescriptor = super.discover(discoveryRequest, uniqueId);
		var errorSelectors = discoveryRequest.getSelectorsByType(ErrorSelector.class);
		if (!errorSelectors.isEmpty()) {
			var selector = errorSelectors.getFirst();
			var failure = SelectorResolutionResult.failed(new RuntimeException(selector.message()));
			discoveryRequest.getDiscoveryListener().selectorProcessed(engineDescriptor.getUniqueId(), selector,
				failure);
		}
		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		throw new RuntimeException("should not be called");
	}
}
