/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.exception

import example.kotlin.exception.MultipleHandlersTestCase.ThirdExecutedHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler

// tag::user_guide[]
// Register handlers for @Test, @BeforeEach, @AfterEach as well as @BeforeAll and @AfterAll
@ExtendWith(ThirdExecutedHandler::class)
class MultipleHandlersTestCase {
    // Register handlers for @Test, @BeforeEach, @AfterEach only
    @ExtendWith(SecondExecutedHandler::class)
    @ExtendWith(FirstExecutedHandler::class)
    @Test
    fun testMethod() {
    }

    // end::user_guide[]

    class FirstExecutedHandler : TestExecutionExceptionHandler {
        override fun handleTestExecutionException(
            context: ExtensionContext,
            ex: Throwable
        ): Unit = throw ex
    }

    class SecondExecutedHandler : LifecycleMethodExecutionExceptionHandler {
        override fun handleBeforeEachMethodExecutionException(
            context: ExtensionContext,
            ex: Throwable
        ): Unit = throw ex
    }

    class ThirdExecutedHandler : LifecycleMethodExecutionExceptionHandler {
        override fun handleBeforeAllMethodExecutionException(
            context: ExtensionContext,
            ex: Throwable
        ): Unit = throw ex
    }
    // tag::user_guide[]
}
// end::user_guide[]
