/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

// @formatter:off
// tag::user_guide[]
public class SwingEdtInterceptor implements InvocationInterceptor {

	//end::user_guide[]
	@SuppressWarnings("NullAway")
	//tag::user_guide[]
	@Override
	public void interceptTestMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		AtomicReference<Throwable> throwable = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			try {
				invocation.proceed();
			}
			catch (Throwable t) {
				throwable.set(t);
			}
		});
		Throwable t = throwable.get();
		if (t != null) {
			throw t;
		}
	}
}
// end::user_guide[]
// @formatter:on
