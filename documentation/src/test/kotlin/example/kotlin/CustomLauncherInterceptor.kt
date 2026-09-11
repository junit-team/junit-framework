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

// tag::user_guide[]
import org.junit.platform.launcher.LauncherInterceptor
import java.io.IOException
import java.io.UncheckedIOException
import java.net.URI
import java.net.URLClassLoader

class CustomLauncherInterceptor : LauncherInterceptor {
    private val customClassLoader: URLClassLoader =
        URLClassLoader(
            arrayOf(URI.create("some.jar").toURL()),
            Thread.currentThread().contextClassLoader
        )

    override fun <T> intercept(invocation: LauncherInterceptor.Invocation<T>): T {
        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.contextClassLoader
        currentThread.contextClassLoader = customClassLoader
        try {
            return invocation.proceed()
        } finally {
            currentThread.contextClassLoader = originalClassLoader
        }
    }

    override fun close() {
        try {
            customClassLoader.close()
        } catch (e: IOException) {
            throw UncheckedIOException("Failed to close custom class loader", e)
        }
    }
}
// end::user_guide[]
