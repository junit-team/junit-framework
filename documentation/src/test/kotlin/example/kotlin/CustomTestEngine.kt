/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

/**
 * This is a no-op [TestEngine] that is only used to make examples compile.
 */
class CustomTestEngine : TestEngine {
    override fun getId(): String = "custom-test-engine"

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor = EngineDescriptor(UniqueId.forEngine(id), "Custom Test Engine")

    override fun execute(request: ExecutionRequest) {
    }
}
