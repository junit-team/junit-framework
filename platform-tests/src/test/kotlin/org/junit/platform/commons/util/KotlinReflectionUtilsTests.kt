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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.commons.JUnitException
import org.junit.platform.commons.support.ModifierSupport
import kotlin.coroutines.Continuation

class KotlinReflectionUtilsTests {
    @Nested
    inner class Suspend {
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
    }

    @Suppress("JUnitMalformedDeclaration")
    open class OpenTestMethodTestCase {
        @Test
        open suspend fun test() {
        }
    }

    @Nested
    inner class InlineValueType {
        @Test
        fun recognizesInstanceOfInlineType() {
            assertFalse(KotlinReflectionUtils.isInstanceOfInlineType(null))
            assertFalse(KotlinReflectionUtils.isInstanceOfInlineType(1))
            assertTrue(KotlinReflectionUtils.isInstanceOfInlineType(1u))
        }

        @Test
        fun recognizesNonInlineTypeAssignableTo() {
            assertFalse(KotlinReflectionUtils.isInlineTypeAssignableTo(null, Int::class.java))
            assertFalse(KotlinReflectionUtils.isInlineTypeAssignableTo(1, Int::class.java))
        }

        @Test
        fun recognizesInlineTypeAssignableToRequiresKotlinReflect() {
            val exception =
                assertThrows(JUnitException::class) {
                    KotlinReflectionUtils.isInlineTypeAssignableTo(1u, Int::class.java)
                }
            assertThat(exception).hasMessage(
                "Kotlin inline value type [class kotlin.UInt] requires " +
                    "org.jetbrains.kotlin:kotlin-reflect to be on the classpath or module path. " +
                    "Please add a corresponding dependency."
            )
        }
    }
}
