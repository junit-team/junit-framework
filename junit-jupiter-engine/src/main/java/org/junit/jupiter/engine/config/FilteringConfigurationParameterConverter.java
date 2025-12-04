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

import static java.util.function.Predicate.not;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.platform.engine.ConfigurationParameters;

/**
 * @since 6.0
 */
class FilteringConfigurationParameterConverter<T> implements ConfigurationParameterConverter<T> {

	private final Predicate<? super String> predicate;
	private final ConfigurationParameterConverter<T> delegate;

	static <T> FilteringConfigurationParameterConverter<T> exclude(Predicate<? super String> exclusion,
			ConfigurationParameterConverter<T> delegate) {
		return new FilteringConfigurationParameterConverter<>(not(exclusion), delegate);
	}

	private FilteringConfigurationParameterConverter(Predicate<? super String> predicate,
			ConfigurationParameterConverter<T> delegate) {
		this.predicate = predicate;
		this.delegate = delegate;
	}

	@Override
	public Optional<T> get(ConfigurationParameters configurationParameters, String key) {
		return configurationParameters.get(key) //
				.map(String::strip) //
				.filter(predicate) //
				.flatMap(__ -> delegate.get(configurationParameters, key));
	}

}
