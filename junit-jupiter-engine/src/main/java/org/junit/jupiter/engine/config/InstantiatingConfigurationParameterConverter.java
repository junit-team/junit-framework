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
import java.util.function.Supplier;

import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * @since 5.5
 */
class InstantiatingConfigurationParameterConverter<T> implements ConfigurationParameterConverter<T> {

	private static final Logger logger = LoggerFactory.getLogger(InstantiatingConfigurationParameterConverter.class);

	private final Class<T> clazz;
	private final String name;

	InstantiatingConfigurationParameterConverter(Class<T> clazz, String name) {
		this.clazz = clazz;
		this.name = name;
	}

	@Override
	public Optional<T> get(ConfigurationParameters configurationParameters, String key) {
		return supply(configurationParameters, key).get();
	}

	Supplier<Optional<T>> supply(ConfigurationParameters configurationParameters, String key) {
		// @formatter:off
		return configurationParameters.get(key)
				.map(String::strip)
				.filter(className -> !className.isEmpty())
				.map(className -> newInstanceSupplier(className, key))
				.orElse(Optional::empty);
		// @formatter:on
	}

	private Supplier<Optional<T>> newInstanceSupplier(String className, String key) {
		Try<Class<?>> clazz = ReflectionSupport.tryToLoadClass(className);
		// @formatter:off
		return () -> clazz.andThenTry(ReflectionSupport::newInstance)
				.andThenTry(this.clazz::cast)
				.ifSuccess(generator -> logSuccessMessage(className, key))
				.ifFailure(cause -> logFailureMessage(className, key, cause))
				.toOptional();
		// @formatter:on
	}

	private void logFailureMessage(String className, String key, Exception cause) {
		logger.warn(cause, () -> """
				Failed to load default %s class '%s' set via the '%s' configuration parameter. \
				Falling back to default behavior.""".formatted(this.name, className, key));
	}

	private void logSuccessMessage(String className, String key) {
		logger.config(() -> "Using default %s '%s' set via the '%s' configuration parameter.".formatted(this.name,
			className, key));
	}

}
