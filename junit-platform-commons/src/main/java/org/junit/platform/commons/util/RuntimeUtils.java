/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * Collection of utilities for working with {@link Runtime},
 * {@link java.lang.management.RuntimeMXBean}, etc.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.6
 */
@API(status = INTERNAL, since = "1.6")
public final class RuntimeUtils {

	private RuntimeUtils() {
		/* no-op */
	}

	/**
	 * Try to determine whether the VM was started in debug mode or not.
	 */
	public static boolean isDebugMode() {
		return getInputArguments() //
				.map(args -> args.stream().anyMatch(
					arg -> arg.startsWith("-agentlib:jdwp") || arg.startsWith("-Xrunjdwp"))) //
				.orElse(false);
	}

	/**
	 * Try to get the input arguments the VM was started with.
	 */
	static Optional<List<String>> getInputArguments() {
		Optional<Class<?>> managementFactoryClass = ReflectionUtils.tryToLoadClass(
			"java.lang.management.ManagementFactory").toOptional();
		if (managementFactoryClass.isEmpty()) {
			return Optional.empty();
		}
		// Can't use "java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()"
		// directly as module "java.management" might not be available and/or the current platform
		// doesn't support the Java Management Extensions (JMX) API (like Android?).
		// See https://github.com/junit-team/junit4/pull/1187
		try {
			Object bean = managementFactoryClass.get().getMethod("getRuntimeMXBean").invoke(null);
			Class<?> mx = ReflectionUtils.tryToLoadClass("java.lang.management.RuntimeMXBean").getNonNull();
			@SuppressWarnings("unchecked")
			List<String> args = (List<String>) mx.getMethod("getInputArguments").invoke(bean);
			return Optional.of(args);
		}
		catch (Exception e) {
			return Optional.empty();
		}
	}

}
