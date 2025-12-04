/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;

/**
 * Configuration parameters that {@link TestEngine TestEngines} may use to
 * influence test discovery and execution.
 *
 * <p>For example, the JUnit Jupiter engine uses a configuration parameter to
 * enable IDEs and build tools to deactivate conditional test execution.
 *
 * <p>Configuration parameters are also made available to implementations of the
 * {@link org.junit.platform.launcher.TestExecutionListener} API via the
 * {@link org.junit.platform.launcher.TestPlan}.
 *
 * @since 1.0
 * @see TestEngine
 * @see EngineDiscoveryRequest
 * @see ExecutionRequest
 */
@API(status = STABLE, since = "1.0")
public interface ConfigurationParameters {

	/**
	 * Name of the JUnit Platform configuration file: {@value}.
	 *
	 * <p>If a properties file with this name is present in the root of the
	 * classpath, it will be used as a source for <em>configuration
	 * parameters</em>. If multiple files are present, only the first one
	 * detected in the classpath will be used.
	 *
	 * @see java.util.Properties
	 */
	String CONFIG_FILE_NAME = "junit-platform.properties";

	/**
	 * Get the configuration parameter stored under the specified {@code key}.
	 *
	 * <p>If no such key is present in this {@code ConfigurationParameters},
	 * an attempt will be made to look up the value as a JVM system property.
	 * If no such system property exists, an attempt will be made to look up
	 * the value in the {@linkplain #CONFIG_FILE_NAME JUnit Platform properties
	 * file}.
	 *
	 * @param key the key to look up; never {@code null} or blank
	 * @return an {@code Optional} containing the value; never {@code null}
	 * but potentially empty
	 *
	 * @see #getBoolean(String)
	 * @see System#getProperty(String)
	 * @see #CONFIG_FILE_NAME
	 */
	Optional<String> get(String key);

	/**
	 * Get the boolean configuration parameter stored under the specified
	 * {@code key}.
	 *
	 * <p>If no such key is present in this {@code ConfigurationParameters},
	 * an attempt will be made to look up the value as a JVM system property.
	 * If no such system property exists, an attempt will be made to look up
	 * the value in the {@linkplain #CONFIG_FILE_NAME JUnit Platform properties
	 * file}.
	 *
	 * @param key the key to look up; never {@code null} or blank
	 * @return an {@code Optional} containing the value; never {@code null}
	 * but potentially empty
	 *
	 * @see #get(String)
	 * @see Boolean#parseBoolean(String)
	 * @see System#getProperty(String)
	 * @see #CONFIG_FILE_NAME
	 */
	Optional<Boolean> getBoolean(String key);

	/**
	 * Get and transform the configuration parameter stored under the specified
	 * {@code key} using the specified {@code transformer}.
	 *
	 * <p>If no such key is present in this {@code ConfigurationParameters},
	 * an attempt will be made to look up the value as a JVM system property.
	 * If no such system property exists, an attempt will be made to look up
	 * the value in the {@linkplain #CONFIG_FILE_NAME JUnit Platform properties
	 * file}.
	 *
	 * <p>In case the transformer throws an exception, it will be wrapped in a
	 * {@link JUnitException} with a helpful message.
	 *
	 * @param key the key to look up; never {@code null} or blank
	 * @param transformer the transformer to apply in case a value is found;
	 * never {@code null}
	 * @return an {@code Optional} containing the value; never {@code null}
	 * but potentially empty
	 *
	 * @since 1.3
	 * @see #getBoolean(String)
	 * @see System#getProperty(String)
	 * @see #CONFIG_FILE_NAME
	 */
	@API(status = STABLE, since = "1.3")
	default <T> Optional<T> get(String key, Function<? super String, ? extends @Nullable T> transformer) {
		Preconditions.notNull(transformer, "transformer must not be null");
		return get(key).map(input -> {
			try {
				return transformer.apply(input);
			}
			catch (Exception ex) {
				String message = "Failed to transform configuration parameter with key '%s' and initial value '%s'".formatted(
					key, input);
				throw new JUnitException(message, ex);
			}
		});
	}

	/**
	 * Get the keys of all configuration parameters stored in this
	 * {@code ConfigurationParameters}.
	 *
	 * @return the set of keys contained in this {@code ConfigurationParameters}
	 */
	@API(status = STABLE, since = "1.9")
	Set<String> keySet();

}
