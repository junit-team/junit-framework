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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// tag::user_guide[]
class RandomNumberDemo(
    @Random randomNumber2: Int
) {
    // Use randomNumber1 field in test methods and @BeforeEach
    // or @AfterEach lifecycle methods.
    @Random
    var randomNumber1: Int = 0

    // randomNumber2 is available in the constructor via primary constructor above.

    @BeforeEach
    fun beforeEach(
        @Random randomNumber3: Int
    ) {
        // Use randomNumber3 in @BeforeEach method.
    }

    @Test
    fun test(
        @Random randomNumber4: Int
    ) {
        // Use randomNumber4 in test method.
    }

    companion object {
        // Use static randomNumber0 field anywhere in the test class,
        // including @BeforeAll or @AfterAll lifecycle methods.
        @Random
        @JvmStatic
        var randomNumber0: Int = 0
    }
}
// end::user_guide[]
