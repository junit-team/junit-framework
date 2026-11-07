/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.commons.support.ModifierSupport
import kotlin.coroutines.Continuation

class KotlinReflectionUtilsTests {
    @Test
    fun recognizesSuspendFunction() {
        val method =
            OpenTestMethodTestCase::class.java.getDeclaredMethod(
                "test",
                Continuation::class.java
            )

        assertFalse(ModifierSupport.isStatic(method))
        assertFalse(method.isSynthetic)

        assertTrue(KotlinReflectionUtils.isKotlinSuspendingFunction(method))
    }

    @Test
    fun doesNotRecognizeSyntheticMethodAsSuspendFunction() {
        val method =
            OpenTestMethodTestCase::class.java.getDeclaredMethod(
                "test\$suspendImpl",
                OpenTestMethodTestCase::class.java,
                Continuation::class.java
            )

        assertTrue(ModifierSupport.isStatic(method))
        assertTrue(method.isSynthetic)

        assertFalse(KotlinReflectionUtils.isKotlinSuspendingFunction(method))
    }

    @Test
    fun findsUnboxMethodOfStringBackedInlineValueClass() {
        val method = KotlinReflectionUtils.findInlineTypeUnboxMethod(StringBacked("value")).orElseThrow()

        assertEquals(String::class.java, method.returnType)
        assertEquals("value", ReflectionUtils.invokeMethod(method, StringBacked("value")))
    }

    @Test
    fun findsUnboxMethodOfPrimitiveBackedInlineValueClass() {
        val method = KotlinReflectionUtils.findInlineTypeUnboxMethod(LongBacked(42)).orElseThrow()

        assertEquals(Long::class.javaPrimitiveType, method.returnType)
        assertEquals(42L, ReflectionUtils.invokeMethod(method, LongBacked(42)))
    }

    @Test
    fun doesNotFindUnboxMethodForValuesThatAreNotInstancesOfInlineValueClasses() {
        assertTrue(KotlinReflectionUtils.findInlineTypeUnboxMethod(null).isEmpty)
        assertTrue(KotlinReflectionUtils.findInlineTypeUnboxMethod("value").isEmpty)
        assertTrue(KotlinReflectionUtils.findInlineTypeUnboxMethod(DataClass("value")).isEmpty)
    }

    @JvmInline
    value class StringBacked(
        val value: String
    )

    @JvmInline
    value class LongBacked(
        val value: Long
    )

    data class DataClass(
        val value: String
    )

    @Suppress("JUnitMalformedDeclaration")
    open class OpenTestMethodTestCase {
        @Test
        open suspend fun test() {
        }
    }
}
