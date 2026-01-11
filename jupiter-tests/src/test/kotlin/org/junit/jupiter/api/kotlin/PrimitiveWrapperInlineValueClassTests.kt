/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests for primitive-wrapper inline value classes.
 *
 * Currently disabled: These fail because Kotlin compiles them to primitives
 * (UInt→int, UserId→long), causing JUnit's type validation to fail before
 * reaching the invocation logic.
 *
 * Supporting these would require modifications to JUnit's core type validation system.
 *
 * @see <a href="https://github.com/junit-team/junit-framework/issues/5081">Issue #5081</a>
 */
@Disabled("Primitive-wrapper inline value classes are not yet supported")
class PrimitiveWrapperInlineValueClassTests {
    @MethodSource("uintProvider")
    @ParameterizedTest
    fun testUInt(value: UInt) {
        assertEquals(42u, value)
    }

    @MethodSource("userIdProvider")
    @ParameterizedTest
    fun testUserId(userId: UserId) {
        assertEquals(123L, userId.value)
    }

    @MethodSource("emailProvider")
    @ParameterizedTest
    fun testEmail(email: Email) {
        assertEquals("test@example.com", email.value)
    }

    companion object {
        @JvmStatic
        fun uintProvider() = listOf(Arguments.of(42u))

        @JvmStatic
        fun userIdProvider() = listOf(Arguments.of(UserId(123L)))

        @JvmStatic
        fun emailProvider() = listOf(Arguments.of(Email("test@example.com")))
    }
}

@JvmInline
value class UserId(
    val value: Long
)

@JvmInline
value class Email(
    val value: String
)
