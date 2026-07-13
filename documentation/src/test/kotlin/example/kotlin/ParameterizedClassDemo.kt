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

import example.util.StringUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class ParameterizedClassDemo {
    @Nested
    // tag::first_example[]
    @ParameterizedClass
    @ValueSource(strings = ["racecar", "radar", "able was I ere I saw elba"])
    inner class PalindromeTests {
        @Parameter
        lateinit var candidate: String

        @Test
        fun palindrome() {
            assertTrue(StringUtils.isPalindrome(candidate))
        }

        @Test
        fun reversePalindrome() {
            val reverseCandidate = candidate.reversed()
            assertTrue(StringUtils.isPalindrome(reverseCandidate))
        }
    }
    // end::first_example[]

    @Nested
    inner class ConstructorInjection {
        @Nested
        // tag::constructor_injection[]
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
        // end::constructor_injection[]
    }

    @Nested
    inner class FieldInjection {
        @Nested
        // tag::field_injection[]
        @ParameterizedClass
        @CsvSource("apple, 23", "banana, 42")
        inner class FruitTests {
            @Parameter(0)
            lateinit var fruit: String

            @Parameter(1)
            var quantity: Int = 0

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
        // end::field_injection[]
    }

    @Nested
    // tag::nested[]
    @Execution(SAME_THREAD)
    @ParameterizedClass
    @ValueSource(strings = ["apple", "banana"])
    inner class FruitTests {
        @Parameter
        lateinit var fruit: String

        @Nested
        @ParameterizedClass
        @ValueSource(ints = [23, 42])
        inner class QuantityTests {
            @Parameter
            var quantity: Int = 0

            @ParameterizedTest
            @ValueSource(strings = ["PT1H", "PT2H"])
            fun test(duration: Duration) {
                assertFruit(fruit)
                assertQuantity(quantity)
                assertFalse(duration.isNegative)
            }
        }
    }
    // end::nested[]

    private fun assertFruit(fruit: String) {
        assertTrue(
            listOf("apple", "banana", "cherry", "dewberry").contains(fruit)
        ) { "not a fruit: $fruit" }
    }

    private fun assertQuantity(quantity: Int) {
        assertTrue(quantity > 0)
    }
}
