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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// tag::user_guide[]
interface ComparableContract<T : Comparable<T>> : Testable<T> {
    fun createSmallerValue(): T

    @Test
    fun returnsZeroWhenComparedToItself() {
        val value = createValue()
        assertEquals(0, value.compareTo(value))
    }

    @Test
    fun returnsPositiveNumberWhenComparedToSmallerValue() {
        val value = createValue()
        val smallerValue = createSmallerValue()
        assertTrue(value > smallerValue)
    }

    @Test
    fun returnsNegativeNumberWhenComparedToLargerValue() {
        val value = createValue()
        val smallerValue = createSmallerValue()
        assertTrue(smallerValue < value)
    }
}
// end::user_guide[]
