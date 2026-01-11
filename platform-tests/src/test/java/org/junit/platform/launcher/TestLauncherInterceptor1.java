/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;

public class TestLauncherInterceptor1 implements LauncherInterceptor {

	public static final String CLASSLOADER_NAME = "interceptor-loader";

	private final ClassLoader originalClassLoader;
	private final URLClassLoader replacedClassLoader;

	public TestLauncherInterceptor1() {
		originalClassLoader = Thread.currentThread().getContextClassLoader();
		var url = getClass().getClassLoader().getResource("intercepted-testservices/");
		replacedClassLoader = new URLClassLoader(CLASSLOADER_NAME, new URL[] { url }, originalClassLoader);
		Thread.currentThread().setContextClassLoader(replacedClassLoader);
	}

	@Override
	public <T> T intercept(Invocation<T> invocation) {
		return invocation.proceed();
	}

	@Override
	public void close() {
		try {
			replacedClassLoader.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}
}
