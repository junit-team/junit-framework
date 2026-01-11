/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

/**
 * Test cases for default implementations of {@link org.junit.platform.engine.TestEngine}.
 *
 * <p>Note: the package {@code org.junit.platform} this class resides in is
 * chosen on purpose. If it was in {@code org.junit.platform.engine} the default
 * implementation will pick up values defined by the real Jupiter test engine.
 *
 * @since 1.1
 */
class TestEngineTests {

	@Test
	void defaults() {
		TestEngine engine = new DefaultEngine();
		assertEquals(Optional.empty(), engine.getGroupId());
		assertEquals(Optional.empty(), engine.getArtifactId());
		assertEquals(Optional.of("DEVELOPMENT"), engine.getVersion());
	}

	private static class DefaultEngine implements TestEngine {

		@Override
		public String getId() {
			return getClass().getSimpleName();
		}

		@Override
		public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
			throw new UnsupportedOperationException("discover");
		}

		@Override
		public void execute(ExecutionRequest request) {
			throw new UnsupportedOperationException("execute");
		}
	}
}
