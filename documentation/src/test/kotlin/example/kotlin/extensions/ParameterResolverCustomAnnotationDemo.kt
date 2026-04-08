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
class ParameterResolverCustomAnnotationDemo {
    @Test
    fun testInt(
        @FirstInteger first: Int,
        @SecondInteger second: Int
    ) {
        assertEquals(1, first)
        assertEquals(2, second)
    }

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.RUNTIME)
    @ExtendWith(FirstIntegerExtension::class)
    annotation class FirstInteger

    class FirstIntegerExtension : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Boolean =
            parameterContext.parameter.type == Int::class.javaPrimitiveType &&
                !parameterContext.isAnnotated(SecondInteger::class.java)

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Any = 1
    }

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.RUNTIME)
    @ExtendWith(SecondIntegerExtension::class)
    annotation class SecondInteger

    class SecondIntegerExtension : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Boolean = parameterContext.isAnnotated(SecondInteger::class.java)

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Any = 2
    }
}
// end::user_guide[]
