/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.Collections.unmodifiableCollection;
import static org.junit.platform.engine.support.store.NamespacedHierarchicalStore.CloseAction.closeAutoCloseables;
import static org.junit.platform.launcher.core.LauncherPhase.DISCOVERY;
import static org.junit.platform.launcher.core.LauncherPhase.EXECUTION;

import java.util.Collection;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Default implementation of the {@link Launcher} API.
 *
 * <p>External clients can obtain an instance by invoking
 * {@link LauncherFactory#create()}.
 *
 * @since 1.0
 * @see Launcher
 * @see LauncherFactory
 */
class DefaultLauncher implements Launcher {

	private final LauncherListenerRegistry listenerRegistry = new LauncherListenerRegistry();
	private final EngineExecutionOrchestrator executionOrchestrator = new EngineExecutionOrchestrator(
		listenerRegistry.testExecutionListeners);
	private final EngineDiscoveryOrchestrator discoveryOrchestrator;
	private final NamespacedHierarchicalStore<Namespace> sessionLevelStore;

	/**
	 * Construct a new {@code DefaultLauncher} with the supplied test engines.
	 *
	 * @param testEngines the test engines to delegate to; never {@code null} or
	 * empty
	 * @param postDiscoveryFilters the additional post discovery filters for
	 * discovery requests; never {@code null}
	 */
	DefaultLauncher(Iterable<TestEngine> testEngines, Collection<PostDiscoveryFilter> postDiscoveryFilters,
			NamespacedHierarchicalStore<Namespace> sessionLevelStore) {
		Preconditions.condition(testEngines.iterator().hasNext(),
			() -> "Cannot create Launcher without at least one TestEngine; "
					+ "consider adding an engine implementation JAR to the classpath");
		Preconditions.notNull(postDiscoveryFilters, "PostDiscoveryFilter array must not be null");
		Preconditions.containsNoNullElements(postDiscoveryFilters,
			"PostDiscoveryFilter array must not contain null elements");
		this.discoveryOrchestrator = new EngineDiscoveryOrchestrator(testEngines,
			unmodifiableCollection(postDiscoveryFilters), listenerRegistry.launcherDiscoveryListeners);
		this.sessionLevelStore = sessionLevelStore;
	}

	@Override
	public void registerLauncherDiscoveryListeners(LauncherDiscoveryListener... listeners) {
		this.listenerRegistry.launcherDiscoveryListeners.addAll(listeners);
	}

	@Override
	public void registerTestExecutionListeners(TestExecutionListener... listeners) {
		this.listenerRegistry.testExecutionListeners.addAll(listeners);
	}

	@Override
	public TestPlan discover(LauncherDiscoveryRequest discoveryRequest) {
		Preconditions.notNull(discoveryRequest, "LauncherDiscoveryRequest must not be null");
		return InternalTestPlan.from(discover(discoveryRequest, DISCOVERY));
	}

	@Override
	public void execute(LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {
		var executionRequest = LauncherExecutionRequestBuilder.request(discoveryRequest) //
				.listeners(listeners) //
				.build();
		execute(executionRequest);
	}

	@Override
	public void execute(TestPlan testPlan, TestExecutionListener... listeners) {
		var executionRequest = LauncherExecutionRequestBuilder.request(testPlan) //
				.listeners(listeners) //
				.build();
		execute(executionRequest);
	}

	@Override
	public void execute(LauncherExecutionRequest launcherExecutionRequest) {
		var testPlan = launcherExecutionRequest.getTestPlan().map(it -> {
			Preconditions.condition(it instanceof InternalTestPlan, "TestPlan was not returned by this Launcher");
			return ((InternalTestPlan) it);
		}).orElseGet(() -> {
			Preconditions.condition(launcherExecutionRequest.getDiscoveryRequest().isPresent(),
				"Either a TestPlan or LauncherDiscoveryRequest must be present in the LauncherExecutionRequest");
			return InternalTestPlan.from(discover(launcherExecutionRequest.getDiscoveryRequest().get(), EXECUTION));
		});
		execute(testPlan, launcherExecutionRequest.getAdditionalTestExecutionListeners(),
			launcherExecutionRequest.getCancellationToken());
	}

	private LauncherDiscoveryResult discover(LauncherDiscoveryRequest discoveryRequest, LauncherPhase phase) {
		return discoveryOrchestrator.discover(discoveryRequest, phase);
	}

	private void execute(InternalTestPlan internalTestPlan, Collection<? extends TestExecutionListener> listeners,
			CancellationToken cancellationToken) {
		try (NamespacedHierarchicalStore<Namespace> requestLevelStore = createRequestLevelStore()) {
			executionOrchestrator.execute(internalTestPlan, requestLevelStore, listeners, cancellationToken);
		}
	}

	private NamespacedHierarchicalStore<Namespace> createRequestLevelStore() {
		return new NamespacedHierarchicalStore<>(sessionLevelStore, closeAutoCloseables());
	}

}
