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
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.IOException
import java.io.UncheckedIOException

// tag::user_guide[]
class HttpServerExtension : ParameterResolver {
    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean = HttpServer::class.java == parameterContext.parameter.type

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        val rootContext = extensionContext.root
        val store = rootContext.getStore(Namespace.GLOBAL)
        val key = HttpServerResource::class.java
        val resource =
            store.computeIfAbsent(key, {
                try {
                    HttpServerResource(0).apply { start() }
                } catch (e: IOException) {
                    throw UncheckedIOException("Failed to create HttpServerResource", e)
                }
            }, HttpServerResource::class.java)
        return resource.httpServer
    }
}
// end::user_guide[]
