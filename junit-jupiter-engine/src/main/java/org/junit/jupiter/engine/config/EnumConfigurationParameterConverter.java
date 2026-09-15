/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.config;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Locale;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * @since 5.4
 */
@API(status = INTERNAL, since = "5.8")
public class EnumConfigurationParameterConverter<E extends Enum<E>> implements ConfigurationParameterConverter<E> {

	private static final Logger logger = LoggerFactory.getLogger(EnumConfigurationParameterConverter.class);

	private final Class<E> enumType;
	private final String enumDisplayName;

	public EnumConfigurationParameterConverter(Class<E> enumType, String enumDisplayName) {
		this.enumType = enumType;
		this.enumDisplayName = enumDisplayName;
	}

	@Override
	public Optional<E> get(ConfigurationParameters configParams, String key) {
		var result = configParams.get(key).map(value -> convert(key, value));
		result.ifPresent(value -> logUsageMessage(key, value));
		return result;
	}

	public Optional<E> get(ExtensionContext extensionContext, String key) {
		var result = extensionContext.getConfigurationParameter(key, value -> convert(key, value));
		result.ifPresent(value -> logUsageMessage(key, value));
		return result;
	}

	public E getOrDefault(ConfigurationParameters configParams, String key, String defaultValue) {
		return get(configParams, key).orElseGet(() -> convert(key, defaultValue));
	}

	private void logUsageMessage(String key, E result) {
		logger.config(() -> "Using %s '%s' set via the '%s' configuration parameter." //
				.formatted(enumDisplayName, result, key));
	}

	private E convert(String key, String value) {
		String constantName = value.strip().toUpperCase(Locale.ROOT);
		try {
			return Enum.valueOf(enumType, constantName);
		}
		catch (Exception ex) {
			throw new JUnitException("Invalid %s '%s' set via the '%s' configuration parameter.".formatted(
				enumDisplayName, constantName, key));
		}
	}

}
