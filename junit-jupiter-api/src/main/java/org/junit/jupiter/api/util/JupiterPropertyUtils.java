/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jspecify.annotations.Nullable;

final class JupiterPropertyUtils {

	/**
	 * Create a effective clone of a {@link Properties} object, including
	 * defaults.
	 *
	 * <p>The clone will initially have the same observable entries and
	 * defaults as the {@code original} but may not use the same nested
	 * structure as the original.
	 *
	 * <p>As a consequence, removing a key from the clone that was also presents the
	 * originals defaults will not result in a default value being used for subsequent
	 * looks up for that key.
	 *
	 * @return a new {@code Properties} instance containing the same
	 * 	observable entries as the original.
	 */
	static Properties createEffectiveClone(Properties original) {
		Properties clone = new Properties(createEffectivelyCloneOfDefaults(original));
		original.keySet().forEach(key -> clone.put(key, original.get(key)));
		return clone;
	}

	private static @Nullable Properties createEffectivelyCloneOfDefaults(Properties original) {
		Set<String> defaultPropertyNames = getEffectiveDefaultPropertyNames(original);
		if (defaultPropertyNames.isEmpty()) {
			return null;
		}
		Properties defaultsClone = new Properties();
		for (String defaultPropertyName : defaultPropertyNames) {
			defaultsClone.setProperty(defaultPropertyName, original.getProperty(defaultPropertyName));
		}
		return defaultsClone;
	}

	private static Set<String> getEffectiveDefaultPropertyNames(Properties original) {
		var allPropertyNames = original.stringPropertyNames();
		var defaultPropertyNames = new HashSet<>(allPropertyNames);
		// The property name came from properties (and not the defaults) if it
		// was  present in the properties as a string
		defaultPropertyNames.removeIf(propertyName -> original.get(propertyName) instanceof String);
		return defaultPropertyNames;
	}

	private JupiterPropertyUtils() {
		/* no-op */
	}
}
