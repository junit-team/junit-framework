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
import org.junit.platform.engine.support.store.Namespace
import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import java.io.IOException
import java.io.UncheckedIOException
import java.net.InetAddress.getLoopbackAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class GlobalSetupTeardownListener : LauncherSessionListener {
    override fun launcherSessionOpened(session: LauncherSession) {
        // Avoid setup for test discovery by delaying it until tests are about to be executed
        session.launcher.registerTestExecutionListeners(
            object : TestExecutionListener {
                override fun testPlanExecutionStarted(testPlan: TestPlan) {
                    // end::user_guide[]
                    if (!testPlan.configurationParameters.getBoolean("enableHttpServer").orElse(true)) {
                        // avoid starting multiple HTTP servers unnecessarily from UsingTheLauncherDemo
                        return
                    }
                    // tag::user_guide[]
                    val store = session.store // <1>
                    // end::user_guide[]
                    // @formatter:off
                    // tag::user_guide[]
                    store.computeIfAbsent(Namespace.GLOBAL, "httpServer") { // <2>
                        // end::user_guide[]
                        // @formatter:on
                        // tag::user_guide[]
                        val address = InetSocketAddress(getLoopbackAddress(), 0)
                        val server: HttpServer =
                            try {
                                HttpServer.create(address, 0)
                            } catch (e: IOException) {
                                throw UncheckedIOException("Failed to start HTTP server", e)
                            }
                        server.createContext("/test") { exchange ->
                            exchange.sendResponseHeaders(204, -1)
                            exchange.close()
                        }
                        val executorService = Executors.newCachedThreadPool()
                        server.executor = executorService
                        server.start() // <3>

                        CloseableHttpServer(server, executorService)
                    }
                }
            }
        )
    }
}
// end::user_guide[]
