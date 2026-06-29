/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Simple {@link TestExecutionListener} for logging informational messages
 * for all events via a {@link BiConsumer} that consumes {@code Throwable}
 * and {@code Supplier<String>}.
 *
 * @since 1.0
 * @see #forJavaUtilLogging()
 * @see #forJavaUtilLogging(Level)
 * @see LoggingListener#LoggingListener(BiConsumer)
 */
@API(status = MAINTAINED, since = "1.0")
public class LoggingListener implements TestExecutionListener {

	/**
	 * Create a {@code LoggingListener} which delegates to a
	 * {@link java.util.logging.Logger} using a log level of
	 * {@link Level#FINE FINE}.
	 *
	 * @see #forJavaUtilLogging(Level)
	 * @see #forBiConsumer(BiConsumer)
	 */
	public static LoggingListener forJavaUtilLogging() {
		return forJavaUtilLogging(Level.FINE);
	}

	/**
	 * Create a {@code LoggingListener} which delegates to a
	 * {@link java.util.logging.Logger} using the supplied
	 * {@linkplain Level log level}.
	 *
	 * @param logLevel the log level to use; never {@code null}
	 * @see #forJavaUtilLogging()
	 * @see #forBiConsumer(BiConsumer)
	 */
	public static LoggingListener forJavaUtilLogging(Level logLevel) {
		Preconditions.notNull(logLevel, "logLevel must not be null");
		Logger logger = Logger.getLogger(LoggingListener.class.getName());
		return new LoggingListener((t, messageSupplier) -> logger.log(logLevel, t, messageSupplier));
	}

	/**
	 * Create a {@code LoggingListener} which delegates to the supplied
	 * {@link BiConsumer} for consumption of logging messages.
	 *
	 * <p>The {@code BiConsumer's} arguments are a {@link Throwable} (potentially
	 * {@code null}) and a {@link Supplier} (never {@code null}) for the log
	 * message.
	 *
	 * @param logger a logger implemented as a {@code BiConsumer};
	 * never {@code null}
	 *
	 * @see #forJavaUtilLogging()
	 * @see #forJavaUtilLogging(Level)
	 */
	public static LoggingListener forBiConsumer(BiConsumer<@Nullable Throwable, Supplier<String>> logger) {
		return new LoggingListener(logger);
	}

	private final BiConsumer<@Nullable Throwable, Supplier<String>> logger;

	private LoggingListener(BiConsumer<@Nullable Throwable, Supplier<String>> logger) {
		Preconditions.notNull(logger, "logger must not be null");
		this.logger = logger;
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		log("TestPlan Execution Started: %s", testPlan);
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		log("TestPlan Execution Finished: %s", testPlan);
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		log("Dynamic Test Registered: %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId());
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		log("Execution Started: %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId());
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		log("Execution Skipped: %s - %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), reason);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		logWithThrowable("Execution Finished: %s - %s - %s", testExecutionResult.getThrowable().orElse(null),
			testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), testExecutionResult);
	}

	private void log(String message, Object... args) {
		logWithThrowable(message, null, args);
	}

	private void logWithThrowable(String message, @Nullable Throwable t, Object... args) {
		this.logger.accept(t, () -> message.formatted(args));
	}

}
