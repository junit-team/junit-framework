/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.jupiter.api.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests for generic inline value classes.
 * These work because they compile to Object in JVM, bypassing strict type validation.
 */
class GenericInlineValueClassTests {
    @MethodSource("resultProvider")
    @ParameterizedTest
    fun testResult(result: Result<String>) {
        assertEquals("success", result.getOrThrow())
    }

    @MethodSource("multipleResultsProvider")
    @ParameterizedTest
    fun testMultipleResults(
        result1: Result<String>,
        result2: Result<Int>
    ) {
        assertEquals("data", result1.getOrThrow())
        assertEquals(42, result2.getOrThrow())
    }

    @MethodSource("nullableResultProvider")
    @ParameterizedTest
    fun testNullableResult(result: Result<String>?) {
        assertEquals("test", result?.getOrNull())
    }

    @MethodSource("customGenericProvider")
    @ParameterizedTest
    fun testCustomGenericContainer(container: Container<String>) {
        assertEquals("content", container.value)
    }

    companion object {
        @JvmStatic
        fun resultProvider() =
            listOf(
                Arguments.of(Result.success("success"))
            )

        @JvmStatic
        fun multipleResultsProvider() =
            listOf(
                Arguments.of(
                    Result.success("data"),
                    Result.success(42)
                )
            )

        @JvmStatic
        fun nullableResultProvider() =
            listOf(
                Arguments.of(Result.success("test"))
            )

        @JvmStatic
        fun customGenericProvider() =
            listOf(
                Arguments.of(Container("content"))
            )
    }
}

@JvmInline
value class Container<T>(
    val value: T
)
