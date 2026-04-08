/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.extensions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

// tag::user_guide[]
class ParameterResolverCustomTypeDemo {
    @Test
    @ExtendWith(FirstIntegerResolver::class, SecondIntegerResolver::class)
    fun testInt(
        i: Int,
        wrappedInteger: WrappedInteger
    ) {
        assertEquals(1, i)
        assertEquals(2, wrappedInteger.value)
    }

    class FirstIntegerResolver : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Boolean = parameterContext.parameter.type == Int::class.javaPrimitiveType

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Any = 1
    }

    class SecondIntegerResolver : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Boolean = parameterContext.parameter.type == WrappedInteger::class.java

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Any = WrappedInteger(2)
    }

    data class WrappedInteger(
        val value: Int
    )
}
// end::user_guide[]
