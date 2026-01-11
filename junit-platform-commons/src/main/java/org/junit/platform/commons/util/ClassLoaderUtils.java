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

import java.net.URL;
import java.security.CodeSource;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * Collection of utilities for working with {@linkplain ClassLoader} and associated tasks.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class ClassLoaderUtils {

	private ClassLoaderUtils() {
		/* no-op */
	}

	/**
	 * Get the {@link ClassLoader} for the supplied {@link Class}, falling back
	 * to the {@link #getDefaultClassLoader() default class loader} if the class
	 * loader for the supplied class is {@code null}.
	 * @param clazz the class for which to retrieve the class loader; never {@code null}
	 * @since 1.10
	 */
	public static ClassLoader getClassLoader(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		ClassLoader classLoader = clazz.getClassLoader();
		return (classLoader != null) ? classLoader : getDefaultClassLoader();
	}

	public static ClassLoader getDefaultClassLoader() {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if (contextClassLoader != null) {
				return contextClassLoader;
			}
		}
		catch (Throwable t) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(t);
			/* otherwise ignore */
		}
		return ClassLoader.getSystemClassLoader();
	}

	/**
	 * Get the location from which the supplied object's class was loaded.
	 *
	 * @param object the object for whose class the location should be retrieved
	 * @return an {@code Optional} containing the URL of the class' location; never
	 * {@code null} but potentially empty
	 */
	public static Optional<URL> getLocation(Object object) {
		Preconditions.notNull(object, "object must not be null");
		// determine class loader
		ClassLoader loader = object.getClass().getClassLoader();
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
			while (loader != null && loader.getParent() != null) {
				loader = loader.getParent();
			}
		}
		// try finding resource by name
		if (loader != null) {
			String name = object.getClass().getName();
			name = name.replace(".", "/") + ".class";
			try {
				return Optional.ofNullable(loader.getResource(name));
			}
			catch (Throwable t) {
				UnrecoverableExceptions.rethrowIfUnrecoverable(t);
				/* otherwise ignore */
			}
		}
		// try protection domain
		try {
			CodeSource codeSource = object.getClass().getProtectionDomain().getCodeSource();
			if (codeSource != null) {
				return Optional.ofNullable(codeSource.getLocation());
			}
		}
		catch (SecurityException ignore) {
			/* ignore */
		}
		return Optional.empty();
	}
}
