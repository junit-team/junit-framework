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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.launcher.LauncherConstants.DRY_RUN_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherPhase.getDiscoveryIssueFailurePhase;
import static org.junit.platform.launcher.core.ListenerRegistry.forEngineExecutionListeners;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryResult.EngineResultInfo;

/**
 * Orchestrates test execution using the configured test engines.
 *
 * @since 1.7
 */
@API(status = INTERNAL, since = "1.7", consumers = { "org.junit.platform.testkit", "org.junit.platform.suite.engine" })
public class EngineExecutionOrchestrator {

	private final ListenerRegistry<TestExecutionListener> listenerRegistry;

	public EngineExecutionOrchestrator() {
		this(ListenerRegistry.forTestExecutionListeners());
	}

	EngineExecutionOrchestrator(ListenerRegistry<TestExecutionListener> listenerRegistry) {
		this.listenerRegistry = listenerRegistry;
	}

	void execute(InternalTestPlan internalTestPlan, NamespacedHierarchicalStore<Namespace> requestLevelStore,
			Collection<? extends TestExecutionListener> listeners) {
		ConfigurationParameters configurationParameters = internalTestPlan.getConfigurationParameters();
		ListenerRegistry<TestExecutionListener> testExecutionListenerListeners = buildListenerRegistryForExecution(
			listeners);
		withInterceptedStreams(configurationParameters, testExecutionListenerListeners,
			testExecutionListener -> execute(internalTestPlan, EngineExecutionListener.NOOP, testExecutionListener,
				requestLevelStore));
	}

	/**
	 * Executes tests for the supplied {@linkplain LauncherDiscoveryResult
	 * discoveryResult} and notifies the supplied {@linkplain
	 * EngineExecutionListener engineExecutionListener} and
	 * {@linkplain TestExecutionListener testExecutionListener} of execution
	 * events.
	 */
	@API(status = INTERNAL, since = "1.9", consumers = { "org.junit.platform.suite.engine" })
	public void execute(LauncherDiscoveryResult discoveryResult, EngineExecutionListener engineExecutionListener,
			TestExecutionListener testExecutionListener, NamespacedHierarchicalStore<Namespace> requestLevelStore) {
		Preconditions.notNull(discoveryResult, "discoveryResult must not be null");
		Preconditions.notNull(engineExecutionListener, "engineExecutionListener must not be null");
		Preconditions.notNull(testExecutionListener, "testExecutionListener must not be null");
		Preconditions.notNull(requestLevelStore, "requestLevelStore must not be null");

		InternalTestPlan internalTestPlan = InternalTestPlan.from(discoveryResult);
		execute(internalTestPlan, engineExecutionListener, testExecutionListener, requestLevelStore);
	}

	private void execute(InternalTestPlan internalTestPlan, EngineExecutionListener parentEngineExecutionListener,
			TestExecutionListener testExecutionListener, NamespacedHierarchicalStore<Namespace> requestLevelStore) {
		internalTestPlan.markStarted();

		// Do not directly pass the internal test plan to test execution listeners.
		// Hyrum's Law indicates that someone will eventually come to depend on it.
		TestPlan testPlan = internalTestPlan.getDelegate();
		LauncherDiscoveryResult discoveryResult = internalTestPlan.getDiscoveryResult();

		testExecutionListener.testPlanExecutionStarted(testPlan);
		if (isDryRun(internalTestPlan)) {
			dryRun(testPlan, testExecutionListener);
		}
		else {
			execute(discoveryResult,
				buildEngineExecutionListener(parentEngineExecutionListener, testExecutionListener, testPlan),
				requestLevelStore);
		}
		testExecutionListener.testPlanExecutionFinished(testPlan);
	}

	private Boolean isDryRun(InternalTestPlan internalTestPlan) {
		return internalTestPlan.getConfigurationParameters().getBoolean(DRY_RUN_PROPERTY_NAME).orElse(false);
	}

	private void dryRun(TestPlan testPlan, TestExecutionListener listener) {
		testPlan.accept(new TestPlan.Visitor() {
			@Override
			public void preVisitContainer(TestIdentifier testIdentifier) {
				listener.executionStarted(testIdentifier);
			}

			@Override
			public void visit(TestIdentifier testIdentifier) {
				if (!testIdentifier.isContainer()) {
					listener.executionSkipped(testIdentifier, "JUnit Platform dry-run mode is enabled");
				}
			}

			@Override
			public void postVisitContainer(TestIdentifier testIdentifier) {
				listener.executionFinished(testIdentifier, TestExecutionResult.successful());
			}
		});
	}

	private static EngineExecutionListener buildEngineExecutionListener(
			EngineExecutionListener parentEngineExecutionListener, TestExecutionListener testExecutionListener,
			TestPlan testPlan) {
		ListenerRegistry<EngineExecutionListener> engineExecutionListenerRegistry = forEngineExecutionListeners();
		engineExecutionListenerRegistry.add(new ExecutionListenerAdapter(testPlan, testExecutionListener));
		engineExecutionListenerRegistry.add(parentEngineExecutionListener);
		return engineExecutionListenerRegistry.getCompositeListener();
	}

	private void withInterceptedStreams(ConfigurationParameters configurationParameters,
			ListenerRegistry<TestExecutionListener> listenerRegistry, Consumer<TestExecutionListener> action) {

		TestExecutionListener testExecutionListener = listenerRegistry.getCompositeListener();
		Optional<StreamInterceptingTestExecutionListener> streamInterceptingTestExecutionListener = StreamInterceptingTestExecutionListener.create(
			configurationParameters, testExecutionListener::reportingEntryPublished);
		streamInterceptingTestExecutionListener.ifPresent(listenerRegistry::add);
		try {
			action.accept(listenerRegistry.getCompositeListener());
		}
		finally {
			streamInterceptingTestExecutionListener.ifPresent(StreamInterceptingTestExecutionListener::unregister);
		}
	}

	/**
	 * Executes tests for the supplied {@linkplain LauncherDiscoveryResult
	 * discovery results} and notifies the supplied {@linkplain
	 * EngineExecutionListener listener} of execution events.
	 */
	@API(status = INTERNAL, since = "1.7", consumers = { "org.junit.platform.testkit" })
	public void execute(LauncherDiscoveryResult discoveryResult, EngineExecutionListener engineExecutionListener,
			NamespacedHierarchicalStore<Namespace> requestLevelStore) {
		Preconditions.notNull(discoveryResult, "discoveryResult must not be null");
		Preconditions.notNull(engineExecutionListener, "engineExecutionListener must not be null");

		ConfigurationParameters configurationParameters = discoveryResult.getConfigurationParameters();
		EngineExecutionListener listener = selectExecutionListener(engineExecutionListener, configurationParameters);

		for (TestEngine testEngine : discoveryResult.getTestEngines()) {
			failOrExecuteEngine(discoveryResult, listener, testEngine, requestLevelStore);
		}
	}

	private static EngineExecutionListener selectExecutionListener(EngineExecutionListener engineExecutionListener,
			ConfigurationParameters configurationParameters) {
		boolean stackTracePruningEnabled = configurationParameters.getBoolean(STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME) //
				.orElse(true);
		if (stackTracePruningEnabled) {
			return new StackTracePruningEngineExecutionListener(engineExecutionListener);
		}
		return engineExecutionListener;
	}

	private void failOrExecuteEngine(LauncherDiscoveryResult discoveryResult, EngineExecutionListener listener,
			TestEngine testEngine, NamespacedHierarchicalStore<Namespace> requestLevelStore) {
		EngineResultInfo engineDiscoveryResult = discoveryResult.getEngineResult(testEngine);
		DiscoveryIssueNotifier discoveryIssueNotifier = shouldReportDiscoveryIssues(discoveryResult) //
				? engineDiscoveryResult.getDiscoveryIssueNotifier() //
				: DiscoveryIssueNotifier.NO_ISSUES;
		TestDescriptor engineDescriptor = engineDiscoveryResult.getRootDescriptor();
		Throwable failure = engineDiscoveryResult.getCause() //
				.orElseGet(() -> discoveryIssueNotifier.createExceptionForCriticalIssues(testEngine));
		if (failure != null) {
			listener.executionStarted(engineDescriptor);
			if (engineDiscoveryResult.getCause().isPresent()) {
				discoveryIssueNotifier.logCriticalIssues(testEngine);
			}
			discoveryIssueNotifier.logNonCriticalIssues(testEngine);
			listener.executionFinished(engineDescriptor, TestExecutionResult.failed(failure));
		}
		else {
			executeEngine(engineDescriptor, listener, discoveryResult.getConfigurationParameters(), testEngine,
				discoveryResult.getOutputDirectoryProvider(), discoveryIssueNotifier, requestLevelStore);
		}
	}

	private static boolean shouldReportDiscoveryIssues(LauncherDiscoveryResult discoveryResult) {
		ConfigurationParameters configurationParameters = discoveryResult.getConfigurationParameters();
		return getDiscoveryIssueFailurePhase(configurationParameters).orElse(
			LauncherPhase.EXECUTION) == LauncherPhase.EXECUTION;
	}

	private ListenerRegistry<TestExecutionListener> buildListenerRegistryForExecution(
			Collection<? extends TestExecutionListener> listeners) {
		if (listeners.isEmpty()) {
			return this.listenerRegistry;
		}
		return ListenerRegistry.copyOf(this.listenerRegistry).addAll(listeners);
	}

	private void executeEngine(TestDescriptor engineDescriptor, EngineExecutionListener listener,
			ConfigurationParameters configurationParameters, TestEngine testEngine,
			OutputDirectoryProvider outputDirectoryProvider, DiscoveryIssueNotifier discoveryIssueNotifier,
			NamespacedHierarchicalStore<Namespace> requestLevelStore) {
		OutcomeDelayingEngineExecutionListener delayingListener = new OutcomeDelayingEngineExecutionListener(listener,
			engineDescriptor);
		try {
			testEngine.execute(ExecutionRequest.create(engineDescriptor, delayingListener, configurationParameters,
				outputDirectoryProvider, requestLevelStore));
			discoveryIssueNotifier.logNonCriticalIssues(testEngine);
			delayingListener.reportEngineOutcome();
		}
		catch (Throwable throwable) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
			JUnitException cause = null;
			if (throwable instanceof LinkageError error) {
				cause = ClasspathAlignmentChecker.check(error).orElse(null);
			}
			if (cause == null) {
				String message = "TestEngine with ID '%s' failed to execute tests".formatted(testEngine.getId());
				cause = new JUnitException(message, throwable);
			}
			delayingListener.reportEngineStartIfNecessary();
			discoveryIssueNotifier.logNonCriticalIssues(testEngine);
			delayingListener.reportEngineFailure(cause);
		}
	}
}
