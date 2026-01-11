/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.logging;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * The {@code Logger} API serves as a simple logging facade for
 * {@code java.util.logging} (JUL).
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public interface Logger {

	/**
	 * Log the message from the provided {@code messageSupplier} at error level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#SEVERE} in JUL.
	 */
	void error(Supplier<String> messageSupplier);

	/**
	 * Log the provided {@code Throwable} and message from the provided
	 * {@code messageSupplier} at error level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#SEVERE} in JUL.
	 */
	void error(@Nullable Throwable throwable, Supplier<String> messageSupplier);

	/**
	 * Log the message from the provided {@code messageSupplier} at warning level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#WARNING} in JUL.
	 */
	void warn(Supplier<String> messageSupplier);

	/**
	 * Log the provided {@code Throwable} and message from the provided
	 * {@code messageSupplier} at warning level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#WARNING} in JUL.
	 */
	void warn(@Nullable Throwable throwable, Supplier<String> messageSupplier);

	/**
	 * Log the message from the provided {@code messageSupplier} at info level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#INFO} in JUL.
	 */
	void info(Supplier<String> messageSupplier);

	/**
	 * Log the provided {@code Throwable} and message from the provided
	 * {@code messageSupplier} at info level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#INFO} in JUL.
	 */
	void info(@Nullable Throwable throwable, Supplier<String> messageSupplier);

	/**
	 * Log the message from the provided {@code messageSupplier} at config level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#CONFIG} in JUL.
	 */
	void config(Supplier<String> messageSupplier);

	/**
	 * Log the provided {@code Throwable} and message from the provided
	 * {@code messageSupplier} at config level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#CONFIG} in JUL.
	 */
	void config(@Nullable Throwable throwable, Supplier<String> messageSupplier);

	/**
	 * Log the message from the provided {@code messageSupplier} at debug level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#FINE} in JUL.
	 */
	void debug(Supplier<String> messageSupplier);

	/**
	 * Log the provided {@code Throwable} and message from the provided
	 * {@code messageSupplier} at debug level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#FINE} in JUL.
	 */
	void debug(@Nullable Throwable throwable, Supplier<String> messageSupplier);

	/**
	 * Log the message from the provided {@code messageSupplier} at trace level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#FINER} in JUL.
	 */
	void trace(Supplier<String> messageSupplier);

	/**
	 * Log the provided {@code Throwable} and message from the provided
	 * {@code messageSupplier} at trace level.
	 *
	 * <p>Maps to {@link java.util.logging.Level#FINER} in JUL.
	 */
	void trace(@Nullable Throwable throwable, Supplier<String> messageSupplier);

}
