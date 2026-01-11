/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * Defines the strategy for closing custom class loaders created for test
 * discovery and execution.
 */
@API(status = INTERNAL, since = "1.13")
public enum CustomClassLoaderCloseStrategy {

	/**
	 * Close the custom class loader after calling the
	 * {@link org.junit.platform.launcher.Launcher} for test discovery or
	 * execution.
	 */
	CLOSE_AFTER_CALLING_LAUNCHER {

		@Override
		public void handle(ClassLoader customClassLoader) {
			if (customClassLoader instanceof @SuppressWarnings("resource") AutoCloseable closeable) {
				try {
					closeable.close();
				}
				catch (Exception ex) {
					throw new JUnitException("Failed to close custom class loader", ex);
				}
			}
		}
	},

	/**
	 * Rely on the JVM to release resources held by the custom class loader when
	 * it terminates.
	 *
	 * <p>This mode is only safe to use when calling {@link System#exit(int)}
	 * afterward.
	 */
	KEEP_OPEN {

		@Override
		public void handle(ClassLoader customClassLoader) {
			// do nothing
		}
	};

	/**
	 * Handle the class loader according to the strategy.
	 */
	public abstract void handle(ClassLoader classLoader);

}
