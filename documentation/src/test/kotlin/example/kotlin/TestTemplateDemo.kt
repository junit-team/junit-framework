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
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import java.util.stream.Stream

class TestTemplateDemo {
    // tag::user_guide[]
    val fruits = listOf("apple", "banana", "lemon")

    @TestTemplate
    @ExtendWith(MyTestTemplateInvocationContextProvider::class)
    fun testTemplate(fruit: String) {
        assertTrue(fruit in fruits)
    }

    class MyTestTemplateInvocationContextProvider : TestTemplateInvocationContextProvider {
        override fun supportsTestTemplate(context: ExtensionContext) = true

        override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> =
            Stream.of(invocationContext("apple"), invocationContext("banana"))

        private fun invocationContext(parameter: String) =
            object : TestTemplateInvocationContext {
                override fun getDisplayName(invocationIndex: Int) = parameter

                override fun getAdditionalExtensions(): List<Extension> =
                    listOf(
                        object : ParameterResolver {
                            override fun supportsParameter(
                                parameterContext: ParameterContext,
                                extensionContext: ExtensionContext
                            ) = parameterContext.parameter.type == String::class.java

                            override fun resolveParameter(
                                parameterContext: ParameterContext,
                                extensionContext: ExtensionContext
                            ): Any = parameter
                        }
                    )
            }
    }
    // end::user_guide[]
}
