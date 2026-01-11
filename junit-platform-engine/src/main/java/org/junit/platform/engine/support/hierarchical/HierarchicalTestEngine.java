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

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestEngine;

/**
 * Abstract base class for all {@link TestEngine} implementations that wish
 * to organize test suites hierarchically based on the {@link Node} abstraction.
 *
 * @param <C> the type of {@code EngineExecutionContext} used by this engine
 * @since 1.0
 * @see Node
 */
@API(status = MAINTAINED, since = "1.0")
public abstract class HierarchicalTestEngine<C extends EngineExecutionContext> implements TestEngine {

	public HierarchicalTestEngine() {
	}

	/**
	 * Create an {@linkplain #createExecutorService(ExecutionRequest) executor
	 * service}; create an initial {@linkplain #createExecutionContext execution
	 * context}; execute the behavior of all {@linkplain Node nodes} in the
	 * hierarchy starting with the supplied {@code request}'s
	 * {@linkplain ExecutionRequest#getRootTestDescriptor() root} and notify
	 * its {@linkplain ExecutionRequest#getEngineExecutionListener() execution
	 * listener} of test execution events.
	 *
	 * <p>Supports cancellation via the {@link CancellationToken} passed in the
	 * supplied {@code request}.
	 *
	 * @see Node
	 * @see #createExecutorService
	 * @see #createExecutionContext
	 */
	@Override
	public final void execute(ExecutionRequest request) {
		try (HierarchicalTestExecutorService executorService = createExecutorService(request)) {
			C executionContext = createExecutionContext(request);
			ThrowableCollector.Factory throwableCollectorFactory = createThrowableCollectorFactory(request);
			new HierarchicalTestExecutor<>(request, executionContext, executorService,
				throwableCollectorFactory).execute().get();
		}
		catch (Exception exception) {
			throw new JUnitException("Error executing tests for engine " + getId(), exception);
		}
	}

	/**
	 * Create the {@linkplain HierarchicalTestExecutorService executor service}
	 * to use for executing the supplied {@linkplain ExecutionRequest request}.
	 *
	 * <p>An engine may use the information in the supplied <em>request</em>
	 * such as the contained
	 * {@linkplain ExecutionRequest#getConfigurationParameters() configuration parameters}
	 * to decide what kind of service to return or how to configure it.
	 *
	 * <p>By default, this method returns an instance of
	 * {@link SameThreadHierarchicalTestExecutorService}.
	 *
	 * @param request the request about to be executed
	 * @since 1.3
	 * @see ForkJoinPoolHierarchicalTestExecutorService
	 * @see SameThreadHierarchicalTestExecutorService
	 */
	@API(status = STABLE, since = "1.10")
	protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
		return new SameThreadHierarchicalTestExecutorService();
	}

	/**
	 * Create the {@linkplain ThrowableCollector.Factory factory} for creating
	 * {@link ThrowableCollector} instances used to handle exceptions that occur
	 * during execution of this engine's tests.
	 *
	 * <p>An engine may use the information in the supplied <em>request</em>
	 * such as the contained
	 * {@linkplain ExecutionRequest#getConfigurationParameters() configuration parameters}
	 * to decide what kind of factory to return or how to configure it.
	 *
	 * <p>By default, this method returns a factory that always creates instances of
	 * {@link OpenTest4JAwareThrowableCollector}.
	 *
	 * @param request the request about to be executed
	 * @since 1.3
	 * @see OpenTest4JAwareThrowableCollector
	 * @see ThrowableCollector
	 */
	@API(status = STABLE, since = "1.10")
	protected ThrowableCollector.Factory createThrowableCollectorFactory(ExecutionRequest request) {
		return OpenTest4JAwareThrowableCollector::new;
	}

	/**
	 * Create the initial execution context for executing the supplied
	 * {@linkplain ExecutionRequest request}.
	 *
	 * @param request the request about to be executed
	 * @return the initial context that will be passed to nodes in the hierarchy
	 */
	protected abstract C createExecutionContext(ExecutionRequest request);

}
