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
import org.junit.jupiter.api.ClassTemplate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.util.stream.Stream

// tag::user_guide[]
@ClassTemplate
@ExtendWith(ClassTemplateDemo.MyClassTemplateInvocationContextProvider::class)
class ClassTemplateDemo {
    private var fruit: String? = null

    @Test
    fun notNull() {
        assertNotNull(fruit)
    }

    @Test
    fun wellKnown() {
        assertTrue(fruit in WELL_KNOWN_FRUITS)
    }

    class MyClassTemplateInvocationContextProvider : ClassTemplateInvocationContextProvider {
        override fun supportsClassTemplate(context: ExtensionContext) = true

        override fun provideClassTemplateInvocationContexts(context: ExtensionContext): Stream<ClassTemplateInvocationContext> =
            Stream.of(invocationContext("apple"), invocationContext("banana"))

        private fun invocationContext(parameter: String) =
            object : ClassTemplateInvocationContext {
                override fun getDisplayName(invocationIndex: Int) = parameter

                override fun getAdditionalExtensions(): List<Extension> =
                    listOf(
                        TestInstancePostProcessor { testInstance, _ ->
                            (testInstance as ClassTemplateDemo).fruit = parameter
                        }
                    )
            }
    }

    companion object {
        val WELL_KNOWN_FRUITS = listOf("apple", "banana", "lemon")
    }
}
// end::user_guide[]
