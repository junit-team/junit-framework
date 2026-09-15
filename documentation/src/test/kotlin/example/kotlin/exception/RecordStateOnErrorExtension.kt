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

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler

// tag::user_guide[]
class RecordStateOnErrorExtension : LifecycleMethodExecutionExceptionHandler {
    override fun handleBeforeAllMethodExecutionException(
        context: ExtensionContext,
        ex: Throwable
    ) {
        memoryDumpForFurtherInvestigation("Failure recorded during class setup")
        throw ex
    }

    override fun handleBeforeEachMethodExecutionException(
        context: ExtensionContext,
        ex: Throwable
    ) {
        memoryDumpForFurtherInvestigation("Failure recorded during test setup")
        throw ex
    }

    override fun handleAfterEachMethodExecutionException(
        context: ExtensionContext,
        ex: Throwable
    ) {
        memoryDumpForFurtherInvestigation("Failure recorded during test cleanup")
        throw ex
    }

    override fun handleAfterAllMethodExecutionException(
        context: ExtensionContext,
        ex: Throwable
    ) {
        memoryDumpForFurtherInvestigation("Failure recorded during class cleanup")
        throw ex
    }
    // end::user_guide[]

    private fun memoryDumpForFurtherInvestigation(error: String) {
    }
    // tag::user_guide[]
}
// end::user_guide[]
