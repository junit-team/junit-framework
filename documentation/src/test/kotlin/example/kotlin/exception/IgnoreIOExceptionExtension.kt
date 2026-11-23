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
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import java.io.IOException

// tag::user_guide[]
class IgnoreIOExceptionExtension : TestExecutionExceptionHandler {
    override fun handleTestExecutionException(
        context: ExtensionContext,
        throwable: Throwable
    ) {
        when (throwable) {
            is IOException -> return
            else -> throw throwable
        }
    }
}
// end::user_guide[]
