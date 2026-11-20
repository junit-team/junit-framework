/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package com.example.project

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ParameterizedMethodTests {
    @MethodSource("uintProvider")
    @ParameterizedTest
    fun testUInt(value: UInt) {
        assertEquals(42u, value)
        fail("expected")
    }

    @MethodSource("resultProvider")
    @ParameterizedTest
    fun testResult(result: Result<String>) {
        assertEquals("success", result.getOrThrow())
        fail("expected")
    }

    companion object {
        @JvmStatic
        fun uintProvider() = listOf(Arguments.of(42u))

        @JvmStatic
        fun resultProvider() = listOf(Arguments.of(Result.success("success")))
    }
}
