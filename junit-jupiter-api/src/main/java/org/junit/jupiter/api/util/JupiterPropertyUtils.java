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

import java.util.Properties;

final class JupiterPropertyUtils {

	/**
	 * Create a effective clone of a {@link Properties} object.
	 *
	 * <p>The clone will have the same observable properties as the
	 * {@code original} but is constructed without default values. Furthermore
	 * any non-string values are ignored.
	 *
	 * @return a new instance containing the same observable properties as the original.
	 */
	static Properties createEffectiveClone(Properties original) {
		Properties clone = new Properties();
		// This implementation is used because
		// * Properties.defaults is not accessible
		// * Properties::clone does not include the defaults
		// * The defaults can be obtained by removing all keys from the
		//   original and checking the remaining values. This requires
		//   mutation.
		original.stringPropertyNames().forEach(
			propertyName -> clone.put(propertyName, original.getProperty(propertyName)));

		return clone;
	}
}
