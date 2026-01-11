/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.FieldSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.Month

/**
 * Tests for Kotlin compatibility of ParameterizedTest
 */
object ParameterizedTestKotlinSequenceIntegrationTests {
    @ParameterizedTest
    @MethodSource("dataProvidedByKotlinSequenceMethod")
    fun `a method source can be supplied by a Sequence-returning method`(
        value: Int,
        month: Month
    ) {
        assertEquals(value, month.value)
    }

    @JvmStatic
    private fun dataProvidedByKotlinSequenceMethod() = dataProvidedByKotlinSequenceField

    @JvmStatic
    val dataProvidedByKotlinSequenceField =
        sequenceOf(
            arguments(1, Month.JANUARY),
            arguments(3, Month.MARCH),
            arguments(8, Month.AUGUST),
            arguments(5, Month.MAY),
            arguments(12, Month.DECEMBER)
        )

    @ParameterizedTest
    @FieldSource("dataProvidedByKotlinSequenceField")
    fun `a field source can be supplied by a Sequence-typed field`(
        value: Int,
        month: Month
    ) {
        assertEquals(value, month.value)
    }
}
