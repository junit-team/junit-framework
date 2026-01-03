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

import org.jspecify.annotations.Nullable;

final class JupiterPropertyUtils {

	/**
	 * Create a effective clone of a {@link Properties} object, including
	 * defaults.
	 *
	 * <p>The clone will have the same observable entries and
	 * defaults as the {@code original} but may not use the same nested
	 * structure as the original.
	 *
	 * @return a new {@code Properties} instance containing the same
	 * 	observable entries as the original.
	 */
	static Properties createEffectiveClone(Properties original) {
		Properties clone = new Properties(createEffectiveCloneOfDefaults(original));
		original.keySet().forEach(key -> clone.put(key, original.get(key)));
		return clone;
	}

	private static @Nullable Properties createEffectiveCloneOfDefaults(Properties original) {
		// Backup the direct entries of the properties object and remove them
		var backup = new HashSet<>(original.entrySet());
		original.clear();

		// With the defaults now exposed, clone them.
		Properties defaultsClone = null;
		var propertyNames = original.stringPropertyNames();
		if (!propertyNames.isEmpty()) {
			defaultsClone = new Properties();
			for (String defaultPropertyName : propertyNames) {
				defaultsClone.setProperty(defaultPropertyName, original.getProperty(defaultPropertyName));
			}
		}

		// Finally restore the backup
		backup.forEach(entry -> original.put(entry.getKey(), entry.getValue()));
		return defaultsClone;
	}

	private JupiterPropertyUtils() {
		/* no-op */
	}
}
