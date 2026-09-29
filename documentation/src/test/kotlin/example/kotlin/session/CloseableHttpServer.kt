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
import java.util.concurrent.ExecutorService

class CloseableHttpServer(
    val server: HttpServer,
    private val executorService: ExecutorService
) : AutoCloseable {
    override fun close() { // <1>
        server.stop(0) // <2>
        executorService.shutdownNow()
    }
}
// end::user_guide[]
