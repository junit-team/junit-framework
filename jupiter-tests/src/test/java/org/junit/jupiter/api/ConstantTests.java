/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.platform.commons.util.ClassNamePatternFilterUtils;
import org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy;
import org.junit.platform.engine.support.hierarchical.ParallelHierarchicalTestExecutorServiceFactory;

public class ConstantTests {

	@Test
	void constantsAreConsistent() {
		assertThat(Constants.PARALLEL_CONFIG_EXECUTOR_SERVICE_PROPERTY_NAME).isEqualTo(Constants.PARALLEL_CONFIG_PREFIX
				+ ParallelHierarchicalTestExecutorServiceFactory.EXECUTOR_SERVICE_PROPERTY_NAME);

		assertThat(Constants.PARALLEL_CONFIG_STRATEGY_PROPERTY_NAME).isEqualTo(Constants.PARALLEL_CONFIG_PREFIX
				+ DefaultParallelExecutionConfigurationStrategy.CONFIG_STRATEGY_PROPERTY_NAME);
		assertThat(Constants.PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME).isEqualTo(Constants.PARALLEL_CONFIG_PREFIX
				+ DefaultParallelExecutionConfigurationStrategy.CONFIG_FIXED_PARALLELISM_PROPERTY_NAME);
		assertThat(Constants.PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME).isEqualTo(
			Constants.PARALLEL_CONFIG_PREFIX
					+ DefaultParallelExecutionConfigurationStrategy.CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME);
		assertThat(Constants.PARALLEL_CONFIG_FIXED_SATURATE_PROPERTY_NAME).isEqualTo(Constants.PARALLEL_CONFIG_PREFIX
				+ DefaultParallelExecutionConfigurationStrategy.CONFIG_FIXED_SATURATE_PROPERTY_NAME);
		assertThat(Constants.PARALLEL_CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME).isEqualTo(Constants.PARALLEL_CONFIG_PREFIX
				+ DefaultParallelExecutionConfigurationStrategy.CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME);
		assertThat(Constants.PARALLEL_CONFIG_CUSTOM_CLASS_PROPERTY_NAME).isEqualTo(Constants.PARALLEL_CONFIG_PREFIX
				+ DefaultParallelExecutionConfigurationStrategy.CONFIG_CUSTOM_CLASS_PROPERTY_NAME);

		assertThat(Constants.DEACTIVATE_ALL_CONDITIONS_PATTERN).isEqualTo(ClassNamePatternFilterUtils.ALL_PATTERN);
	}
}
