/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.scanning.ClasspathScanner;

/**
 * @since 1.12
 */
class ClasspathScannerLoader {

	static ClasspathScanner getInstance() {
		ServiceLoader<ClasspathScanner> serviceLoader = ServiceLoader.load(ClasspathScanner.class,
			ClassLoaderUtils.getDefaultClassLoader());

		List<Provider<ClasspathScanner>> classpathScanners = serviceLoader.stream().toList();

		if (classpathScanners.size() == 1) {
			return classpathScanners.get(0).get();
		}

		if (classpathScanners.size() > 1) {
			throw new JUnitException(
				"There should not be more than one ClasspathScanner implementation present on the classpath but there were %d: %s".formatted(
					classpathScanners.size(),
					classpathScanners.stream().map(Provider::type).map(Class::getName).toList()));
		}

		return new DefaultClasspathScanner(ClassLoaderUtils::getDefaultClassLoader, ReflectionUtils::tryToLoadClass);
	}

	private ClasspathScannerLoader() {
	}

}
