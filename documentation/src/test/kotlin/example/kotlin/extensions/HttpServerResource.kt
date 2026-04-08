/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.extensions

import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress

// tag::user_guide[]
class HttpServerResource(
    port: Int
) : AutoCloseable {
    val httpServer: HttpServer

    init {
        val loopbackAddress = InetAddress.getLoopbackAddress()
        httpServer = HttpServer.create(InetSocketAddress(loopbackAddress, port), 0)
    }

    // end::user_guide[]

    // tag::user_guide[]
    fun start() {
        // Example handler
        httpServer.createContext("/example") { exchange ->
            val body = "This is a test"
            exchange.sendResponseHeaders(200, body.length.toLong())
            exchange.responseBody.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }
        }
        httpServer.executor = null
        httpServer.start()
    }

    override fun close() {
        httpServer.stop(0)
    }
}
// end::user_guide[]
