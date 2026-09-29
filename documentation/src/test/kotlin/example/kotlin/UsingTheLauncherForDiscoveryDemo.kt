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

// tag::imports[]
import org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.discoveryRequest
import org.junit.platform.launcher.core.LauncherFactory
// end::imports[]

/**
 * @since 6.0
 */
class UsingTheLauncherForDiscoveryDemo {
    @org.junit.jupiter.api.Test
    @Suppress("UNUSED_VARIABLE")
    fun discovery() {
        // tag::discovery[]
        val discoveryRequest =
            discoveryRequest()
                .selectors(
                    selectPackage("com.example.mytests"),
                    selectClass(MyTestClass::class.java)
                ).filters(
                    includeClassNamePatterns(".*Tests")
                ).build()

        LauncherFactory.openSession().use { session ->
            val testPlan = session.launcher.discover(discoveryRequest)

            // ... discover additional test plans or execute tests
        }
        // end::discovery[]
    }

    class MyTestClass
}
