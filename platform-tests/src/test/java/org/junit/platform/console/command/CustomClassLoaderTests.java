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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

/**
 * @since 6.2
 */
class CustomClassLoaderTests {

	@Test
	void customClassLoaderIsRegisteredAsParallelCapable() {
		var customClassLoader = new CustomClassLoader(URLClassLoader.newInstance(new URL[0]));
		assertTrue(customClassLoader.isRegisteredAsParallelCapable());
	}

}
