/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Utility class for executing code with a temporary classpath.
 *
 * @since 5.13
 */
public class TemporaryClasspathExecutor {

	private TemporaryClasspathExecutor() {
	}

	/**
	 * Execute the {@link Runnable} within a custom classpath, temporarily modifying the
	 * thread's {@link Thread#getContextClassLoader() context class loader} to include
	 * the provided {@code classpathRoot}.
	 *
	 * <p>After the given {@code Runnable} completes, the original context class loader is
	 * restored.
	 *
	 * @param classpathRoot the root path to be added to the classpath, resolved relative
	 * to the current thread's context class loader.
	 * @param runnable the {@code Runnable} to execute with the temporary classpath.
	 */
	public static void withAdditionalClasspathRoot(String classpathRoot, Runnable runnable) {
		var current = Thread.currentThread().getContextClassLoader();
		try (var classLoader = new URLClassLoader(new URL[] { current.getResource(classpathRoot) }, current)) {
			Thread.currentThread().setContextClassLoader(classLoader);
			runnable.run();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}

}
