/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class MethodSourceParameterResolutionDemo {
    // tag::parameter_resolution_MethodSource_example[]
    @ParameterizedTest
    @MethodSource("factoryMethodWithArguments")
    fun testWithFactoryMethodWithArguments(argument: String) {
        assertTrue(argument.startsWith("2"))
    }

    class IntegerResolver : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Boolean = parameterContext.parameter.type == Int::class.javaPrimitiveType

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Any = 2
    }
    // end::parameter_resolution_MethodSource_example[]

    companion object {
        @JvmField
        @RegisterExtension
        val integerResolver = IntegerResolver()

        @JvmStatic
        fun factoryMethodWithArguments(quantity: Int): Stream<Arguments> =
            Stream.of(
                arguments("$quantity apples"),
                arguments("$quantity lemons")
            )
    }
}
