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

import org.junit.jupiter.api.AssertionTestUtils.assertEmptyMessage
import org.junit.jupiter.api.AssertionTestUtils.assertMessageContains
import org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.opentest4j.AssertionFailedError
import java.util.stream.Stream

class KotlinFailAssertionsTests {
    @Test
    fun `fail with string`() {
        val message = "test"
        val ex =
            assertThrows<AssertionFailedError> {
                fail(message)
            }
        assertMessageEquals(ex, message)
    }

    @Test
    fun `fail with message supplier`() {
        val message = "test"
        val ex =
            assertThrows<AssertionFailedError> {
                fail { message }
            }
        assertMessageEquals(ex, message)
    }

    @Test
    fun `fail with null string`() {
        val ex =
            assertThrows<AssertionFailedError> {
                fail(null as String?)
            }
        assertEmptyMessage(ex)
    }

    @Test
    fun `fail with null message supplier`() {
        val ex =
            assertThrows<AssertionFailedError> {
                fail(null as (() -> String)?)
            }
        assertEmptyMessage(ex)
    }

    @Test
    fun `fail with string and throwable`() {
        val message = "message"
        val throwableCause = "cause"
        val ex =
            assertThrows<AssertionFailedError> {
                fail(message, Throwable(throwableCause))
            }
        assertMessageEquals(ex, message)
        val cause = ex.cause
        assertMessageContains(cause!!, throwableCause)
    }

    @Test
    fun `fail with throwable`() {
        val throwableCause = "cause"
        val ex =
            assertThrows<AssertionFailedError> {
                fail(Throwable(throwableCause))
            }
        assertEmptyMessage(ex)
        val cause = ex.cause
        assertMessageContains(cause!!, throwableCause)
    }

    @Test
    fun `fail with string and null throwable`() {
        val message = "message"
        val ex =
            assertThrows<AssertionFailedError> {
                fail(message, null)
            }
        assertMessageEquals(ex, message)
        if (ex.cause != null) {
            throw AssertionError("Cause should have been null")
        }
    }

    @Test
    fun `fail with null string and throwable`() {
        val throwableCause = "cause"
        val ex =
            assertThrows<AssertionFailedError> {
                fail(null, Throwable(throwableCause))
            }
        assertEmptyMessage(ex)
        val cause = ex.cause
        assertMessageContains(cause!!, throwableCause)
    }

    @Test
    fun `fail usable as a stream expression`() {
        val count =
            Stream
                .empty<Any>()
                .peek { _ -> fail("peek should never be called") }
                .filter { _ -> fail("filter should never be called", Throwable("cause")) }
                .map { _ -> fail(Throwable("map should never be called")) }
                .sorted { _, _ -> fail { "sorted should never be called" } }
                .count()
        assertEquals(0L, count)
    }

    @Test
    fun `fail usable as a sequence expression`() {
        val count =
            emptyList<Any>()
                .asSequence()
                .onEach { _ -> fail("peek should never be called") }
                .filter { _ -> fail("filter should never be called", Throwable("cause")) }
                .map { _ -> fail(Throwable("map should never be called")) }
                .count()
        assertEquals(0, count)
    }
}
