/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.function;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * {@code ThrowingConsumer} is a functional interface that can be used to
 * implement any generic block of code that consumes an argument and
 * potentially throws a {@link Throwable}.
 *
 * <p>The {@code ThrowingConsumer} interface is similar to
 * {@link java.util.function.Consumer}, except that a {@code ThrowingConsumer}
 * can throw any kind of exception, including checked exceptions.
 *
 * <h2>Rationale for throwing {@code Throwable} instead of {@code Exception}</h2>
 *
 * <p>Although Java applications typically throw exceptions that are instances
 * of {@link java.lang.Exception}, {@link java.lang.RuntimeException},
 * {@link java.lang.Error}, or {@link java.lang.AssertionError} (in testing
 * scenarios), there may be use cases where a {@code ThrowingConsumer} needs to
 * explicitly throw a {@code Throwable}. In order to support such specialized
 * use cases, {@link #accept} is declared to throw {@code Throwable}.
 *
 * @param <T> the type of argument consumed
 * @since 5.0
 * @see java.util.function.Consumer
 * @see org.junit.jupiter.api.DynamicTest#stream
 * @see Executable
 * @see ThrowingSupplier
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface ThrowingConsumer<T extends @Nullable Object> {

	/**
	 * Consume the supplied argument, potentially throwing an exception.
	 *
	 * @param t the argument to consume
	 */
	void accept(T t) throws Throwable;

}
