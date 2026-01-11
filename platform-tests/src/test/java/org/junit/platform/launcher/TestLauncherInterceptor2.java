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

public class TestLauncherInterceptor2 implements LauncherInterceptor {

	public static boolean INTERCEPTING;

	@Override
	public <T> T intercept(Invocation<T> invocation) {
		INTERCEPTING = true;
		try {
			return invocation.proceed();
		}
		finally {
			INTERCEPTING = false;
		}
	}

	@Override
	public void close() {
	}
}
