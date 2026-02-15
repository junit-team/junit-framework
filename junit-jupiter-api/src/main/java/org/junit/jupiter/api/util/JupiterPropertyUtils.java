/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

final class JupiterPropertyUtils {

	private JupiterPropertyUtils() {
		/* no-op */
	}

	static Properties cloneWithoutDefaults(ExtensionContext context, Properties properties) {
		// Custom implementations have to implement clone correctly.
		if (properties.getClass() == Properties.class) {
			throwIfHasObservableDefaults(context, properties);
		}
		return (Properties) properties.clone();
	}

	private static void throwIfHasObservableDefaults(ExtensionContext context, Properties properties) {
		Set<Object> keySet = properties.keySet();
		// A best effort check.
		List<String> defaultPropertyNames = properties.stringPropertyNames().stream() //
				.filter(propertyName -> !keySet.contains(propertyName)) //
				.toList();
		if (!defaultPropertyNames.isEmpty()) {
			throw new ExtensionConfigurationException("""
					SystemPropertiesExtension was configured to restore the system properties by [%s]. \
					However, it was not possible to create an accurate snapshot of the system properties \
					using Properties::clone, because default properties were present: %s""" //
					.formatted(context.getElement().orElseThrow(), defaultPropertyNames));
		}
	}

}
