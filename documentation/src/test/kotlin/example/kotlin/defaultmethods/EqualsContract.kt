/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.defaultmethods

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

// tag::user_guide[]
interface EqualsContract<T> : Testable<T> {
    fun createNotEqualValue(): T

    @Test
    fun valueEqualsItself() {
        val value = createValue()
        assertEquals(value, value)
    }

    @Test
    fun valueDoesNotEqualNull() {
        val value = createValue()
        assertNotEquals(null, value)
    }

    @Test
    fun valueDoesNotEqualDifferentValue() {
        val value = createValue()
        val differentValue = createNotEqualValue()
        assertNotEquals(value, differentValue)
        assertNotEquals(differentValue, value)
    }
}
// end::user_guide[]
