/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.provider.ValueSource
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit

class ParameterizedClassKotlinIntegrationTests {
    @Test
    fun supportsDataClasses() {
        val results =
            EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(TestCase::class.java))
                .execute()

        results.containerEvents().assertStatistics {
            it.started(4).succeeded(4)
        }
        results.testEvents().assertStatistics {
            it.started(4).succeeded(2).failed(2)
        }
    }

    @ParameterizedClass
    @ValueSource(ints = [-1, 1])
    data class TestCase(
        val value: Int,
        val testInfo: TestInfo
    ) {
        @Test
        fun test1() {
            assertEquals("test1()", testInfo.displayName)
            assertTrue(value < 0, "negative")
        }

        @Test
        fun test2() {
            assertEquals("test2()", testInfo.displayName)
            assertTrue(value < 0, "negative")
        }
    }
}
