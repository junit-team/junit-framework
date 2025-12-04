/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import org.jspecify.annotations.Nullable;

public class TestLauncherSessionListener implements LauncherSessionListener {

	public static @Nullable LauncherSession session;

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		TestLauncherSessionListener.session = session;
	}

	@Override
	public void launcherSessionClosed(LauncherSession session) {
		TestLauncherSessionListener.session = null;
	}
}
