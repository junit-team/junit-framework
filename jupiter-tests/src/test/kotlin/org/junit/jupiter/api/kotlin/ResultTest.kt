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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ResultTest {
    /**
     * This test passes.
     * Good pass: .getOrThrow() returns the expected type and value.
     */
    @Test
    fun normal() {
        val result: Result<String> = Result.success("something")
        val actual = result.getOrThrow()
        assertEquals("something", actual)
    }

    /**
     * This test passes.
     * Good pass: the cast is invalid and therefore .getOrThrow() should throw as it does.
     */
    @Test
    fun cast() {
        val result: Result<String> = Result.success("something")

        @Suppress("UNCHECKED_CAST")
        val castResult = result as Result<Result<String>>
        assertThrows<ClassCastException> {
            val actual = castResult.getOrThrow()
        }
    }

    /**
     * This test passes.
     * Good pass: direct calling the method returns the right type.
     * This to me proves that the issue is somewhere inside @ParameterizedTest handling.
     */
    @Test
    fun direct() {
        val args = valueProviderFull()

        @Suppress("UNCHECKED_CAST")
        val result: Result<String> = args.single().get().single() as Result<String>
        val actual = result.getOrThrow()
        assertEquals("something", actual)
    }

    /**
     * This test passes.
     * Good pass: the type of the parameter matches the type of the value provided as the argument from method source.
     */
    @MethodSource("valueProviderRaw")
    @ParameterizedTest
    fun parameterizedRaw(value: String) {
        val result: Result<String> = Result.success(value)
        val actual = result.getOrThrow()
        assertEquals("something", actual)
    }

    /**
     * This test errors with:
     * > java.lang.ClassCastException: class kotlin.Result cannot be cast to class java.lang.String.
     * This test should pass, because the argument from the method source is a Result<String>.
     */
    @MethodSource("valueProviderFull")
    @ParameterizedTest
    fun parameterizedFull(result: Result<String>) {
        val actual = result.getOrThrow()
        assertEquals("something", actual)
    }

    /**
     * This test passes.
     * This test should fail when trying to call `castResult.getOrThrow()`.
     */
    @MethodSource("valueProviderFull")
    @ParameterizedTest
    fun parameterizedFullCast(result: Result<String>) {
        @Suppress("UNCHECKED_CAST")
        val castResult = result as Result<Result<String>>
        val actual = castResult.getOrThrow()
        assertEquals(Result.success("something"), actual)
        assertEquals("something", actual.getOrThrow())
    }

    /**
     * This test passes.
     * This test should fail when trying to call `result.getOrThrow()`,
     * because the provided argument is a Result<String>.
     */
    @MethodSource("valueProviderFull")
    @ParameterizedTest
    fun parameterizedFullMistyped(result: Result<Result<String>>) {
        val actual = result.getOrThrow()
        assertEquals(Result.success("something"), actual)
        assertEquals("something", actual.getOrThrow())
    }

    companion object {
        @JvmStatic
        fun valueProviderRaw() =
            listOf(
                Arguments.of("something")
            )

        @JvmStatic
        fun valueProviderFull() =
            listOf(
                Arguments.of(Result.success("something"))
            )
    }
}
