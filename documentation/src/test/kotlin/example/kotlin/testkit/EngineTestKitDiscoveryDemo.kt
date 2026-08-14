/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.testkit

// tag::user_guide[]
import example.kotlin.ExampleTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.platform.engine.DiscoveryIssue
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit

class EngineTestKitDiscoveryDemo {
    @Test
    fun verifyJupiterDiscovery() {
        val results =
            EngineTestKit
                .engine("junit-jupiter") // <1>
                .selectors(selectClass(ExampleTestCase::class.java)) // <2>
                .discover() // <3>

        assertEquals("JUnit Jupiter", results.engineDescriptor.displayName) // <4>
        assertEquals(emptyList<DiscoveryIssue>(), results.discoveryIssues) // <5>
    }
}
// end::user_guide[]
