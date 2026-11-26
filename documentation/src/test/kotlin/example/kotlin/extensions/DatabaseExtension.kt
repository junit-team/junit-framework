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

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.store.Namespace

// tag::user_guide[]
class DatabaseExtension :
    BeforeAllCallback,
    ParameterResolver {
    override fun beforeAll(context: ExtensionContext) {
        val database = getOrCreateDatabase(context) // <1>
        database.insert("testClass", context.requiredTestClass.simpleName)
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean = Database::class.java == parameterContext.parameter.type

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any = getOrCreateDatabase(extensionContext) // <2>

    private fun getOrCreateDatabase(context: ExtensionContext): Database =
        context.getStore(NAMESPACE).computeIfAbsent(Database::class.java, {
            Database().apply { insert("greeting", "Hello") } // <3>
        }, Database::class.java)

    companion object {
        private val NAMESPACE = Namespace.create(DatabaseExtension::class.java)
    }
}
// end::user_guide[]
