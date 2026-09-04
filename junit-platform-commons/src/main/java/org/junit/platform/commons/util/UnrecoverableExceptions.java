/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.Objects.requireNonNull;
import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Internal utilities for working with <em>unrecoverable</em> exceptions.
 *
 * <p><em>Unrecoverable</em> exceptions are those that should always terminate
 * test plan execution immediately.
 *
 * <h2>Currently Unrecoverable Exceptions</h2>
 * <ul>
 * <li>{@link OutOfMemoryError}</li>
 * </ul>
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.7
 */
@API(status = INTERNAL, since = "1.7")
public final class UnrecoverableExceptions {

	private UnrecoverableExceptions() {
		/* no-op */
	}

	/**
	 * Determine whether the supplied {@link Throwable exception} is
	 * <em>unrecoverable</em>.
	 *
	 * @param exception the exception to check; may be {@code null}
	 * @return {@code true} if the supplied {@code exception} is unrecoverable
	 */
	public static boolean isUnrecoverable(@Nullable Throwable exception) {
		return exception instanceof OutOfMemoryError;
	}

	/**
	 * Rethrow the supplied {@link Throwable exception} if it is
	 * <em>unrecoverable</em>.
	 *
	 * <p>If the supplied {@code exception} is not <em>unrecoverable</em>, this
	 * method does nothing.
	 */
	public static void rethrowIfUnrecoverable(@Nullable Throwable exception) {
		if (isUnrecoverable(exception)) {
			// NullAway cannot refine the nullability of the parameter, so we
			// use the implicit non-null argument of an unrecoverable exception.
			throw ExceptionUtils.throwAsUncheckedException(requireNonNull(exception));
		}
	}

}
