/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.platform.commons.util

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

    @Suppress("JUnitMalformedDeclaration")
    open class OpenTestMethodTestCase {
        @Test
        open suspend fun test() {
        }
    }
}
