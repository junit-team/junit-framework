/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.interceptor

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method
import javax.swing.SwingUtilities

// tag::user_guide[]
class SwingEdtInterceptor : InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: Invocation<Void?>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        var throwable: Throwable? = null
        SwingUtilities.invokeAndWait {
            try {
                invocation.proceed()
            } catch (t: Throwable) {
                throwable = t
            }
        }
        throwable?.let { throw it }
    }
}
// end::user_guide[]
