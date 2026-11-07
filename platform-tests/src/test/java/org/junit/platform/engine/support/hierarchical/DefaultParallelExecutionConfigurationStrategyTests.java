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

import static java.math.MathContext.DECIMAL128;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * @since 1.3
 */
class DefaultParallelExecutionConfigurationStrategyTests {

	// Ensures we can calculate the factor for the desired dynamic parallelism
	// for any number of cpu values.
	static final MathContext DECIMAL128_ROUND_UP = new MathContext(DECIMAL128.getPrecision(), RoundingMode.UP);

	final ConfigurationParameters configParams = mock();

	@BeforeEach
	void setUp() {
		when(configParams.get(any(), any())).thenCallRealMethod();
	}

	@Test
	void dynamicStrategyThrowsExceptionWhenMaxPoolSizeFactorIsLessThanOne() {
		when(configParams.get("dynamic.max-pool-size-factor")).thenReturn(Optional.of("0.5"));

		var strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var exception = assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));

		assertThat(exception).hasRootCauseMessage("max pool size factor must be greater than or equal to one");
	}

	@Test
	void fixedStrategyCreatesValidConfiguration() {
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.of("42"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		var configuration = strategy.createConfiguration(configParams);

		assertThat(configuration.getParallelism()).isEqualTo(42);
		assertThat(configuration.getCorePoolSize()).isEqualTo(42);
		assertThat(configuration.getMinimumRunnable()).isEqualTo(42);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(256 + 42);
		assertThat(configuration.getKeepAliveSeconds()).isEqualTo(30);
		assertThat(configuration.getSaturatePredicate()).isNotNull();
		assertThat(configuration.getSaturatePredicate().test(null)).isTrue();
	}

	@Test
	void fixedSaturateStrategyCreatesValidConfiguration() {
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.of("42"));
		when(configParams.get("fixed.max-pool-size")).thenReturn(Optional.of("42"));
		when(configParams.get("fixed.saturate")).thenReturn(Optional.of("false"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		var configuration = strategy.createConfiguration(configParams);
		assertThat(configuration.getParallelism()).isEqualTo(42);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(42);
		assertThat(configuration.getSaturatePredicate()).isNotNull();
		assertThat(configuration.getSaturatePredicate().test(null)).isFalse();
	}

	@Test
	void dynamicStrategyCreatesValidConfiguration() {
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of("2.0"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var configuration = strategy.createConfiguration(configParams);

		var availableProcessors = Runtime.getRuntime().availableProcessors();
		assertThat(configuration.getParallelism()).isEqualTo(availableProcessors * 2);
		assertThat(configuration.getCorePoolSize()).isEqualTo(availableProcessors * 2);
		assertThat(configuration.getMinimumRunnable()).isEqualTo(availableProcessors * 2);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(256 + (availableProcessors * 2));
		assertThat(configuration.getKeepAliveSeconds()).isEqualTo(30);
		assertThat(configuration.getSaturatePredicate()).isNotNull();
		assertThat(configuration.getSaturatePredicate().test(null)).isTrue();
	}

	@Test
	void dynamicSaturateStrategyCreatesValidConfiguration() {
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of("2.0"));
		when(configParams.get("dynamic.max-pool-size-factor")).thenReturn(Optional.of("3.0"));
		when(configParams.get("dynamic.saturate")).thenReturn(Optional.of("false"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var configuration = strategy.createConfiguration(configParams);

		var availableProcessors = Runtime.getRuntime().availableProcessors();
		assertThat(configuration.getParallelism()).isEqualTo(availableProcessors * 2);
		assertThat(configuration.getCorePoolSize()).isEqualTo(availableProcessors * 2);
		assertThat(configuration.getMinimumRunnable()).isEqualTo(availableProcessors * 2);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(availableProcessors * 6);
		assertThat(configuration.getKeepAliveSeconds()).isEqualTo(30);
		assertThat(configuration.getSaturatePredicate()).isNotNull();
		assertThat(configuration.getSaturatePredicate().test(null)).isFalse();
	}

	@Test
	void customStrategyCreatesValidConfiguration() {
		when(configParams.get("custom.class")).thenReturn(
			Optional.of(CustomParallelExecutionConfigurationStrategy.class.getName()));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.CUSTOM;
		var configuration = strategy.createConfiguration(configParams);

		assertThat(configuration.getParallelism()).isEqualTo(1);
		assertThat(configuration.getCorePoolSize()).isEqualTo(4);
		assertThat(configuration.getMinimumRunnable()).isEqualTo(2);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(3);
		assertThat(configuration.getKeepAliveSeconds()).isEqualTo(5);
		assertThat(configuration.getSaturatePredicate()).isNotNull();
		assertThat(configuration.getSaturatePredicate().test(null)).isTrue();
	}

	@ParameterizedTest
	@EnumSource
	void createsStrategyFromConfigParam(DefaultParallelExecutionConfigurationStrategy strategy) {
		when(configParams.get("strategy")).thenReturn(Optional.of(strategy.name().toLowerCase()));

		assertThat(DefaultParallelExecutionConfigurationStrategy.getStrategy(configParams)).isSameAs(strategy);
	}

	@Test
	void fixedStrategyThrowsExceptionWhenParallelismIsNotPresent() {
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.empty());

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		var exception = assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));

		assertThat(exception).hasMessage("Configuration parameter 'fixed.parallelism' must be set");
	}

	@Test
	void fixedStrategyThrowsExceptionWhenParallelismIsNotAnInteger() {
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.of("foo"));

		var strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));
	}

	@Test
	void fixedStrategyThrowsExceptionWhenParallelismIsNotPositive() {
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.of("0"));

		var strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		var exception = assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));

		assertThat(exception).hasRootCauseMessage("parallelism must be positive");
	}

	@Test
	void fixedStrategyThrowsExceptionWhenThreadPoolIsNotPositive() {
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.of("2"));
		when(configParams.get("fixed.max-pool-size")).thenReturn(Optional.of("0"));

		var strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		var exception = assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));

		assertThat(exception).hasRootCauseMessage("max pool size must be positive");
	}

	@Test
	void fixedStrategyThrowsExceptionWhenThreadPoolIsSmallerThanParallelism() {
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.of("2"));
		when(configParams.get("fixed.max-pool-size")).thenReturn(Optional.of("1"));

		var strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		var exception = assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));

		assertThat(exception).hasRootCauseMessage("max pool size must be greater than or equal to the parallelism");
	}

	@Test
	void fixedStrategyLimitsDefaultMaxPoolSizeToPositiveInteger() {
		int desiredParallelism = Integer.MAX_VALUE - 100;
		when(configParams.get("fixed.parallelism")).thenReturn(Optional.of(String.valueOf(desiredParallelism)));

		var strategy = DefaultParallelExecutionConfigurationStrategy.FIXED;
		var configuration = strategy.createConfiguration(configParams);

		assertThat(configuration.getParallelism()).isEqualTo(desiredParallelism);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void dynamicStrategyUsesDefaultWhenPropertyIsNotPresent() {
		when(configParams.get("dynamic.factor")).thenReturn(Optional.empty());

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var configuration = strategy.createConfiguration(configParams);

		var availableProcessors = Runtime.getRuntime().availableProcessors();
		assertThat(configuration.getParallelism()).isEqualTo(availableProcessors);
		assertThat(configuration.getCorePoolSize()).isEqualTo(availableProcessors);
		assertThat(configuration.getMinimumRunnable()).isEqualTo(availableProcessors);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(256 + availableProcessors);
		assertThat(configuration.getKeepAliveSeconds()).isEqualTo(30);
	}

	@Test
	void dynamicStrategyThrowsExceptionWhenPropertyIsNotAnInteger() {
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of("foo"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));
	}

	@Test
	void dynamicStrategyThrowsExceptionWhenFactorIsZero() {
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of("0"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));
	}

	@Test
	void dynamicStrategyThrowsExceptionWhenFactorIsNegative() {
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of("-1"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));
	}

	@Test
	void dynamicStrategyUsesAtLeastParallelismOfOneWhenPropertyIsTooSmall() {
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of("0.00000000001"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var configuration = strategy.createConfiguration(configParams);

		assertThat(configuration.getParallelism()).isEqualTo(1);
		assertThat(configuration.getCorePoolSize()).isEqualTo(1);
		assertThat(configuration.getMinimumRunnable()).isEqualTo(1);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(256 + 1);
		assertThat(configuration.getKeepAliveSeconds()).isEqualTo(30);
	}

	@Test
	void dynamicStrategyLimitParallelismToPositiveInteger() {
		long desiredParallelism = 2L * Integer.MAX_VALUE;
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		var factor = BigDecimal.valueOf(desiredParallelism) //
				.divide(BigDecimal.valueOf(availableProcessors), DECIMAL128_ROUND_UP);
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of(String.valueOf(factor)));

		var strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var configuration = strategy.createConfiguration(configParams);

		assertThat(configuration.getParallelism()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void dynamicStrategyLimitMaxPoolSizeToPositiveInteger() {
		long desiredMaxCorePoolSize = 2L * Integer.MAX_VALUE;
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		var maxPoolSizeFactor = BigDecimal.valueOf(desiredMaxCorePoolSize) //
				.divide(BigDecimal.valueOf(availableProcessors), DECIMAL128_ROUND_UP);
		when(configParams.get("dynamic.max-pool-size-factor")).thenReturn(
			Optional.of(String.valueOf(maxPoolSizeFactor)));

		var strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var configuration = strategy.createConfiguration(configParams);

		assertThat(configuration.getParallelism()).isEqualTo(availableProcessors);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void dynamicStrategyLimitsDefaultMaxPoolSizeToPositiveInteger() {
		int desiredParallelism = Integer.MAX_VALUE - 100;
		var availableProcessors = BigDecimal.valueOf(Runtime.getRuntime().availableProcessors());
		var factor = BigDecimal.valueOf(desiredParallelism) //
				.divide(availableProcessors, DECIMAL128_ROUND_UP);
		when(configParams.get("dynamic.factor")).thenReturn(Optional.of(String.valueOf(factor)));

		var strategy = DefaultParallelExecutionConfigurationStrategy.DYNAMIC;
		var configuration = strategy.createConfiguration(configParams);

		assertThat(configuration.getParallelism()).isEqualTo(desiredParallelism);
		assertThat(configuration.getMaxPoolSize()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void customStrategyThrowsExceptionWhenPropertyIsNotPresent() {
		when(configParams.get("custom.class")).thenReturn(Optional.empty());

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.CUSTOM;
		assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));
	}

	@Test
	void customStrategyThrowsExceptionWhenClassDoesNotExist() {
		when(configParams.get("custom.class")).thenReturn(Optional.of("com.acme.ClassDoesNotExist"));

		ParallelExecutionConfigurationStrategy strategy = DefaultParallelExecutionConfigurationStrategy.CUSTOM;
		assertThrows(JUnitException.class, () -> strategy.createConfiguration(configParams));
	}

	static class CustomParallelExecutionConfigurationStrategy implements ParallelExecutionConfigurationStrategy {
		@Override
		public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters) {
			return new DefaultParallelExecutionConfiguration(1, 2, 3, 4, 5, __ -> true);
		}
	}

}
