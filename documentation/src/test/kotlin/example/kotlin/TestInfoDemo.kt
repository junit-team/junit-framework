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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@DisplayName("TestInfo Demo")
class TestInfoDemo(
    testInfo: TestInfo
) {
    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll(testInfo: TestInfo) {
            assertEquals("TestInfo Demo", testInfo.displayName)
        }
    }

    init {
        assertTrue(testInfo.displayName in listOf("TEST 1", "test2()"))
    }

    @BeforeEach
    fun init(testInfo: TestInfo) {
        assertTrue(testInfo.displayName in listOf("TEST 1", "test2()"))
    }

    @Test
    @DisplayName("TEST 1")
    @Tag("my-tag")
    fun test1(testInfo: TestInfo) {
        assertEquals("TEST 1", testInfo.displayName)
        assertTrue("my-tag" in testInfo.tags)
    }

    @Test
    fun test2() {
    }
}
// end::user_guide[]
