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
import example.util.StringUtils.isPalindrome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import java.util.Random
import java.util.stream.IntStream
import java.util.stream.Stream

// end::user_guide[]
// tag::user_guide[]
class DynamicTestsDemo {
    private val calculator = Calculator()

    // This method will not be executed but produce a warning
    @TestFactory
    // end::user_guide[]
    @Tag("exclude")
    fun dummy(): DynamicTest = dynamicTest("dummy") {}

    // tag::user_guide[]
    fun dynamicTestsWithInvalidReturnType(): List<String> = listOf("Hello")

    @TestFactory
    fun dynamicTestsFromCollection(): Collection<DynamicTest> =
        listOf(
            dynamicTest("1st dynamic test") { assertTrue(isPalindrome("madam")) },
            dynamicTest("2nd dynamic test") { assertEquals(4, calculator.multiply(2, 2)) }
        )

    @TestFactory
    fun dynamicTestsFromIterable(): Iterable<DynamicTest> =
        listOf(
            dynamicTest("3rd dynamic test") { assertTrue(isPalindrome("madam")) },
            dynamicTest("4th dynamic test") { assertEquals(4, calculator.multiply(2, 2)) }
        )

    @TestFactory
    fun dynamicTestsFromIterator(): Iterator<DynamicTest> =
        listOf(
            dynamicTest("5th dynamic test") { assertTrue(isPalindrome("madam")) },
            dynamicTest("6th dynamic test") { assertEquals(4, calculator.multiply(2, 2)) }
        ).iterator()

    @TestFactory
    fun dynamicTestsFromArray(): Array<DynamicTest> =
        arrayOf(
            dynamicTest("7th dynamic test") { assertTrue(isPalindrome("madam")) },
            dynamicTest("8th dynamic test") { assertEquals(4, calculator.multiply(2, 2)) }
        )

    @TestFactory
    fun dynamicTestsFromStream(): Stream<DynamicTest> =
        Stream
            .of("racecar", "radar", "mom", "dad")
            .map { text -> dynamicTest(text) { assertTrue(isPalindrome(text)) } }

    @TestFactory
    fun dynamicTestsFromSequence(): Sequence<DynamicTest> =
        sequenceOf("racecar", "radar", "mom", "dad")
            .map { text -> dynamicTest(text) { assertTrue(isPalindrome(text)) } }

    @TestFactory
    fun dynamicTestsFromIntStream(): Stream<DynamicTest> {
        // Generates tests for the first 10 even integers.
        return IntStream
            .iterate(0) { n -> n + 2 }
            .limit(10)
            .mapToObj { n -> dynamicTest("test$n") { assertEquals(0, n % 2) } }
    }

    @TestFactory
    fun generateRandomNumberOfTests(): Stream<DynamicTest> {
        // Generates random positive integers between 0 and 100 until
        // a number evenly divisible by 7 is encountered.
        val inputGenerator =
            object : Iterator<Int> {
                var random = Random()

                // end::user_guide[]
                init {
                    // Use fixed seed to always produce the same number of tests for execution on the CI server
                    random = Random(23)
                }

                // tag::user_guide[]
                var current = 0

                override fun hasNext(): Boolean {
                    current = random.nextInt(100)
                    return current % 7 != 0
                }

                override fun next(): Int = current
            }

        // Generates display names like: input:5, input:37, input:85, etc.
        val displayNameGenerator = { input: Int -> "input:$input" }

        // Executes tests based on the current input value.
        val testExecutor = { input: Int -> assertTrue(input % 7 != 0) }

        // Returns a stream of dynamic tests.
        return DynamicTest.stream(inputGenerator, displayNameGenerator, testExecutor)
    }

    @TestFactory
    fun dynamicTestsFromStreamFactoryMethod(): Stream<DynamicTest> {
        // Stream of palindromes to check
        val inputStream = Stream.of("racecar", "radar", "mom", "dad")

        // Generates display names like: racecar is a palindrome
        val displayNameGenerator = { text: String -> "$text is a palindrome" }

        // Executes tests based on the current input value.
        val testExecutor = { text: String -> assertTrue(isPalindrome(text)) }

        // Returns a stream of dynamic tests.
        return DynamicTest.stream(inputStream, displayNameGenerator, testExecutor)
    }

    @TestFactory
    fun dynamicTestsWithContainers(): Stream<DynamicNode> =
        Stream
            .of("A", "B", "C")
            .map { input ->
                dynamicContainer(
                    "Container $input",
                    Stream.of(
                        dynamicTest("not null") { assertNotNull(input) },
                        dynamicContainer(
                            "properties",
                            Stream.of(
                                dynamicTest("length > 0") { assertTrue(input.length > 0) },
                                dynamicTest("not empty") { assertFalse(input.isEmpty()) }
                            )
                        )
                    )
                )
            }

    // end::user_guide[]
    // tag::execution_mode[]
    @TestFactory
    @Execution(CONCURRENT) // <1>
    fun dynamicTestsWithConfiguredExecutionMode(): Stream<DynamicNode> =
        Stream
            .of("A", "B", "C")
            .map { input ->
                dynamicContainer { outer ->
                    outer
                        .displayName("Container $input")
                        .children(
                            dynamicTest { config ->
                                config
                                    .displayName("not null")
                                    .executionMode(SAME_THREAD) // <2>
                                    .executable { assertNotNull(input) }
                            },
                            dynamicContainer { inner ->
                                inner
                                    .displayName("properties")
                                    .executionMode(CONCURRENT) // <3>
                                    .childExecutionMode(SAME_THREAD) // <4>
                                    .children(
                                        dynamicTest { config ->
                                            config
                                                .displayName("length > 0")
                                                .executionMode(CONCURRENT) // <5>
                                                .executable { assertTrue(input.length > 0) }
                                        },
                                        dynamicTest { config ->
                                            config
                                                .displayName("not empty")
                                                .executable { assertFalse(input.isEmpty()) }
                                        }
                                    )
                            }
                        )
                }
            }
    // end::execution_mode[]

    // tag::user_guide[]
    @TestFactory
    fun dynamicNodeSingleTest(): DynamicNode = dynamicTest("'pop' is a palindrome") { assertTrue(isPalindrome("pop")) }

    @TestFactory
    fun dynamicNodeSingleContainer(): DynamicNode =
        dynamicContainer(
            "palindromes",
            Stream
                .of("racecar", "radar", "mom", "dad")
                .map { text -> dynamicTest(text) { assertTrue(isPalindrome(text)) } }
        )
}
// end::user_guide[]
