/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.sharedresources

import example.kotlin.FirstCustomEngine
import example.kotlin.SecondCustomEngine
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.discoveryRequest
import org.junit.platform.launcher.core.LauncherFactory

class SharedResourceDemo {
    // tag::user_guide[]
    @Test
    fun runBothCustomEnginesTest() {
        val firstCustomEngine = FirstCustomEngine()
        val secondCustomEngine = SecondCustomEngine()

        val launcherConfig =
            LauncherConfig
                .builder()
                .addTestEngines(firstCustomEngine, secondCustomEngine)
                .enableTestEngineAutoRegistration(false)
                .build()

        val discoveryRequest =
            discoveryRequest()
                .selectors(selectPackage("com.example.mytests"))
                .build()

        val launcher = LauncherFactory.create(launcherConfig)
        launcher.execute(discoveryRequest)

        assertSame(firstCustomEngine.socket, secondCustomEngine.socket)
        assertTrue(firstCustomEngine.socket!!.isClosed, "socket should be closed")
    }
    // end::user_guide[]
}
