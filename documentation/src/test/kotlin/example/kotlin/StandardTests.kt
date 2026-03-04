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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class StandardTests {
    @BeforeEach
    fun init() {
    }

    @Test
    fun succeedingTest() {
    }

    // end::user_guide[]
    @extensions.ExpectToFail
    // tag::user_guide[]
    @Test
    fun failingTest() {
        fail("a failing test")
    }

    @Test
    @Disabled("for demonstration purposes")
    fun skippedTest() {
        // not executed
    }

    @Test
    fun abortedTest() {
        assumeTrue("abc".contains("Z"))
        fail("test should have been aborted")
    }

    @AfterEach
    fun tearDown() {
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun initAll() {
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
        }
    }
}
// end::user_guide[]
