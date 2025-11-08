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
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.Locale;

import org.apiguardian.api.API;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters;
import org.junit.platform.engine.support.hierarchical.ForkJoinPoolHierarchicalTestExecutorService.TaskEventListener;

/**
 * Factory for {@link HierarchicalTestExecutorService} instances that support
 * parallel execution.
 *
 * @since 6.1
 * @see ParallelExecutorServiceType
 * @see ForkJoinPoolHierarchicalTestExecutorService
 * @see WorkerThreadPoolHierarchicalTestExecutorService
 */
@API(status = MAINTAINED, since = "6.1")
public final class ParallelHierarchicalTestExecutorServiceFactory {

	/**
	 * Property name used to determine the desired
	 * {@link ParallelExecutorServiceType ParallelExecutorServiceType}.
	 *
	 * <p>Value must be
	 * {@link ParallelExecutorServiceType#FORK_JOIN_POOL FORK_JOIN_POOL} or
	 * {@link ParallelExecutorServiceType#WORKER_THREAD_POOL WORKER_THREAD_POOL},
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
	 * @see ParallelExecutorServiceType
	 * @see ParallelExecutionConfigurationStrategy
	 * @see PrefixedConfigurationParameters
	 */
	public static HierarchicalTestExecutorService create(ConfigurationParameters configurationParameters) {
		var type = configurationParameters.get(EXECUTOR_SERVICE_PROPERTY_NAME, ParallelExecutorServiceType::parse) //
				.orElse(ParallelExecutorServiceType.FORK_JOIN_POOL);
		var configuration = DefaultParallelExecutionConfigurationStrategy.toConfiguration(configurationParameters);
		return create(type, configuration);
	}

	/**
	 * Create a new {@link HierarchicalTestExecutorService} based on the
	 * supplied {@link ConfigurationParameters}.
	 *
	 * <p>The {@value #EXECUTOR_SERVICE_PROPERTY_NAME} key is ignored in favor
	 * of the supplied {@link ParallelExecutorServiceType} parameter when
	 * invoking this method.
	 *
	 * @see ParallelExecutorServiceType
	 * @see ParallelExecutionConfigurationStrategy
	 */
	public static HierarchicalTestExecutorService create(ParallelExecutorServiceType executorServiceType,
			ParallelExecutionConfiguration configuration) {
		return switch (executorServiceType) {
			case FORK_JOIN_POOL -> new ForkJoinPoolHierarchicalTestExecutorService(configuration,
				TaskEventListener.NOOP);
			case WORKER_THREAD_POOL -> new WorkerThreadPoolHierarchicalTestExecutorService(configuration);
		};
	}

	private ParallelHierarchicalTestExecutorServiceFactory() {
	}

	/**
	 * Type of {@link HierarchicalTestExecutorService} that supports parallel
	 * execution.
	 *
	 * @since 6.1
	 */
	@API(status = MAINTAINED, since = "6.1")
	public enum ParallelExecutorServiceType {

		/**
		 * Indicates that {@link ForkJoinPoolHierarchicalTestExecutorService}
		 * should be used.
		 */
		FORK_JOIN_POOL,

		/**
		 * Indicates that {@link WorkerThreadPoolHierarchicalTestExecutorService}
		 * should be used.
		 */
		@API(status = EXPERIMENTAL, since = "6.1")
		WORKER_THREAD_POOL;

		private static ParallelExecutorServiceType parse(String value) {
			return valueOf(value.toUpperCase(Locale.ROOT));
		}
	}

}
