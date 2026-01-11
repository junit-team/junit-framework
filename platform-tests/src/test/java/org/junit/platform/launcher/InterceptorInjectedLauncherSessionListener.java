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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InterceptorInjectedLauncherSessionListener implements LauncherSessionListener {

	public static int CALLS;

	public InterceptorInjectedLauncherSessionListener() {
		assertEquals(TestLauncherInterceptor1.CLASSLOADER_NAME,
			Thread.currentThread().getContextClassLoader().getName());
		assertTrue(TestLauncherInterceptor2.INTERCEPTING);
	}

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		CALLS++;
	}

	@Override
	public void launcherSessionClosed(LauncherSession session) {
		assertEquals(TestLauncherInterceptor1.CLASSLOADER_NAME,
			Thread.currentThread().getContextClassLoader().getName());
	}
}
