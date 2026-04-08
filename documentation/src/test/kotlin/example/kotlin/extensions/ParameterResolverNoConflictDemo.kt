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
class ParameterResolverNoConflictDemo {
    @Test
    @ExtendWith(FirstIntegerResolver::class)
    fun firstResolution(i: Int) {
        assertEquals(1, i)
    }

    @Test
    @ExtendWith(SecondIntegerResolver::class)
    fun secondResolution(i: Int) {
        assertEquals(2, i)
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
        ): Boolean = parameterContext.parameter.type == Int::class.javaPrimitiveType

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Any = 2
    }
}
// end::user_guide[]
