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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/**
 * @since 1.0
 */
class CustomContextClassLoaderExecutorTests {

	@Test
	void invokeWithoutCustomClassLoaderDoesNotSetClassLoader() {
		var originalClassLoader = Thread.currentThread().getContextClassLoader();
		var executor = new CustomContextClassLoaderExecutor(Optional.empty());

		int result = executor.invoke(() -> {
			assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
			return 42;
		});

		assertEquals(42, result);
		assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
	}

	@Test
	void invokeWithCustomClassLoaderSetsCustomAndResetsToOriginal() {
		var originalClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader customClassLoader = URLClassLoader.newInstance(new URL[0]);
		var executor = new CustomContextClassLoaderExecutor(Optional.of(customClassLoader));

		int result = executor.invoke(() -> {
			assertSame(customClassLoader, Thread.currentThread().getContextClassLoader());
			return 23;
		});

		assertEquals(23, result);
		assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
	}

	@Test
	void invokeWithCustomClassLoaderAndEnsureItIsClosedAfterUsage() {
		var closed = new AtomicBoolean(false);
		ClassLoader localClassLoader = new URLClassLoader(new URL[0]) {
			@Override
			public void close() throws IOException {
				closed.set(true);
				super.close();
			}
		};
		var executor = new CustomContextClassLoaderExecutor(Optional.of(localClassLoader));

		int result = executor.invoke(() -> 4711);

		assertEquals(4711, result);
		assertTrue(closed.get());
	}

	@Test
	void invokeWithCustomClassLoaderAndKeepItOpenAfterUsage() {
		var closed = new AtomicBoolean(false);
		ClassLoader localClassLoader = new URLClassLoader(new URL[0]) {
			@Override
			public void close() throws IOException {
				closed.set(true);
				super.close();
			}
		};
		var executor = new CustomContextClassLoaderExecutor(Optional.of(localClassLoader),
			CustomClassLoaderCloseStrategy.KEEP_OPEN);

		int result = executor.invoke(() -> 4711);

		assertEquals(4711, result);
		assertFalse(closed.get());
	}
}
