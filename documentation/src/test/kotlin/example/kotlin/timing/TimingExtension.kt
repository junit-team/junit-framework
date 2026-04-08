/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.timing

// tag::user_guide[]
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store
import java.util.logging.Logger

// end::user_guide[]

// tag::user_guide[]
class TimingExtension :
    BeforeTestExecutionCallback,
    AfterTestExecutionCallback {
    private val logger: Logger = Logger.getLogger(TimingExtension::class.java.name)

    override fun beforeTestExecution(context: ExtensionContext) {
        getStore(context).put(START_TIME, System.currentTimeMillis())
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val testMethod = context.requiredTestMethod
        val startTime = getStore(context).remove(START_TIME, Long::class.javaObjectType)!!
        val duration = System.currentTimeMillis() - startTime

        logger.info { "Method [${testMethod.name}] took $duration ms." }
    }

    private fun getStore(context: ExtensionContext): Store = context.getStore(Namespace.create(javaClass, context.requiredTestMethod))

    companion object {
        private const val START_TIME = "start time"
    }
}
// end::user_guide[]
