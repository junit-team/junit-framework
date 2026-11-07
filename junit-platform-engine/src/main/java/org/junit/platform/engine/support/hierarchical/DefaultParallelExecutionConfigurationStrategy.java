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

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.math.BigDecimal;
import java.util.Locale;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.function.Try;
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
			int parallelism = configurationParameters //
					.get(CONFIG_FIXED_PARALLELISM_PROPERTY_NAME, value -> parseParallelism(value)) //
					.orElseThrow(() -> new JUnitException("Configuration parameter '%s' must be set" //
							.formatted(CONFIG_FIXED_PARALLELISM_PROPERTY_NAME)));

			int maxPoolSize = configurationParameters //
					.get(CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME, value -> parseMaxPoolSize(value, parallelism)) //
					.orElseGet(() -> defaultMaxPoolSize(parallelism));

			boolean saturate = configurationParameters //
					.get(CONFIG_FIXED_SATURATE_PROPERTY_NAME, Boolean::valueOf) //
					.orElse(CONFIG_FIXED_SATURATE_DEFAULT);

			return new DefaultParallelExecutionConfiguration(parallelism, parallelism, maxPoolSize, parallelism,
				KEEP_ALIVE_SECONDS, __ -> saturate);
		}

		private static int parseParallelism(String value) {
			var parallelism = Integer.parseInt(value);
			Preconditions.condition(parallelism >= 1, "parallelism must be positive");
			return parallelism;
		}

		private static Integer parseMaxPoolSize(String value, int parallelism) {
			var maxPoolSize = Integer.parseInt(value);
			Preconditions.condition(maxPoolSize >= 1, "max pool size must be positive");
			Preconditions.condition(maxPoolSize >= parallelism,
				"max pool size must be greater than or equal to the parallelism");
			return maxPoolSize;
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
			var factor = configurationParameters //
					.get(CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME, this::parseDynamicFactor) //
					.orElse(BigDecimal.valueOf(CONFIG_DYNAMIC_FACTOR_DEFAULT));

			int parallelism = calculateParallelism(factor);

			int maxPoolSize = configurationParameters //
					.get(CONFIG_DYNAMIC_MAX_POOL_SIZE_FACTOR_PROPERTY_NAME, this::parseDynamicMaxPoolSizeFactor) //
					.map(maxPoolSizeFactor -> calculateMaxPoolSize(maxPoolSizeFactor, parallelism)) //
					.orElseGet(() -> defaultMaxPoolSize(parallelism));

			boolean saturate = configurationParameters //
					.get(CONFIG_DYNAMIC_SATURATE_PROPERTY_NAME, Boolean::valueOf) //
					.orElse(true);

			return new DefaultParallelExecutionConfiguration(parallelism, parallelism, maxPoolSize, parallelism,
				KEEP_ALIVE_SECONDS, __ -> saturate);
		}

		private BigDecimal parseDynamicFactor(String value) {
			var dynamicFactor = new BigDecimal(value);
			Preconditions.condition(dynamicFactor.compareTo(BigDecimal.ZERO) > 0,
				"dynamic factor must be greater than zero");
			return dynamicFactor;
		}

		private static int calculateParallelism(BigDecimal factor) {
			var availableProcessors = BigDecimal.valueOf(Runtime.getRuntime().availableProcessors());
			return clampPositiveIntValue(factor.multiply(availableProcessors));
		}

		private BigDecimal parseDynamicMaxPoolSizeFactor(String value) {
			var maxPoolSizeFactor = new BigDecimal(value);
			Preconditions.condition(maxPoolSizeFactor.compareTo(BigDecimal.ONE) >= 0,
				"max pool size factor must be greater than or equal to one");
			return maxPoolSizeFactor;
		}

		private static int calculateMaxPoolSize(BigDecimal maxPoolSizeFactor, int parallelism) {
			var maxPoolSize = maxPoolSizeFactor.multiply(BigDecimal.valueOf(parallelism));
			return clampPositiveIntValue(maxPoolSize);
		}

		private static int clampPositiveIntValue(BigDecimal value) {
			return value.max(BigDecimal.ONE).min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
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
			return configurationParameters.get(CONFIG_CUSTOM_CLASS_PROPERTY_NAME,
				className -> loadConfigurationStrategy(className) //
						.andThenTry(strategy -> strategy.createConfiguration(configurationParameters)) //
						.getNonNullOrThrow(cause -> new JUnitException(
							"Could not create configuration for strategy class: " + className, cause)))//
					.orElseThrow(() -> new JUnitException(CONFIG_CUSTOM_CLASS_PROPERTY_NAME + " must be set"));

		}

		private Try<ParallelExecutionConfigurationStrategy> loadConfigurationStrategy(String className) {
			return ReflectionSupport.tryToLoadClass(className) //
					.andThenTry(strategyClass -> {
						Preconditions.condition(
							ParallelExecutionConfigurationStrategy.class.isAssignableFrom(strategyClass),
							CONFIG_CUSTOM_CLASS_PROPERTY_NAME + " does not implement "
									+ ParallelExecutionConfigurationStrategy.class);
						return (ParallelExecutionConfigurationStrategy) ReflectionSupport.newInstance(strategyClass);
					});
		}
	};

	private static final int KEEP_ALIVE_SECONDS = 30;

	/**
	 * Default value for {@value #CONFIG_STRATEGY_PROPERTY_NAME} is {@value}.
	 */
	@API(status = MAINTAINED, since = "6.2")
	public static final String CONFIG_STRATEGY_DEFAULT = "dynamic";

	/**
	 * Property name used to determine the desired configuration strategy.
	 *
	 * <p>Value must be one names of enum constants defined in
	 * {@linkplain DefaultParallelExecutionConfigurationStrategy this class},
	 * ignoring case. If not specified, the default is
	 * {@value CONFIG_STRATEGY_DEFAULT}.
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
	 * Default value for {@value #CONFIG_FIXED_SATURATE_PROPERTY_NAME} is {@value}.
	 */
	@API(status = MAINTAINED, since = "6.2")
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
	 * Default value for {@value #CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME} is {@value}.
	 */
	@API(status = MAINTAINED, since = "6.2")
	public static final double CONFIG_DYNAMIC_FACTOR_DEFAULT = 1.0;

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
		return configurationParameters.get(CONFIG_STRATEGY_PROPERTY_NAME,
			value -> valueOf(value.toUpperCase(Locale.ROOT))).orElseGet(
				DefaultParallelExecutionConfigurationStrategy::getDefaultParallelExecutionConfigurationStrategy);
	}

	private static DefaultParallelExecutionConfigurationStrategy getDefaultParallelExecutionConfigurationStrategy() {
		return valueOf(CONFIG_STRATEGY_DEFAULT.toUpperCase(Locale.ROOT));
	}

	private static int defaultMaxPoolSize(int parallelism) {
		return parallelism > Integer.MAX_VALUE - 256 ? Integer.MAX_VALUE : parallelism + 256;
	}
}
