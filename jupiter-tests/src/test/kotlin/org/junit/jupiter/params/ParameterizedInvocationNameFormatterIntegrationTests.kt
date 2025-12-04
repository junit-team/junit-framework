/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.jupiter.params

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.provider.ValueSource

class ParameterizedInvocationNameFormatterIntegrationTests {
    @ValueSource(strings = ["foo", "bar"])
    @ParameterizedTest
    fun defaultDisplayName(
        param: String,
        info: TestInfo
    ) {
        if (param.equals("foo")) {
            assertEquals("[1] param = \"foo\"", info.displayName)
        } else {
            assertEquals("[2] param = \"bar\"", info.displayName)
        }
    }

    @ValueSource(strings = ["foo", "bar"])
    @ParameterizedTest(name = "{0}")
    fun `1st argument`(
        param: String,
        info: TestInfo
    ) {
        if (param.equals("foo")) {
            assertEquals("\"foo\"", info.displayName)
        } else {
            assertEquals("\"bar\"", info.displayName)
        }
    }

    @ValueSource(strings = ["foo", "bar"])
    @ParameterizedTest(name = "{displayName}")
    fun `it's an {enigma} '{0}'`(
        @Suppress("UNUSED_PARAMETER") param: String,
        info: TestInfo
    ) {
        assertEquals("it's an {enigma} '{0}'(String, TestInfo)", info.displayName)
    }

    @ValueSource(strings = ["foo", "bar"])
    @ParameterizedTest(name = "{displayName} - {0}")
    fun `displayName and 1st 'argument'`(
        param: String,
        info: TestInfo
    ) {
        if (param.equals("foo")) {
            assertEquals("displayName and 1st 'argument'(String, TestInfo) - \"foo\"", info.displayName)
        } else {
            assertEquals("displayName and 1st 'argument'(String, TestInfo) - \"bar\"", info.displayName)
        }
    }

    @ValueSource(strings = ["foo", "bar"])
    @ParameterizedTest(name = "{0} - {displayName}", quoteTextArguments = false)
    fun `1st 'argument' and displayName`(
        param: String,
        info: TestInfo
    ) {
        if (param.equals("foo")) {
            assertEquals("foo - 1st 'argument' and displayName(String, TestInfo)", info.displayName)
        } else {
            assertEquals("bar - 1st 'argument' and displayName(String, TestInfo)", info.displayName)
        }
    }
}
