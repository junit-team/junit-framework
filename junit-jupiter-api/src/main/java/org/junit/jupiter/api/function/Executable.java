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

/**
 * {@code Executable} is a functional interface that can be used to
 * implement any generic block of code that potentially throws a
 * {@link Throwable}.
 *
 * <p>The {@code Executable} interface is similar to {@link java.lang.Runnable},
 * except that an {@code Executable} can throw any kind of exception.
 *
 * <h2>Rationale for throwing {@code Throwable} instead of {@code Exception}</h2>
 *
 * <p>Although Java applications typically throw exceptions that are instances
 * of {@link java.lang.Exception}, {@link java.lang.RuntimeException},
 * {@link java.lang.Error}, or {@link java.lang.AssertionError} (in testing
 * scenarios), there may be use cases where an {@code Executable} needs to
 * explicitly throw a {@code Throwable}. In order to support such specialized
 * use cases, {@link #execute()} is declared to throw {@code Throwable}.
 *
 * @since 5.0
 * @see org.junit.jupiter.api.Assertions#assertAll(Executable...)
 * @see org.junit.jupiter.api.Assertions#assertAll(String, Executable...)
 * @see org.junit.jupiter.api.Assertions#assertThrows(Class, Executable)
 * @see org.junit.jupiter.api.Assumptions#assumingThat(java.util.function.BooleanSupplier, Executable)
 * @see org.junit.jupiter.api.DynamicTest#dynamicTest(String, Executable)
 * @see ThrowingConsumer
 * @see ThrowingSupplier
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface Executable {

	void execute() throws Throwable;

}
