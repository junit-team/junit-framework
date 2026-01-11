/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners.session;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Collection of {@code static} factory methods for creating
 * {@link LauncherSessionListener LauncherSessionListeners}.
 *
 * @since 1.8
 */
@API(status = INTERNAL, since = "1.8")
public class LauncherSessionListeners {

	private LauncherSessionListeners() {
	}

	public static LauncherSessionListener composite(List<LauncherSessionListener> listeners) {
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listeners must not contain any null elements");
		if (listeners.isEmpty()) {
			return LauncherSessionListener.NOOP;
		}
		if (listeners.size() == 1) {
			return listeners.get(0);
		}
		return new CompositeLauncherSessionListener(listeners);
	}

}
