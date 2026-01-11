/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.12
 */
class ParameterizedTestSpiInstantiator {

	static <T> T instantiate(Class<T> spiInterface, Class<? extends T> implementationClass,
			ExtensionContext extensionContext) {

		return extensionContext.getExecutableInvoker() //
				.invoke(findConstructor(spiInterface, implementationClass));
	}

	@SuppressWarnings("unchecked")
	private static <T> Constructor<? extends T> findConstructor(Class<T> spiInterface,
			Class<? extends T> implementationClass) {

		return (Constructor<? extends T>) findBestConstructor(spiInterface, implementationClass);
	}

	/**
	 * Find the "best" constructor for the supplied implementation class.
	 *
	 * <p>For backward compatibility, it first checks for a single constructor
	 * and returns that. If there are multiple constructors, it checks for a
	 * default constructor which takes precedence over any other constructors.
	 * Otherwise, this method throws an exception stating that it failed to
	 * find a suitable constructor.
	 */
	private static <T> Constructor<?> findBestConstructor(Class<T> spiInterface,
			Class<? extends T> implementationClass) {

		Preconditions.condition(!ReflectionUtils.isInnerClass(implementationClass),
			() -> "The %s [%s] must be either a top-level class or a static nested class".formatted(
				spiInterface.getSimpleName(), implementationClass.getName()));

		Constructor<?>[] constructors = implementationClass.getDeclaredConstructors();

		// Single constructor?
		if (constructors.length == 1) {
			return constructors[0];
		}
		// Find default constructor.
		for (Constructor<?> constructor : constructors) {
			if (constructor.getParameterCount() == 0) {
				return constructor;
			}
		}
		// Otherwise...
		String message = """
				Failed to find constructor for %s [%s]. \
				Please ensure that a no-argument or a single constructor exists.""".formatted(
			spiInterface.getSimpleName(), implementationClass.getName());
		throw new JUnitException(message);
	}

	private ParameterizedTestSpiInstantiator() {
	}

}
