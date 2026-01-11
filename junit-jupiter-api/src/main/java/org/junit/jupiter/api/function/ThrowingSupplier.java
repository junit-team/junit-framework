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
 * {@code ThrowingSupplier} is a functional interface that can be used to
 * implement any generic block of code that returns an object and
 * potentially throws a {@link Throwable}.
 *
 * <p>The {@code ThrowingSupplier} interface is similar to
 * {@link java.util.function.Supplier}, except that a {@code ThrowingSupplier}
 * can throw any kind of exception, including checked exceptions.
 *
 * <h2>Rationale for throwing {@code Throwable} instead of {@code Exception}</h2>
 *
 * <p>Although Java applications typically throw exceptions that are instances
 * of {@link Exception}, {@link RuntimeException},
 * {@link Error}, or {@link AssertionError} (in testing
 * scenarios), there may be use cases where a {@code ThrowingSupplier} needs to
 * explicitly throw a {@code Throwable}. In order to support such specialized
 * use cases, {@link #get} is declared to throw {@code Throwable}.
 *
 * @param <T> the type of argument supplied
 * @since 5.0
 * @see java.util.function.Supplier
 * @see org.junit.jupiter.api.Assertions#assertTimeout(java.time.Duration, ThrowingSupplier)
 * @see org.junit.jupiter.api.Assertions#assertTimeoutPreemptively(java.time.Duration, ThrowingSupplier)
 * @see Executable
 * @see ThrowingConsumer
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface ThrowingSupplier<T extends @Nullable Object> {

	/**
	 * Get a result, potentially throwing an exception.
	 *
	 * @return a result
	 */
	T get() throws Throwable;

}
