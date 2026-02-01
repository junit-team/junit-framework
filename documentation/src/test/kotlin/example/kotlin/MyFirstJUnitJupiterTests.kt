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
import org.junit.jupiter.api.Test

class MyFirstJUnitJupiterTests {
    private val calculator = Calculator()

    @Test
    fun addition() {
        assertEquals(2, calculator.add(1, 1))
    }
}
// end::user_guide[]
