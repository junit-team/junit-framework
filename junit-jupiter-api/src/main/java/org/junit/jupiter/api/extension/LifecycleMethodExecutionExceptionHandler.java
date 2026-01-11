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
 * {@code LifecycleMethodExecutionExceptionHandler} defines the API for
 * {@link Extension Extensions} that wish to handle exceptions thrown during
 * the execution of {@code @BeforeAll}, {@code @BeforeEach}, {@code @AfterEach},
 * and {@code @AfterAll} lifecycle methods.
 *
 * <p>Common use cases include swallowing an exception if it's anticipated,
 * logging errors, or rolling back a transaction in certain error scenarios.
 *
 * <p>Implementations of this extension API must be registered at the class level
 * if exceptions thrown from {@code @BeforeAll} or {@code @AfterAll} methods are
 * to be handled. When registered at the test level, only exceptions thrown from
 * {@code @BeforeEach} or {@code @AfterEach} methods will be handled.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on constructor
 * requirements.
 *
 * <h2 id="implementation-guidelines">Implementation Guidelines</h2>
 *
 * <p>An implementation of an exception handler method defined in this API must
 * perform one of the following.
 *
 * <ol>
 * <li>Rethrow the supplied {@code Throwable} <em>as is</em>, which is the default implementation.</li>
 * <li>Swallow the supplied {@code Throwable}, thereby preventing propagation.</li>
 * <li>Throw a new exception, potentially wrapping the supplied {@code Throwable}.</li>
 * </ol>
 *
 * <p>If the supplied {@code Throwable} is swallowed by a handler method, subsequent
 * handler methods for the same lifecycle will not be invoked; otherwise, the
 * corresponding handler method of the next registered
 * {@code LifecycleMethodExecutionExceptionHandler} (if there is one) will be
 * invoked with any {@link Throwable} thrown by the previous handler.
 *
 * @since 5.5
 * @see TestExecutionExceptionHandler
 */
@API(status = STABLE, since = "5.10")
public interface LifecycleMethodExecutionExceptionHandler extends Extension {

	/**
	 * Handle the supplied {@link Throwable} that was thrown during execution of
	 * a {@code @BeforeAll} lifecycle method.
	 *
	 * <p>Please refer to the class-level Javadoc for
	 * <a href="#implementation-guidelines">Implementation Guidelines</a>.
	 *
	 * @param context the current extension context; never {@code null}
	 * @param throwable the {@code Throwable} to handle; never {@code null}
	 */
	default void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {

		throw throwable;
	}

	/**
	 * Handle the supplied {@link Throwable} that was thrown during execution of
	 * a {@code @BeforeEach} lifecycle method.
	 *
	 * <p>Please refer to the class-level Javadoc for
	 * <a href="#implementation-guidelines">Implementation Guidelines</a>.
	 *
	 * @param context the current extension context; never {@code null}
	 * @param throwable the {@code Throwable} to handle; never {@code null}
	 */
	default void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {

		throw throwable;
	}

	/**
	 * Handle the supplied {@link Throwable} that was thrown during execution of
	 * a {@code @AfterEach} lifecycle method.
	 *
	 * <p>Please refer to the class-level Javadoc for
	 * <a href="#implementation-guidelines">Implementation Guidelines</a>.
	 *
	 * @param context the current extension context; never {@code null}
	 * @param throwable the {@code Throwable} to handle; never {@code null}
	 */
	default void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {

		throw throwable;
	}

	/**
	 * Handle the supplied {@link Throwable} that was thrown during execution of
	 * a {@code @AfterAll} lifecycle method.
	 *
	 * <p>Please refer to the class-level Javadoc for
	 * <a href="#implementation-guidelines">Implementation Guidelines</a>.
	 *
	 * @param context the current extension context; never {@code null}
	 * @param throwable the {@code Throwable} to handle; never {@code null}
	 */
	default void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {

		throw throwable;
	}

}
