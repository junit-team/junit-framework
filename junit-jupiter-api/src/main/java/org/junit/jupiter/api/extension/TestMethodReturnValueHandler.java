/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;

/**
 * {@code TestMethodReturnValueHandler} allows extensions to support
 * {@link org.junit.jupiter.api.Test @Test} methods that return a non-void
 * value.
 *
 * <p>By default, JUnit Jupiter requires {@code @Test} methods to return
 * {@code void}. A registered {@code TestMethodReturnValueHandler} relaxes
 * this requirement for return types it {@linkplain #supportsReturnType
 * supports}. The handler is responsible for invoking the test method
 * (via {@link InvocationInterceptor.Invocation#proceed()
 * invocation.proceed()}) and processing the return value.
 *
 * <p>Because the handler controls the invocation, it can set up a
 * custom execution context before calling {@code proceed()} &mdash; for
 * example, running the test method on a specific thread or executor.
 *
 * <p>Implementations are discovered via Java's {@link java.util.ServiceLoader}
 * mechanism and must be registered in
 * {@code META-INF/services/org.junit.jupiter.api.extension.TestMethodReturnValueHandler}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class CompletableFutureHandler implements TestMethodReturnValueHandler {
 *
 *     @Override
 *     public boolean supportsReturnType(Class<?> returnType) {
 *         return CompletableFuture.class.isAssignableFrom(returnType);
 *     }
 *
 *     @Override
 *     public void execute(InvocationInterceptor.Invocation<Object> invocation,
 *             ExtensionContext context) throws Throwable {
 *         Object result = invocation.proceed();
 *         if (result != null) {
 *             ((CompletableFuture<?>) result).get(30, TimeUnit.SECONDS);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 6.2
 * @see org.junit.jupiter.api.Test
 */
@API(status = EXPERIMENTAL, since = "6.2")
public interface TestMethodReturnValueHandler extends Extension {

	/**
	 * Determine if this handler supports the supplied return type.
	 *
	 * @param returnType the return type of the test method; never {@code null}
	 * @return {@code true} if this handler can handle the return type
	 */
	boolean supportsReturnType(Class<?> returnType);

	/**
	 * Execute a {@code @Test} method that returns a supported type.
	 *
	 * <p>The handler must call {@link InvocationInterceptor.Invocation#proceed()
	 * invocation.proceed()} to invoke the test method and obtain the return
	 * value. The handler is then responsible for processing the result
	 * &mdash; for example, subscribing to a reactive type and awaiting
	 * completion.
	 *
	 * <p>Because the handler controls when {@code proceed()} is called, it
	 * can set up a custom execution context beforehand &mdash; for example,
	 * running the test on a specific thread or executor.
	 *
	 * <p>If this method throws, the test is marked as failed with the thrown
	 * exception.
	 *
	 * @param invocation the invocation that executes the test method;
	 * calling {@code proceed()} invokes the method and returns its result;
	 * never {@code null}
	 * @param context the current extension context; never {@code null}
	 * @throws Throwable if the return value indicates a test failure
	 */
	void execute(InvocationInterceptor.Invocation<Object> invocation, ExtensionContext context) throws Throwable;

}
