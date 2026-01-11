/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners.discovery;

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.LauncherDiscoveryListener;

/**
 * Collection of {@code static} factory methods for creating
 * {@link LauncherDiscoveryListener LauncherDiscoveryListeners}.
 *
 * @since 1.6
 */
@API(status = STABLE, since = "1.10")
public class LauncherDiscoveryListeners {

	private LauncherDiscoveryListeners() {
	}

	/**
	 * Create a {@link LauncherDiscoveryListener} that aborts test discovery on
	 * failures.
	 *
	 * <p>The following events are considered failures:
	 *
	 * <ul>
	 *     <li>
	 *         any recoverable {@link Throwable} thrown by
	 *         {@link TestEngine#discover}.
	 *     </li>
	 * </ul>
	 */
	public static LauncherDiscoveryListener abortOnFailure() {
		return new AbortOnFailureLauncherDiscoveryListener();
	}

	/**
	 * Create a {@link LauncherDiscoveryListener} that logs test discovery
	 * events based on their severity.
	 *
	 * <p>For example, failures during test discovery are logged as errors.
	 */
	public static LauncherDiscoveryListener logging() {
		return new LoggingLauncherDiscoveryListener();
	}

	@API(status = INTERNAL, since = "1.6")
	public static LauncherDiscoveryListener composite(List<LauncherDiscoveryListener> listeners) {
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listeners must not contain any null elements");
		if (listeners.isEmpty()) {
			return LauncherDiscoveryListener.NOOP;
		}
		if (listeners.size() == 1) {
			return listeners.get(0);
		}
		return new CompositeLauncherDiscoveryListener(listeners);
	}

	@API(status = INTERNAL, since = "1.6")
	public static LauncherDiscoveryListener fromConfigurationParameter(String key, String value) {
		return Arrays.stream(LauncherDiscoveryListenerType.values()) //
				.filter(type -> type.parameterValue.equalsIgnoreCase(value)) //
				.findFirst() //
				.map(type -> type.creator.get()) //
				.orElseThrow(() -> newInvalidConfigurationParameterException(key, value));
	}

	private static JUnitException newInvalidConfigurationParameterException(String key, String value) {
		String allowedValues = Arrays.stream(LauncherDiscoveryListenerType.values()) //
				.map(type -> type.parameterValue) //
				.collect(joining("', '", "'", "'"));
		return new JUnitException("Invalid value of configuration parameter '" + key + "': " //
				+ value + " (allowed are " + allowedValues + ")");
	}

	private enum LauncherDiscoveryListenerType {

		LOGGING("logging", LauncherDiscoveryListeners::logging),

		ABORT_ON_FAILURE("abortOnFailure", LauncherDiscoveryListeners::abortOnFailure);

		private final String parameterValue;
		private final Supplier<LauncherDiscoveryListener> creator;

		LauncherDiscoveryListenerType(String parameterValue, Supplier<LauncherDiscoveryListener> creator) {
			this.parameterValue = parameterValue;
			this.creator = creator;
		}

	}

}
