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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.CsvSource

class ParameterizedRecordDemo {
    @Nested
    // tag::example[]
    @ParameterizedClass
    @CsvSource("apple, 23", "banana, 42")
    inner class FruitTests(
        private val fruit: String,
        private val quantity: Int
    ) {
        @Test
        fun test() {
            assertFruit(fruit)
            assertQuantity(quantity)
        }

        @Test
        fun anotherTest() {
            // ...
        }
    }
    // end::example[]

    private fun assertFruit(fruit: String) {
        assertTrue(listOf("apple", "banana", "cherry", "dewberry").contains(fruit))
    }

    private fun assertQuantity(quantity: Int) {
        assertTrue(quantity >= 0)
    }
}
