/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static java.util.Objects.requireNonNull;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.math.BigDecimal;
import java.util.Locale;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * Default implementations of configuration strategies for parallel test
 * execution.
 *
 * @since 1.3
 */
@API(status = STABLE, since = "1.10")
public enum DefaultParallelExecutionConfigurationStrategy implements ParallelExecutionConfigurationStrategy {

	/**
	 * Uses the mandatory {@value #CONFIG_FIXED_PARALLELISM_PROPERTY_NAME} configuration
	 * parameter as the desired parallelism.
	 */
	FIXED {
		@Override
		public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters) {
			int parallelism = configurationParameters.get(CONFIG_FIXED_PARALLELISM_PROPERTY_NAME,
				Integer::valueOf).orElseThrow(
					() -> new JUnitException(
						"Configuration parameter '%s' must be set".formatted(CONFIG_FIXED_PARALLELISM_PROPERTY_NAME)));

			int maxPoolSize = configurationParameters.get(CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME,
				Integer::valueOf).orElse(parallelism + 256);

			boolean saturate = configurationParameters.get(CONFIG_FIXED_SATURATE_PROPERTY_NAME,
				Boolean::valueOf).orElse(CONFIG_FIXED_SATURATE_DEFAULT);

			return new DefaultParallelExecutionConfiguration(parallelism, parallelism, maxPoolSize, parallelism,
				KEEP_ALIVE_SECONDS, __ -> saturate);
		}
	},

	/**
	 * Computes the desired parallelism based on the number of available
	 * processors/cores multiplied by the {@value #CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME}
	 * configuration parameter.
	 */
	DYNAMIC {
		@Override
		public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters) {
			BigDecimal factor = configurationParameters.get(CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME,
				BigDecimal::new).orElse(CONFIG_DYNAMIC_FACTOR_DEFAULT);

			Preconditions.condition(factor.compareTo(BigDecimal.ZERO) > 0,
				() -> "Factor '%s' specified via configuration parameter '%s' must be greater than 0".formatted(factor,
					CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME));

			int parallelism = Math.max(1,
				factor.multiply(BigDecimal.valueOf(Runtime.getRuntime().availableProcessors())).intValue());

			int maxPoolSize = configurationParameters.get(CONFIG_DYNAMIC_MAX_POOL_SIZE_FACTOR_PROPERTY_NAME,
				BigDecimal::new).map(maxPoolSizeFactor -> {
					Preconditions.condition(maxPoolSizeFactor.compareTo(BigDecimal.ONE) >= 0,
						() -> "Factor '%s' specified via configuration parameter '%s' must be greater than or equal to 1".formatted(
							factor, CONFIG_DYNAMIC_MAX_POOL_SIZE_FACTOR_PROPERTY_NAME));
					return maxPoolSizeFactor.multiply(BigDecimal.valueOf(parallelism)).intValue();
				}).orElseGet(() -> 256 + parallelism);

			boolean saturate = configurationParameters.get(CONFIG_DYNAMIC_SATURATE_PROPERTY_NAME,
				Boolean::valueOf).orElse(true);

			return new DefaultParallelExecutionConfiguration(parallelism, parallelism, maxPoolSize, parallelism,
				KEEP_ALIVE_SECONDS, __ -> saturate);
		}
	},

	/**
	 * Allows the specification of a custom {@link ParallelExecutionConfigurationStrategy}
	 * implementation via the mandatory {@value #CONFIG_CUSTOM_CLASS_PROPERTY_NAME}
	 * configuration parameter to determine the desired configuration.
	 */
	CUSTOM {
		@Override
		public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters) {
			String className = configurationParameters.get(CONFIG_CUSTOM_CLASS_PROPERTY_NAME).orElseThrow(
				() -> new JUnitException(CONFIG_CUSTOM_CLASS_PROPERTY_NAME + " must be set"));
			return ReflectionSupport.tryToLoadClass(className) //
					.andThenTry(strategyClass -> {
						Preconditions.condition(
							ParallelExecutionConfigurationStrategy.class.isAssignableFrom(strategyClass),
							CONFIG_CUSTOM_CLASS_PROPERTY_NAME + " does not implement "
									+ ParallelExecutionConfigurationStrategy.class);
						return (ParallelExecutionConfigurationStrategy) ReflectionSupport.newInstance(strategyClass);
					}) //
					.andThenTry(strategy -> requireNonNull(strategy).createConfiguration(configurationParameters)) //
					.getNonNullOrThrow(cause -> new JUnitException(
						"Could not create configuration for strategy class: " + className, cause));
		}
	};

	private static final int KEEP_ALIVE_SECONDS = 30;

	/**
	 * Default value for {@value #CONFIG_STRATEGY_PROPERTY_NAME} is {@value}.
	 */
	@API(status = MAINTAINED, since = "6.2.0")
	public static final String CONFIG_STRATEGY_DEFAULT = "dynamic";

	/**
	 * Property name used to determine the desired configuration strategy.
	 *
	 * <p>Value must be one of {@code dynamic}, {@code fixed}, or
	 * {@code custom}.
	 */
	public static final String CONFIG_STRATEGY_PROPERTY_NAME = "strategy";

	/**
	 * Property name used to determine the desired parallelism for the
	 * {@link #FIXED} configuration strategy.
	 *
	 * <p>No default value; must be an integer.
	 *
	 * @see #FIXED
	 */
	public static final String CONFIG_FIXED_PARALLELISM_PROPERTY_NAME = "fixed.parallelism";

	/**
	 * Property name used to configure the maximum pool size of the underlying
	 * fork-join pool for the {@link #FIXED} configuration strategy.
	 *
	 * <p>Value must be an integer and greater than or equal to
	 * {@value #CONFIG_FIXED_PARALLELISM_PROPERTY_NAME}; defaults to
	 * {@code 256 + fixed.parallelism}.
	 *
	 * @since 1.10
	 * @see #FIXED
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME = "fixed.max-pool-size";

	/**
	 * TODO:
	 */
	@API(status = MAINTAINED, since = "6.2.0")
	public static final boolean CONFIG_FIXED_SATURATE_DEFAULT = true;

	/**
	 * Property name used to disable saturation of the underlying fork-join pool
	 * for the {@link #FIXED} configuration strategy.
	 *
	 * <p>When set to {@code false} the underlying fork-join pool will reject
	 * additional tasks if all available workers are busy and the maximum
	 * pool-size would be exceeded.
	 * <p>Value must either {@code true} or {@code false}; defaults to {@code true}.
	 *
	 * @since 1.10
	 * @see #FIXED
	 * @see #CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String CONFIG_FIXED_SATURATE_PROPERTY_NAME = "fixed.saturate";

	/**
	 * TODO:
	 */
	@API(status = MAINTAINED, since = "6.2.0")
	public static final BigDecimal CONFIG_DYNAMIC_FACTOR_DEFAULT = BigDecimal.ONE;

	/**
	 * Property name of the factor used to determine the desired parallelism for the
	 * {@link #DYNAMIC} configuration strategy.
	 *
	 * <p>Value must be a non-negative decimal number; defaults to {@code 1}.
	 *
	 * @see #DYNAMIC
	 */
	public static final String CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME = "dynamic.factor";

	/**
	 * Property name of the factor used to determine the maximum pool size of
	 * the underlying fork-join pool for the {@link #DYNAMIC} configuration
	 * strategy.
	 *
	 * <p>Value must be a decimal number equal and greater than or equal to
	 * {@code 1}. When set the maximum pool size is calculated as
	 * {@code dynamic.max-pool-size-factor * dynamic.factor * Runtime.getRuntime().availableProcessors()}
	 * When not set the maximum pool size is calculated as
	 * {@code 256 + dynamic.factor * Runtime.getRuntime().availableProcessors()}
	 * instead.
	 *
	 * @since 1.10
	 * @see #DYNAMIC
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String CONFIG_DYNAMIC_MAX_POOL_SIZE_FACTOR_PROPERTY_NAME = "dynamic.max-pool-size-factor";

	/**
	 * Property name used to disable saturation of the underlying fork-join pool
	 * for the {@link #DYNAMIC} configuration strategy.
	 *
	 * <p>When set to {@code false} the underlying fork-join pool will reject
	 * additional tasks if all available workers are busy and the maximum
	 * pool-size would be exceeded.
	 * <p>Value must either {@code true} or {@code false}; defaults to {@code true}.
	 *
	 * @since 1.10
	 * @see #DYNAMIC
	 * @see #CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String CONFIG_DYNAMIC_SATURATE_PROPERTY_NAME = "dynamic.saturate";

	/**
	 * Property name used to specify the fully qualified class name of the
	 * {@link ParallelExecutionConfigurationStrategy} to be used by the
	 * {@link #CUSTOM} configuration strategy.
	 *
	 * @see #CUSTOM
	 */
	public static final String CONFIG_CUSTOM_CLASS_PROPERTY_NAME = "custom.class";

	static ParallelExecutionConfiguration toConfiguration(ConfigurationParameters configurationParameters) {
		return getStrategy(configurationParameters).createConfiguration(configurationParameters);
	}

	static ParallelExecutionConfigurationStrategy getStrategy(ConfigurationParameters configurationParameters) {
		return configurationParameters.get(CONFIG_STRATEGY_PROPERTY_NAME, value -> {
			try {
				return valueOf(value.toUpperCase(Locale.ROOT));
			}
			catch (Exception e) {
				throw new JUnitException(
					"Invalid ParallelExecutionConfigurationStrategy '%s' set via the '%s' configuration parameter.".formatted(
						value, CONFIG_STRATEGY_PROPERTY_NAME));
			}
		}).orElseGet(DefaultParallelExecutionConfigurationStrategy::getDefaultParallelExecutionConfigurationStrategy);
	}

	private static DefaultParallelExecutionConfigurationStrategy getDefaultParallelExecutionConfigurationStrategy() {
		return valueOf(CONFIG_STRATEGY_DEFAULT.toUpperCase(Locale.ROOT));
	}

}
