/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.Locale;

import org.apiguardian.api.API;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters;

/**
 * Factory for {@link HierarchicalTestExecutorService} instances that support
 * concurrent execution.
 *
 * @since 6.1
 * @see ConcurrentExecutorServiceType
 * @see ForkJoinPoolHierarchicalTestExecutorService
 * @see WorkerThreadPoolHierarchicalTestExecutorService
 */
@API(status = EXPERIMENTAL, since = "6.1")
public class ConcurrentHierarchicalTestExecutorServiceFactory {

	/**
	 * Property name used to determine the desired
	 * {@link ConcurrentExecutorServiceType ConcurrentExecutorServiceType}.
	 *
	 * <p>Value must be
	 * {@link ConcurrentExecutorServiceType#FORK_JOIN_POOL FORK_JOIN_POOL} or
	 * {@link ConcurrentExecutorServiceType#WORKER_THREAD_POOL WORKER_THREAD_POOL},
	 * ignoring case.
	 */
	public static final String EXECUTOR_SERVICE_PROPERTY_NAME = "executor-service";

	/**
	 * Create a new {@link HierarchicalTestExecutorService} based on the
	 * supplied {@link ConfigurationParameters}.
	 *
	 * <p>This method is typically invoked with an instance of
	 * {@link PrefixedConfigurationParameters} that was created with an
	 * engine-specific prefix.
	 *
	 * <p>The {@value #EXECUTOR_SERVICE_PROPERTY_NAME} key is used to determine
	 * which service implementation is to be used. Which other parameters are
	 * read depends on the configured
	 * {@link ParallelExecutionConfigurationStrategy} which is determined by the
	 * {@value DefaultParallelExecutionConfigurationStrategy#CONFIG_STRATEGY_PROPERTY_NAME}
	 * key.
	 *
	 * @see #EXECUTOR_SERVICE_PROPERTY_NAME
	 * @see ConcurrentExecutorServiceType
	 * @see ParallelExecutionConfigurationStrategy
	 * @see PrefixedConfigurationParameters
	 */
	public static HierarchicalTestExecutorService create(ConfigurationParameters configurationParameters) {
		var executorServiceType = configurationParameters.get(EXECUTOR_SERVICE_PROPERTY_NAME,
			it -> ConcurrentExecutorServiceType.valueOf(it.toUpperCase(Locale.ROOT))) //
				.orElse(ConcurrentExecutorServiceType.FORK_JOIN_POOL);
		var configuration = DefaultParallelExecutionConfigurationStrategy.toConfiguration(configurationParameters);
		return create(executorServiceType, configuration);
	}

	/**
	 * Create a new {@link HierarchicalTestExecutorService} based on the
	 * supplied {@link ConfigurationParameters}.
	 *
	 * @see ConcurrentExecutorServiceType
	 * @see ParallelExecutionConfigurationStrategy
	 */
	public static HierarchicalTestExecutorService create(ConcurrentExecutorServiceType type,
			ParallelExecutionConfiguration configuration) {
		return switch (type) {
			case FORK_JOIN_POOL -> new ForkJoinPoolHierarchicalTestExecutorService(configuration);
			case WORKER_THREAD_POOL -> new WorkerThreadPoolHierarchicalTestExecutorService(configuration);
		};
	}

	private ConcurrentHierarchicalTestExecutorServiceFactory() {
	}

	/**
	 * Type of {@link HierarchicalTestExecutorService} that supports concurrent
	 * execution.
	 */
	public enum ConcurrentExecutorServiceType {

		/**
		 * Indicates that {@link ForkJoinPoolHierarchicalTestExecutorService}
		 * should be used.
		 */
		FORK_JOIN_POOL,

		/**
		 * Indicates that {@link WorkerThreadPoolHierarchicalTestExecutorService}
		 * should be used.
		 */
		WORKER_THREAD_POOL

	}

}
