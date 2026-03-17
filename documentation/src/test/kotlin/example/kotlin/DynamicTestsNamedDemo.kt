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

import example.util.StringUtils.isPalindrome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.NamedExecutable
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

class DynamicTestsNamedDemo {
    @TestFactory
    fun dynamicTestsFromStreamFactoryMethodWithNames(): Stream<DynamicTest> {
        // Stream of palindromes to check
        // end::user_guide[]
        // tag::user_guide[]
        val inputStream =
            Stream.of(
                named("racecar is a palindrome", "racecar"),
                named("radar is also a palindrome", "radar"),
                named("mom also seems to be a palindrome", "mom"),
                named("dad is yet another palindrome", "dad")
            )
        // end::user_guide[]
        // tag::user_guide[]

        // Returns a stream of dynamic tests.
        return DynamicTest.stream(inputStream) { text -> assertTrue(isPalindrome(text)) }
    }

    @TestFactory
    fun dynamicTestsFromStreamFactoryMethodWithNamedExecutables(): Stream<DynamicTest> {
        // Stream of palindromes to check
        // end::user_guide[]
        // tag::user_guide[]
        val inputStream =
            Stream
                .of("racecar", "radar", "mom", "dad")
                .map { PalindromeNamedExecutable(it) }
        // end::user_guide[]
        // tag::user_guide[]

        // Returns a stream of dynamic tests based on NamedExecutables.
        return DynamicTest.stream(inputStream)
    }

    class PalindromeNamedExecutable(
        private val text: String
    ) : NamedExecutable {
        override fun getName(): String = "'$text' is a palindrome"

        override fun execute() {
            assertTrue(isPalindrome(text))
        }
    }
}
// end::user_guide[]
