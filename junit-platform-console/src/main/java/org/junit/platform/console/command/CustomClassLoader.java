/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import java.net.URLClassLoader;

/**
 * Custom class loader that proxies a {@link URLClassLoader} but does
 * <em>not</em> implement {@link AutoCloseable} so user code is less likely to
 * accidentally close it.
 *
 * @since 6.2
 */
class CustomClassLoader extends ClassLoader {

	CustomClassLoader(URLClassLoader parent) {
		super(parent);
	}

	void close() throws Exception {
		((URLClassLoader) getParent()).close();
	}
}
