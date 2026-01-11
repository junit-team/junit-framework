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

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * {@code TestExecutionExceptionHandler} defines the API for {@link Extension
 * Extensions} that wish to handle exceptions thrown during test execution.
 *
 * <p>In this context, <em>test execution</em> refers to the physical
 * invocation of a {@code @Test} method and not to any test-level extensions
 * or callbacks.
 *
 * <p>Common use cases include swallowing an exception if it's anticipated
 * or rolling back a transaction in certain error scenarios.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on
 * constructor requirements.
 *
 * @since 5.0
 * @see LifecycleMethodExecutionExceptionHandler
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface TestExecutionExceptionHandler extends Extension {

	/**
	 * Handle the supplied {@link Throwable throwable}.
	 *
	 * <p>Implementors must perform one of the following.
	 * <ol>
	 * <li>Swallow the supplied {@code throwable}, thereby preventing propagation.</li>
	 * <li>Rethrow the supplied {@code throwable} <em>as is</em>.</li>
	 * <li>Throw a new exception, potentially wrapping the supplied {@code throwable}.</li>
	 * </ol>
	 *
	 * <p>If the supplied {@code throwable} is swallowed, subsequent
	 * {@code TestExecutionExceptionHandlers} will not be invoked; otherwise,
	 * the next registered {@code TestExecutionExceptionHandler} (if there is
	 * one) will be invoked with any {@link Throwable} thrown by this handler.
	 *
	 * <p>Note that the {@link ExtensionContext#getExecutionException() execution
	 * exception} in the supplied {@code ExtensionContext} will <em>not</em>
	 * contain the {@code Throwable} thrown during invocation of the corresponding
	 * {@code @Test} method.
	 *
	 * @param context the current extension context; never {@code null}
	 * @param throwable the {@code Throwable} to handle; never {@code null}
	 */
	void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable;

}
