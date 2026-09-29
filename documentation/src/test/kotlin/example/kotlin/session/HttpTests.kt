/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.session

// tag::user_guide[]
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.net.HttpURLConnection
import java.net.URI

@ExtendWith(HttpServerParameterResolver::class)
class HttpTests {
    // end::user_guide[]
    @org.junit.jupiter.api.Tag("exclude")
    // tag::user_guide[]
    @Test
    fun respondsWith204(server: HttpServer) {
        val host = server.address.hostString // <2>
        val port = server.address.port // <3>
        val url = URI.create("http://$host:$port/test").toURL()

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        val responseCode = connection.responseCode // <4>

        assertEquals(204, responseCode) // <5>
    }
}

class HttpServerParameterResolver : ParameterResolver {
    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean = HttpServer::class.java == parameterContext.parameter.type

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any =
        extensionContext
            .getStore(ExtensionContext.Namespace.GLOBAL)
            .get("httpServer", CloseableHttpServer::class.java)!! // <1>
            .server
}
// end::user_guide[]
