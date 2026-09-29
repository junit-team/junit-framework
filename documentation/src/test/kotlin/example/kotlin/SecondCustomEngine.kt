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
import org.junit.platform.engine.TestExecutionResult.successful
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.junit.platform.engine.support.store.Namespace
import java.io.IOException
import java.io.UncheckedIOException
import java.net.InetAddress.getLoopbackAddress
import java.net.ServerSocket

// tag::user_guide[]

/**
 * Second custom test engine implementation.
 */
class SecondCustomEngine : TestEngine {
    var socket: ServerSocket? = null

    override fun getId(): String = "second-custom-test-engine"

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor = EngineDescriptor(uniqueId, "Second Custom Test Engine")

    override fun execute(request: ExecutionRequest) {
        request.engineExecutionListener
            .executionStarted(request.rootTestDescriptor)

        val store = request.store
        socket =
            store.computeIfAbsent(Namespace.GLOBAL, "serverSocket", {
                try {
                    ServerSocket(0, 50, getLoopbackAddress())
                } catch (e: IOException) {
                    throw UncheckedIOException("Failed to start ServerSocket", e)
                }
            }, ServerSocket::class.java)

        request.engineExecutionListener
            .executionFinished(request.rootTestDescriptor, successful())
    }
}
// end::user_guide[]
