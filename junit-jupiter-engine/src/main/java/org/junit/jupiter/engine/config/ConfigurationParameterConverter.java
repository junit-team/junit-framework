/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.config;

import java.util.Optional;

import org.junit.platform.engine.ConfigurationParameters;

/**
 * @since 6.0
 */
interface ConfigurationParameterConverter<T> {

	default T getOrDefault(ConfigurationParameters configParams, String key, T defaultValue) {
		return get(configParams, key).orElse(defaultValue);
	}

	Optional<T> get(ConfigurationParameters configurationParameters, String key);

}
