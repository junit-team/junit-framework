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

import example.extensions.Random
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

// tag::user_guide[]
class MyRandomParametersTest(
    @Random randomNumber: Int
) {
    @Test
    fun injectsInteger(
        @Random i: Int,
        @Random j: Int
    ) {
        assertNotEquals(i, j)
    }
}
// end::user_guide[]
