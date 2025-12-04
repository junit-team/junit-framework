/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import java.util.function.Predicate;

import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * Resource-related utilities to be used in conjunction with {@link ReflectionSupport}.
 *
 * @since 1.12
 */
class ResourceUtils {
	public static final String DEFAULT_PACKAGE_NAME = "";
	private static final char CLASSPATH_RESOURCE_PATH_SEPARATOR = '/';
	private static final char PACKAGE_SEPARATOR_CHAR = '.';

	/**
	 * Match resources against a package filter.
	 *
	 * <p>The {@code /} separated path of a resource is rewritten to a
	 * {@code .} separated package names. The package filter is applied to that
	 * package name.
	 */
	static Predicate<Resource> packageName(Predicate<String> packageFilter) {
		return resource -> packageFilter.test(packageName(resource.getName()));
	}

	private static String packageName(String classpathResourceName) {
		int lastIndexOf = classpathResourceName.lastIndexOf(CLASSPATH_RESOURCE_PATH_SEPARATOR);
		if (lastIndexOf < 0) {
			return DEFAULT_PACKAGE_NAME;
		}
		// classpath resource names do not start with /
		String resourcePackagePath = classpathResourceName.substring(0, lastIndexOf);
		return resourcePackagePath.replace(CLASSPATH_RESOURCE_PATH_SEPARATOR, PACKAGE_SEPARATOR_CHAR);
	}

	private ResourceUtils() {
	}
}
