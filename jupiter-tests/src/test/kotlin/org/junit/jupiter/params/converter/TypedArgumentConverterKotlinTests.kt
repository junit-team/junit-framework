/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.jupiter.params.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ParameterContext
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TypedArgumentConverterKotlinTests {
    @Test
    fun converts() {
        val parameterContext = mock(ParameterContext::class.java)
        val parameter = this.javaClass.getDeclaredMethod("foo", String::class.java).parameters[0]
        `when`(parameterContext.parameter).thenReturn(parameter)

        assertNull(NullableTypeConverter().convert(null, parameterContext))
        assertEquals("null", NonNullableTypeConverter().convert(null, parameterContext))
    }

    @Suppress("unused")
    private fun foo(param: String) = Unit

    class NullableTypeConverter : TypedArgumentConverter<String, String?>(String::class.java, String::class.java) {
        override fun convert(source: String?) = source
    }

    class NonNullableTypeConverter : TypedArgumentConverter<String, String>(String::class.java, String::class.java) {
        override fun convert(source: String?) = source.toString()
    }
}
