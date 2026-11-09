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
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests for primitive-wrapper inline value classes.
 */
@ParameterizedClass
@MethodSource("uintProvider")
class PrimitiveWrapperInlineValueClassTests(value: UInt) {

    @MethodSource("uintProvider")
    @ParameterizedTest
    @JvmName("testUInt")
    fun testUInt(value: UInt) {
        assertEquals(42u, value)
    }

    @MethodSource("userIdProvider")
    @ParameterizedTest
    @JvmName("testUserId")
    fun testUserId(userId: UserId) {
        assertEquals(123L, userId.value)
    }

    @MethodSource("emailProvider")
    @ParameterizedTest
    @JvmName("testEmail")
    fun testEmail(email: Email) {
        assertEquals("test@example.com", email.value)
    }

    @MethodSource("subscriptionProvider")
    @ParameterizedTest
    @JvmName("testSubscription")
    fun testSubscription(subscription: Subscription?) {
        assertNull(subscription)
    }

    companion object {
        @JvmStatic
        fun uintProvider() = listOf(Arguments.of(42u))

        @JvmStatic
        fun userIdProvider() = listOf(Arguments.of(UserId(123L)))

        @JvmStatic
        fun emailProvider() = listOf(Arguments.of(Email("test@example.com")))

        @JvmStatic
        fun subscriptionProvider() = listOf(Arguments.of(null))
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

@JvmInline
value class Subscription(
    val value: Boolean
)
