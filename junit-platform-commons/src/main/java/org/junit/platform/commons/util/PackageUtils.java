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

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apiguardian.api.API;

/**
 * Collection of utilities for working with {@linkplain Package packages}.
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
public final class PackageUtils {

	private PackageUtils() {
		/* no-op */
	}

	public static final String DEFAULT_PACKAGE_NAME = "";

	/**
	 * Get the package attribute for the supplied {@code type} using the
	 * supplied {@code function}.
	 *
	 * <p>This method only returns a non-empty {@link Optional} value holder
	 * if the class loader for the supplied type created a {@link Package}
	 * object and the supplied function does not return {@code null} when
	 * applied.
	 *
	 * @param type the type to get the package attribute for
	 * @param function a function that computes the package attribute value
	 * (e.g., {@code Package::getImplementationTitle}); never {@code null}
	 * @return an {@code Optional} containing the attribute value; never
	 * {@code null} but potentially empty
	 * @throws org.junit.platform.commons.PreconditionViolationException if the
	 * supplied type or function is {@code null}
	 * @see Class#getPackage()
	 * @see Package#getImplementationTitle()
	 * @see Package#getImplementationVersion()
	 */
	public static Optional<String> getAttribute(Class<?> type, Function<Package, String> function) {
		Preconditions.notNull(type, "type must not be null");
		Preconditions.notNull(function, "function must not be null");
		return Optional.ofNullable(type.getPackage()).map(function);
	}

	/**
	 * Get the value of the specified attribute name, specified as a string,
	 * or an empty {@link Optional} if the attribute was not found. The attribute
	 * name is case-insensitive.
	 *
	 * <p>This method also returns an empty {@link Optional} value holder
	 * if any exception is caught while loading the manifest file via the
	 * JAR file of the specified type.
	 *
	 * @param type the type to get the attribute for
	 * @param name the attribute name as a string
	 * @return an {@code Optional} containing the attribute value; never
	 * {@code null} but potentially empty
	 * @throws org.junit.platform.commons.PreconditionViolationException if the
	 * supplied type is {@code null} or the specified name is blank
	 * @see Manifest#getMainAttributes()
	 */
	public static Optional<String> getAttribute(Class<?> type, String name) {
		Preconditions.notNull(type, "type must not be null");
		Preconditions.notBlank(name, "name must not be blank");
		try {
			URL jarUrl = type.getProtectionDomain().getCodeSource().getLocation();
			try (JarFile jarFile = new JarFile(new File(jarUrl.toURI()))) {
				Attributes mainAttributes = jarFile.getManifest().getMainAttributes();
				return Optional.ofNullable(mainAttributes.getValue(name));
			}
		}
		catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Get the module or implementation version for the supplied {@code type}.
	 *
	 * <p>The former is only available if the type is part of a versioned module
	 * on the module path; the latter only if the type is part of a JAR file with
	 * a manifest that contains an {@code Implementation-Version} attribute.
	 *
	 * @since 1.11
	 */
	@API(status = INTERNAL, since = "1.11")
	public static Optional<String> getModuleOrImplementationVersion(Class<?> type) {
		Optional<String> moduleVersion = ModuleUtils.getModuleVersion(type);
		if (moduleVersion.isPresent()) {
			return moduleVersion;
		}
		return getAttribute(type, Package::getImplementationVersion);
	}

}
