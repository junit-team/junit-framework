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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;

/**
 * Factory for the {@link Logger} facade for JUL.
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class LoggerFactory {

	private LoggerFactory() {
		/* no-op */
	}

	private static final Set<LogRecordListener> listeners = ConcurrentHashMap.newKeySet();

	/**
	 * Get a {@link Logger} for the specified class.
	 *
	 * @param clazz the class for which to get the logger; never {@code null}
	 * @return the logger
	 */
	@SuppressWarnings("ConstantValue")
	public static Logger getLogger(Class<?> clazz) {
		// NOTE: we cannot use org.junit.platform.commons.util.Preconditions here
		// since that would introduce a package cycle.
		if (clazz == null) {
			throw new JUnitException("Class must not be null");
		}

		return new DelegatingLogger(clazz.getName());
	}

	/**
	 * Add the supplied {@link LogRecordListener} to the set of registered
	 * listeners.
	 */
	public static void addListener(LogRecordListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove the supplied {@link LogRecordListener} from the set of registered
	 * listeners.
	 */
	public static void removeListener(LogRecordListener listener) {
		listeners.remove(listener);
	}

	private static final class DelegatingLogger implements Logger {

		private static final String FQCN = DelegatingLogger.class.getName();

		private final String name;

		private final java.util.logging.Logger julLogger;

		DelegatingLogger(String name) {
			this.name = name;
			this.julLogger = java.util.logging.Logger.getLogger(this.name);
		}

		@Override
		public void error(Supplier<String> messageSupplier) {
			log(Level.SEVERE, null, messageSupplier);
		}

		@Override
		public void error(@Nullable Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.SEVERE, throwable, messageSupplier);
		}

		@Override
		public void warn(Supplier<String> messageSupplier) {
			log(Level.WARNING, null, messageSupplier);
		}

		@Override
		public void warn(@Nullable Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.WARNING, throwable, messageSupplier);
		}

		@Override
		public void info(Supplier<String> messageSupplier) {
			log(Level.INFO, null, messageSupplier);
		}

		@Override
		public void info(@Nullable Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.INFO, throwable, messageSupplier);
		}

		@Override
		public void config(Supplier<String> messageSupplier) {
			log(Level.CONFIG, null, messageSupplier);
		}

		@Override
		public void config(@Nullable Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.CONFIG, throwable, messageSupplier);
		}

		@Override
		public void debug(Supplier<String> messageSupplier) {
			log(Level.FINE, null, messageSupplier);
		}

		@Override
		public void debug(@Nullable Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.FINE, throwable, messageSupplier);
		}

		@Override
		public void trace(Supplier<String> messageSupplier) {
			log(Level.FINER, null, messageSupplier);
		}

		@Override
		public void trace(@Nullable Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.FINER, throwable, messageSupplier);
		}

		private void log(Level level, @Nullable Throwable throwable, Supplier<String> messageSupplier) {
			boolean loggable = this.julLogger.isLoggable(level);
			if (loggable || !listeners.isEmpty()) {
				LogRecord logRecord = createLogRecord(level, throwable, messageSupplier.get());
				if (loggable) {
					this.julLogger.log(logRecord);
				}
				listeners.forEach(listener -> listener.logRecordSubmitted(logRecord));
			}
		}

		private LogRecord createLogRecord(Level level, @Nullable Throwable throwable, String message) {
			String sourceClassName = null;
			String sourceMethodName = null;
			boolean found = false;
			for (StackTraceElement element : new Throwable().getStackTrace()) {
				String className = element.getClassName();
				if (FQCN.equals(className)) {
					found = true;
				}
				else if (found) {
					sourceClassName = className;
					sourceMethodName = element.getMethodName();
					break;
				}
			}

			LogRecord logRecord = new LogRecord(level, message);
			logRecord.setLoggerName(this.name);
			logRecord.setThrown(throwable);
			logRecord.setSourceClassName(sourceClassName);
			logRecord.setSourceMethodName(sourceMethodName);
			logRecord.setResourceBundleName(this.julLogger.getResourceBundleName());
			logRecord.setResourceBundle(this.julLogger.getResourceBundle());

			return logRecord;
		}

	}

}
