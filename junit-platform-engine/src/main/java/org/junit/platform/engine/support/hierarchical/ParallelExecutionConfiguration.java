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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Configuration to use for parallel test execution.
 *
 * <p>Instances of this class are intended to be used to configure
 * implementations of {@link HierarchicalTestExecutorService}. Such
 * implementations may use all of the properties in this class or
 * only a subset.
 *
 * @since 1.3
 * @see ForkJoinPoolHierarchicalTestExecutorService
 * @see ParallelExecutionConfigurationStrategy
 * @see DefaultParallelExecutionConfigurationStrategy
 */
@API(status = STABLE, since = "1.10")
public interface ParallelExecutionConfiguration {

	/**
	 * Get the parallelism to be used.
	 *
	 * @see ForkJoinPool#getParallelism()
	 */
	int getParallelism();

	/**
	 * Get the minimum number of runnable threads to be used.
	 */
	int getMinimumRunnable();

	/**
	 * Get the maximum thread pool size to be used.
	 */
	int getMaxPoolSize();

	/**
	 * Get the core thread pool size to be used.
	 */
	int getCorePoolSize();

	/**
	 * Get the number of seconds for which inactive threads should be kept alive
	 * before terminating them and shrinking the thread pool.
	 */
	int getKeepAliveSeconds();

	/**
	 * Get the saturate predicate to be used for the execution's {@link ForkJoinPool}.
	 * @return the saturate predicate to be passed to the {@code ForkJoinPool} constructor; may be {@code null}
	 * @since 1.9
	 * @see ForkJoinPool#ForkJoinPool(int, ForkJoinPool.ForkJoinWorkerThreadFactory, Thread.UncaughtExceptionHandler,
	 * boolean, int, int, int, Predicate, long, java.util.concurrent.TimeUnit)
	 */
	@API(status = STABLE, since = "1.11")
	default @Nullable Predicate<? super ForkJoinPool> getSaturatePredicate() {
		return null;
	}

}
