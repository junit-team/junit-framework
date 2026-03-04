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

import example.util.Calculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Assumptions.assumingThat
import org.junit.jupiter.api.Test

class AssumptionsDemo {
    private val calculator = Calculator()

    @Test
    fun testOnlyOnCiServer() {
        assumeTrue(System.getenv("ENV") == "CI")
        // remainder of test
    }

    @Test
    fun testOnlyOnDeveloperWorkstation() {
        assumeTrue(System.getenv("ENV") == "DEV") {
            "Aborting test: not on developer workstation"
        }
        // remainder of test
    }

    @Test
    fun testInAllEnvironments() {
        assumingThat(System.getenv("ENV") == "CI") {
            // perform these assertions only on the CI server
            assertEquals(2, calculator.divide(4, 2))
        }

        // perform these assertions in all environments
        assertEquals(42, calculator.multiply(6, 7))
    }
}
// end::user_guide[]
