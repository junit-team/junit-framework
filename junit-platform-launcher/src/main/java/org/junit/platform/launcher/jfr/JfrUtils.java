/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.jfr;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.launcher.Launcher;

/**
 * Internal utility for Java Flight Recorder (JFR) support.
 *
 * @since 6.0
 */
@API(status = INTERNAL, since = "6.0")
public class JfrUtils {

	public static void registerListeners(Launcher launcher) {
		if (isJfrAvailable()) {
			launcher.registerLauncherDiscoveryListeners(new FlightRecordingDiscoveryListener());
			launcher.registerTestExecutionListeners(new FlightRecordingExecutionListener());
		}
	}

	private static boolean isJfrAvailable() {
		return System.getProperty("org.graalvm.nativeimage.imagecode") == null //
				&& ReflectionSupport.tryToLoadClass("jdk.jfr.FlightRecorder").toOptional().isPresent();
	}

	private JfrUtils() {
	}
}
