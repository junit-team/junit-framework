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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * {@code LogRecordListener} is only intended for testing purposes within
 * JUnit's own test suite.
 *
 * @since 1.1
 */
@API(status = INTERNAL, since = "1.1")
public class LogRecordListener {

	// capture log records by thread to support parallel test execution
	private final ThreadLocal<List<LogRecord>> logRecords = ThreadLocal.withInitial(ArrayList::new);

	/**
	 * Inform the listener of a {@link LogRecord} that was submitted to JUL for
	 * processing.
	 */
	public void logRecordSubmitted(LogRecord logRecord) {
		this.logRecords.get().add(logRecord);
	}

	/**
	 * Get a stream of {@link LogRecord log records} that have been
	 * {@linkplain #logRecordSubmitted submitted} to this listener by the
	 * current thread.
	 *
	 * <p>As stated in the Javadoc for {@code LogRecord}, a submitted
	 * {@code LogRecord} should not be updated by the client application. Thus,
	 * the {@code LogRecords} in the returned stream should only be inspected for
	 * testing purposes and not modified in any way.
	 *
	 * @see #stream(Level)
	 * @see #stream(Class)
	 * @see #stream(Class, Level)
	 */
	public Stream<LogRecord> stream() {
		return this.logRecords.get().stream();
	}

	/**
	 * Get a stream of {@link LogRecord log records} that have been
	 * {@linkplain #logRecordSubmitted submitted} to this listener by the current
	 * thread at the given log level.
	 *
	 * <p>As stated in the Javadoc for {@code LogRecord}, a submitted
	 * {@code LogRecord} should not be updated by the client application. Thus,
	 * the {@code LogRecords} in the returned stream should only be inspected for
	 * testing purposes and not modified in any way.
	 *
	 * @param level the log level for which to get the log records; never {@code null}
	 * @since 1.4
	 * @see #stream()
	 * @see #stream(Class)
	 * @see #stream(Class, Level)
	 */
	@SuppressWarnings({ "ConstantValue", "ReferenceEquality" })
	public Stream<LogRecord> stream(Level level) {
		// NOTE: we cannot use org.junit.platform.commons.util.Preconditions here
		// since that would introduce a package cycle.
		if (level == null) {
			throw new JUnitException("Level must not be null");
		}

		return stream().filter(logRecord -> logRecord.getLevel() == level);
	}

	/**
	 * Get a stream of {@link LogRecord log records} that have been
	 * {@linkplain #logRecordSubmitted submitted} to this listener by the current
	 * thread for the logger name equal to the name of the given class.
	 *
	 * <p>As stated in the Javadoc for {@code LogRecord}, a submitted
	 * {@code LogRecord} should not be updated by the client application. Thus,
	 * the {@code LogRecords} in the returned stream should only be inspected for
	 * testing purposes and not modified in any way.
	 *
	 * @param clazz the class for which to get the log records; never {@code null}
	 * @see #stream()
	 * @see #stream(Level)
	 * @see #stream(Class, Level)
	 */
	@SuppressWarnings("ConstantValue")
	public Stream<LogRecord> stream(Class<?> clazz) {
		// NOTE: we cannot use org.junit.platform.commons.util.Preconditions here
		// since that would introduce a package cycle.
		if (clazz == null) {
			throw new JUnitException("Class must not be null");
		}

		return stream().filter(logRecord -> logRecord.getLoggerName().equals(clazz.getName()));
	}

	/**
	 * Get a stream of {@link LogRecord log records} that have been
	 * {@linkplain #logRecordSubmitted submitted} to this listener by the current
	 * thread for the logger name equal to the name of the given class at the given
	 * log level.
	 *
	 * <p>As stated in the Javadoc for {@code LogRecord}, a submitted
	 * {@code LogRecord} should not be updated by the client application. Thus,
	 * the {@code LogRecords} in the returned stream should only be inspected for
	 * testing purposes and not modified in any way.
	 *
	 * @param clazz the class for which to get the log records; never {@code null}
	 * @param level the log level for which to get the log records; never {@code null}
	 * @see #stream()
	 * @see #stream(Level)
	 * @see #stream(Class)
	 */
	@SuppressWarnings({ "ConstantValue", "ReferenceEquality" })
	public Stream<LogRecord> stream(Class<?> clazz, Level level) {
		// NOTE: we cannot use org.junit.platform.commons.util.Preconditions here
		// since that would introduce a package cycle.
		if (level == null) {
			throw new JUnitException("Level must not be null");
		}

		return stream(clazz).filter(logRecord -> logRecord.getLevel() == level);
	}

	/**
	 * Clear all existing {@link LogRecord log records} that have been
	 * {@linkplain #logRecordSubmitted submitted} to this listener by the
	 * current thread.
	 */
	public void clear() {
		this.logRecords.get().clear();
	}

}
