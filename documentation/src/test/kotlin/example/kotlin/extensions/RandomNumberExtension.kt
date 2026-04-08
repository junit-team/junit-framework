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

// tag::user_guide[]
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields
import org.junit.platform.commons.support.ModifierSupport
import java.lang.reflect.Field
import java.util.function.Predicate

class RandomNumberExtension :
    BeforeAllCallback,
    TestInstancePostProcessor,
    ParameterResolver {
    private val random = java.util.Random(System.nanoTime())

    /**
     * Inject a random integer into static fields that are annotated with
     * `@Random` and can be assigned an integer value.
     */
    override fun beforeAll(context: ExtensionContext) {
        val testClass = context.requiredTestClass
        injectFields(testClass, null, ModifierSupport::isStatic)
    }

    /**
     * Inject a random integer into non-static fields that are annotated with
     * `@Random` and can be assigned an integer value.
     */
    override fun postProcessTestInstance(
        testInstance: Any,
        context: ExtensionContext
    ) {
        val testClass = context.requiredTestClass
        injectFields(testClass, testInstance, ModifierSupport::isNotStatic)
    }

    /**
     * Determine if the parameter is annotated with `@Random` and can be
     * assigned an integer value.
     */
    override fun supportsParameter(
        pc: ParameterContext,
        ec: ExtensionContext
    ): Boolean = pc.isAnnotated(Random::class.java) && isInteger(pc.parameter.type)

    /**
     * Resolve a random integer.
     */
    override fun resolveParameter(
        pc: ParameterContext,
        ec: ExtensionContext
    ): Int = random.nextInt()

    private fun injectFields(
        testClass: Class<*>,
        testInstance: Any?,
        predicate: Predicate<Field>
    ) {
        val combined = predicate.and { field -> isInteger(field.type) }
        findAnnotatedFields(testClass, Random::class.java, combined)
            .forEach { field ->
                field.isAccessible = true
                field.set(testInstance, random.nextInt())
            }
    }

    private fun isInteger(type: Class<*>): Boolean = type == Int::class.javaObjectType || type == Int::class.javaPrimitiveType
}
// end::user_guide[]
