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

import com.sun.net.httpserver.HttpServer
import example.kotlin.extensions.HttpServerExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

// tag::user_guide[]
@ExtendWith(HttpServerExtension::class)
class HttpServerDemo {
    // end::user_guide[]
    @Suppress("HttpUrlsUsage")
    // tag::user_guide[]
    @Test
    fun httpCall(server: HttpServer) {
        val address = server.address
        val requestUrl = URI("http://${address.hostName}:${address.port}/example").toURL()

        val responseBody = sendRequest(requestUrl)

        assertEquals("This is a test", responseBody)
    }

    private fun sendRequest(url: URL): String {
        val connection = url.openConnection() as HttpURLConnection
        val contentLength = connection.contentLength
        url.openStream().use { response ->
            val content = ByteArray(contentLength)
            assertEquals(contentLength, response.read(content))
            return String(content, Charsets.UTF_8)
        }
    }
}
// end::user_guide[]
