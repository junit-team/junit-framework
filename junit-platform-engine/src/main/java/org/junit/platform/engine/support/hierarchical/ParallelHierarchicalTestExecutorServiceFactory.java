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

	/**
	 * Create a reactive {@link HierarchicalTestExecutorService} with the given
	 * maximum parallelism.
	 *
	 * <p>The reactive service coordinates concurrency and resource-lock waits
	 * through completion stages rather than parked threads. Note: currently the
	 * service still parks a worker while the body of a node runs; a fully
	 * non-blocking execution lane from the top of the hierarchy down (option
	 * "A") would be preferable but is intentionally deferred to limit the
	 * impact of this first round.
	 *
	 * @param parallelism the maximum number of nodes executed concurrently
	 * @return a new reactive {@link HierarchicalTestExecutorService}; never
	 * {@code null}
	 */
	public static HierarchicalTestExecutorService createReactive(int parallelism) {
		return new ReactiveHierarchicalTestExecutorService(parallelism);
	}

	/**
	 * Create a reactive {@link HierarchicalTestExecutorService} for the
	 * standalone cooperative lane, without any thread configuration.
	 *
	 * <p>Concurrency comes from the asynchronous test methods' own returned
	 * contexts; a small trigger pool starts each async body. Container and
	 * synchronous/non-async nodes run in discovery order.
	 *
	 * @return a new reactive {@link HierarchicalTestExecutorService}; never
	 * {@code null}
	 */
	public static HierarchicalTestExecutorService createReactive() {
		return new ReactiveHierarchicalTestExecutorService();
	}

	/**
	 * Create a reactive {@link HierarchicalTestExecutorService} with the
	 * parallelism derived from the supplied {@link ConfigurationParameters}.
	 *
	 * @param configurationParameters the configuration parameters to read the
	 * parallelism from
	 * @return a new reactive {@link HierarchicalTestExecutorService}; never
	 * {@code null}
	 */
	public static HierarchicalTestExecutorService createReactive(ConfigurationParameters configurationParameters) {
		var configuration = DefaultParallelExecutionConfigurationStrategy.toConfiguration(configurationParameters);
		return createReactive(configuration.getParallelism());
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
