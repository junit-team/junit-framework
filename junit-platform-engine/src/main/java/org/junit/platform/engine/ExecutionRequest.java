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

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

/**
 * Provides a single {@link TestEngine} access to the information necessary to
 * execute its tests.
 *
 * <p>A request contains an engine's root {@link TestDescriptor}, the
 * {@link EngineExecutionListener} to be notified of test execution events, the
 * {@link ConfigurationParameters} that the engine may use to influence test
 * execution, and an {@link OutputDirectoryProvider} for writing reports and
 * other output files.
 *
 * @since 1.0
 * @see TestEngine
 */
@API(status = STABLE, since = "1.0")
public class ExecutionRequest {

	private final TestDescriptor rootTestDescriptor;
	private final EngineExecutionListener engineExecutionListener;
	private final ConfigurationParameters configurationParameters;
	private final @Nullable OutputDirectoryProvider outputDirectoryProvider;
	private final @Nullable NamespacedHierarchicalStore<Namespace> requestLevelStore;
	private final @Nullable CancellationToken cancellationToken;

	@Deprecated
	@API(status = DEPRECATED, since = "1.11")
	public ExecutionRequest(TestDescriptor rootTestDescriptor, EngineExecutionListener engineExecutionListener,
			ConfigurationParameters configurationParameters) {
		this(rootTestDescriptor, engineExecutionListener, configurationParameters, null, null, null);
	}

	private ExecutionRequest(TestDescriptor rootTestDescriptor, EngineExecutionListener engineExecutionListener,
			ConfigurationParameters configurationParameters, @Nullable OutputDirectoryProvider outputDirectoryProvider,
			@Nullable NamespacedHierarchicalStore<Namespace> requestLevelStore,
			@Nullable CancellationToken cancellationToken) {
		this.rootTestDescriptor = Preconditions.notNull(rootTestDescriptor, "rootTestDescriptor must not be null");
		this.engineExecutionListener = Preconditions.notNull(engineExecutionListener,
			"engineExecutionListener must not be null");
		this.configurationParameters = Preconditions.notNull(configurationParameters,
			"configurationParameters must not be null");
		this.outputDirectoryProvider = outputDirectoryProvider;
		this.requestLevelStore = requestLevelStore;
		this.cancellationToken = cancellationToken;
	}

	/**
	 * Factory for creating an execution request.
	 *
	 * @param rootTestDescriptor the engine's root {@link TestDescriptor}
	 * @param engineExecutionListener the {@link EngineExecutionListener} to be
	 * notified of test execution events
	 * @param configurationParameters {@link ConfigurationParameters} that the
	 * engine may use to influence test execution
	 * @return a new {@code ExecutionRequest}; never {@code null}
	 * @since 1.9
	 * @deprecated without replacement
	 */
	@Deprecated
	@API(status = DEPRECATED, since = "1.11")
	public static ExecutionRequest create(TestDescriptor rootTestDescriptor,
			EngineExecutionListener engineExecutionListener, ConfigurationParameters configurationParameters) {
		return new ExecutionRequest(rootTestDescriptor, engineExecutionListener, configurationParameters);
	}

	/**
	 * Factory for creating an execution request.
	 *
	 * @param rootTestDescriptor the engine's root {@link TestDescriptor}; never
	 * {@code null}
	 * @param engineExecutionListener the {@link EngineExecutionListener} to be
	 * notified of test execution events; never {@code null}
	 * @param configurationParameters {@link ConfigurationParameters} that the
	 * engine may use to influence test execution; never {@code null}
	 * @param outputDirectoryProvider {@link OutputDirectoryProvider} for
	 * writing reports and other output files; never {@code null}
	 * @param requestLevelStore {@link NamespacedHierarchicalStore} for storing
	 * request-scoped data; never {@code null}
	 * @return a new {@code ExecutionRequest}; never {@code null}
	 * @since 1.13
	 */
	@API(status = INTERNAL, since = "1.13")
	public static ExecutionRequest create(TestDescriptor rootTestDescriptor,
			EngineExecutionListener engineExecutionListener, ConfigurationParameters configurationParameters,
			OutputDirectoryProvider outputDirectoryProvider, NamespacedHierarchicalStore<Namespace> requestLevelStore,
			CancellationToken cancellationToken) {

		return new ExecutionRequest(rootTestDescriptor, engineExecutionListener, configurationParameters,
			Preconditions.notNull(outputDirectoryProvider, "outputDirectoryProvider must not be null"),
			Preconditions.notNull(requestLevelStore, "requestLevelStore must not be null"),
			Preconditions.notNull(cancellationToken, "cancellationToken must not be null"));
	}

	/**
	 * {@return the root {@link TestDescriptor} of the engine that processes this
	 * request}
	 *
	 * <p><strong>Note</strong>: the <em>root</em> descriptor is the
	 * {@code TestDescriptor} returned by
	 * {@link TestEngine#discover(EngineDiscoveryRequest, UniqueId)}.
	 */
	public TestDescriptor getRootTestDescriptor() {
		return this.rootTestDescriptor;
	}

	/**
	 * {@return the {@link EngineExecutionListener} to be notified of test execution
	 * events}
	 */
	public EngineExecutionListener getEngineExecutionListener() {
		return this.engineExecutionListener;
	}

	/**
	 * {@return the {@link ConfigurationParameters} that the engine may use to
	 * influence test execution}
	 */
	public ConfigurationParameters getConfigurationParameters() {
		return this.configurationParameters;
	}

	/**
	 * {@return the {@link OutputDirectoryProvider} for this request for writing
	 * reports and other output files}
	 *
	 * @throws PreconditionViolationException if the output directory provider
	 * is not available
	 * @since 1.12
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public OutputDirectoryProvider getOutputDirectoryProvider() {
		return Preconditions.notNull(this.outputDirectoryProvider,
			"No OutputDirectoryProvider was configured for this request");
	}

	/**
	 * {@return the {@link NamespacedHierarchicalStore} for this request for
	 * storing request-scoped data}
	 *
	 * <p>All stored values that implement {@link AutoCloseable} are notified by
	 * invoking their {@code close()} methods when this request has been
	 * executed.
	 *
	 * @since 1.13
	 * @see NamespacedHierarchicalStore
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public NamespacedHierarchicalStore<Namespace> getStore() {
		return Preconditions.notNull(this.requestLevelStore,
			"No NamespacedHierarchicalStore was configured for this request");
	}

	public CancellationToken getCancellationToken() {
		return Preconditions.notNull(this.cancellationToken, "No CancellationToken was configured for this request");
	}

}
