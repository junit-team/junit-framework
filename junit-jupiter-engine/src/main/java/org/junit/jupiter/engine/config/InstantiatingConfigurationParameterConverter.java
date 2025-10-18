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

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * @since 5.5
 */
final class InstantiatingConfigurationParameterConverter<T> implements ConfigurationParameterConverter<T> {

	private static final Logger logger = LoggerFactory.getLogger(InstantiatingConfigurationParameterConverter.class);

	private final Class<T> clazz;
	private final String name;
	private final Strictness strictness;
	private final Function<ConfigurationParameters, Object[]> argumentResolver;

	InstantiatingConfigurationParameterConverter(Class<T> clazz, String name) {
		this(clazz, name, Strictness.WARN, __ -> new Object[0]);
	}

	InstantiatingConfigurationParameterConverter(Class<T> clazz, String name, Strictness strictness,
			Function<ConfigurationParameters, Object[]> argumentResolver) {
		this.clazz = clazz;
		this.name = name;
		this.strictness = strictness;
		this.argumentResolver = argumentResolver;
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
				.map(className -> newInstanceSupplier(className, key, configurationParameters))
				.orElse(Optional::empty);
		// @formatter:on
	}

	private Supplier<Optional<T>> newInstanceSupplier(String className, String key,
			ConfigurationParameters configurationParameters) {
		Try<Class<?>> clazz = ReflectionSupport.tryToLoadClass(className);
		// @formatter:off
		return () -> clazz.andThenTry(it -> instantiate(it, configurationParameters))
				.andThenTry(this.clazz::cast)
				.ifSuccess(generator -> logSuccessMessage(className, key))
				.ifFailure(cause -> logFailureMessage(className, key, cause))
				.toOptional();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private <V> V instantiate(Class<V> clazz, ConfigurationParameters configurationParameters) {
		var arguments = argumentResolver.apply(configurationParameters);
		if (arguments.length == 0) {
			return ReflectionSupport.newInstance(clazz);
		}
		var constructors = ReflectionUtils.findConstructors(clazz, it -> {
			if (it.getParameterCount() != arguments.length) {
				return false;
			}
			var parameters = it.getParameters();
			return IntStream.range(0, parameters.length) //
					.allMatch(i -> parameters[i].getType().isAssignableFrom(arguments[i].getClass()));
		});
		Preconditions.condition(constructors.size() == 1,
			() -> "Failed to find unambiguous constructor for %s. Candidates: %s".formatted(clazz.getName(),
				constructors));
		return ReflectionUtils.newInstance((Constructor<V>) constructors.get(0), arguments);
	}

	private void logFailureMessage(String className, String key, Exception cause) {
		switch (strictness) {
			case WARN -> logger.warn(cause, () -> """
					Failed to load default %s class '%s' set via the '%s' configuration parameter. \
					Falling back to default behavior.""".formatted(this.name, className, key));
			case FAIL -> throw new JUnitException(
				"Failed to load default %s class '%s' set via the '%s' configuration parameter.".formatted(this.name,
					className, key),
				cause);
		}
	}

	private void logSuccessMessage(String className, String key) {
		logger.config(() -> "Using default %s '%s' set via the '%s' configuration parameter.".formatted(this.name,
			className, key));
	}

	enum Strictness {
		FAIL, WARN
	}

}
