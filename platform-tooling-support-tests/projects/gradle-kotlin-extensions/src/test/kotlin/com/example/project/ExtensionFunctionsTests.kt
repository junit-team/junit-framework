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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.opentest4j.AssertionFailedError

class ExtensionFunctionsTests {
    @Test
    fun `assertDoesNotThrow() and assertAll`() {
        assertDoesNotThrow {
            assertAll(setOf {})
        }
        assertDoesNotThrow("message") {
            assertAll("header", setOf {})
        }
        assertDoesNotThrow({ "message" }) {
            assertAll({})
            assertAll("header", {})
        }
    }

    @Test
    fun `fail() and assertThrows`() {
        assertThrows<AssertionFailedError> {
            fail("message")
        }
        assertThrows<AssertionFailedError>("message") {
            fail { "message" }
        }
        assertThrows<AssertionFailedError>({ "message" }) {
            fail(IllegalArgumentException())
        }
    }

    @ParameterizedTest
    @CsvSource("1")
    fun accessor(accessor: ArgumentsAccessor) {
        val value = accessor.get<Int>(0)
        assertEquals(1, value)
    }
}
