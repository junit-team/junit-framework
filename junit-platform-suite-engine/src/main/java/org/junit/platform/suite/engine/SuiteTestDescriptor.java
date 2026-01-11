/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.joining;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilder.request;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.EngineDiscoveryListener;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.hierarchical.OpenTest4JAwareThrowableCollector;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryResult;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * {@link TestDescriptor} for tests based on the JUnit Platform Suite API.
 *
 * <h2>Default Display Names</h2>
 *
 * <p>The default display name is the simple name of the class.
 *
 * @since 1.8
 * @see SuiteDisplayName
 */
final class SuiteTestDescriptor extends AbstractTestDescriptor {

	static final String SEGMENT_TYPE = "suite";

	private final SuiteLauncherDiscoveryRequestBuilder discoveryRequestBuilder = request();
	private final ConfigurationParameters configurationParameters;
	private final OutputDirectoryCreator outputDirectoryCreator;
	private final Boolean failIfNoTests;
	private final Class<?> suiteClass;
	private final LifecycleMethods lifecycleMethods;

	private @Nullable LauncherDiscoveryResult launcherDiscoveryResult;

	private @Nullable SuiteLauncher launcher;

	SuiteTestDescriptor(UniqueId id, Class<?> suiteClass, ConfigurationParameters configurationParameters,
			OutputDirectoryCreator outputDirectoryCreator, EngineDiscoveryListener discoveryListener,
			DiscoveryIssueReporter issueReporter) {
		super(id, getSuiteDisplayName(suiteClass, issueReporter), ClassSource.from(suiteClass));
		this.configurationParameters = configurationParameters;
		this.outputDirectoryCreator = outputDirectoryCreator;
		this.failIfNoTests = getFailIfNoTests(suiteClass);
		this.suiteClass = suiteClass;
		this.lifecycleMethods = new LifecycleMethods(suiteClass, issueReporter);
		this.discoveryRequestBuilder.listener(DiscoveryIssueForwardingListener.create(id, discoveryListener));
	}

	private static Boolean getFailIfNoTests(Class<?> suiteClass) {
		// @formatter:off
		return findAnnotation(suiteClass, Suite.class)
				.map(Suite::failIfNoTests)
				.orElseThrow(() -> new JUnitException("Suite [%s] was not annotated with @Suite".formatted(suiteClass.getName())));
		// @formatter:on
	}

	SuiteTestDescriptor addDiscoveryRequestFrom(Class<?> suiteClass) {
		Preconditions.condition(launcherDiscoveryResult == null,
			"discovery request cannot be modified after discovery");
		discoveryRequestBuilder.applySelectorsAndFiltersFromSuite(suiteClass);
		return this;
	}

	SuiteTestDescriptor addDiscoveryRequestFrom(UniqueId uniqueId) {
		Preconditions.condition(launcherDiscoveryResult == null,
			"discovery request cannot be modified after discovery");
		discoveryRequestBuilder.selectors(DiscoverySelectors.selectUniqueId(uniqueId));
		return this;
	}

	void discover() {
		if (launcherDiscoveryResult != null) {
			return;
		}

		// @formatter:off
		LauncherDiscoveryRequest request = discoveryRequestBuilder
				.filterStandardClassNamePatterns()
				.disableImplicitConfigurationParameters()
				.parentConfigurationParameters(configurationParameters)
				.applyConfigurationParametersFromSuite(suiteClass)
				.outputDirectoryCreator(outputDirectoryCreator)
				.build();
		// @formatter:on
		this.launcher = SuiteLauncher.create();
		this.launcherDiscoveryResult = launcher.discover(request, getUniqueId());
		// @formatter:off
		launcherDiscoveryResult.getTestEngines()
				.stream()
				.map(testEngine -> launcherDiscoveryResult.getEngineTestDescriptor(testEngine))
				.forEach(this::addChild);
		// @formatter:on
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	private static String getSuiteDisplayName(Class<?> suiteClass, DiscoveryIssueReporter issueReporter) {
		// @formatter:off
		var nonBlank = issueReporter.createReportingCondition(StringUtils::isNotBlank, __ -> {
			String message = "@SuiteDisplayName on %s must be declared with a non-blank value.".formatted(
					suiteClass.getName());
			return DiscoveryIssue.builder(DiscoveryIssue.Severity.WARNING, message)
					.source(ClassSource.from(suiteClass))
					.build();
		}).toPredicate();

		return findAnnotation(suiteClass, SuiteDisplayName.class)
				.map(SuiteDisplayName::value)
				.filter(nonBlank)
				.orElse(suiteClass.getSimpleName());
		// @formatter:on
	}

	void execute(EngineExecutionListener executionListener, NamespacedHierarchicalStore<Namespace> requestLevelStore,
			CancellationToken cancellationToken) {

		if (cancellationToken.isCancellationRequested()) {
			executionListener.executionSkipped(this, "Execution cancelled");
			return;
		}

		executionListener.executionStarted(this);
		ThrowableCollector throwableCollector = new OpenTest4JAwareThrowableCollector();

		executeBeforeSuiteMethods(throwableCollector);

		TestExecutionSummary summary = executeTests(executionListener, requestLevelStore, cancellationToken,
			throwableCollector);

		executeAfterSuiteMethods(throwableCollector);

		TestExecutionResult testExecutionResult = computeTestExecutionResult(summary, throwableCollector);
		executionListener.executionFinished(this, testExecutionResult);
	}

	private void executeBeforeSuiteMethods(ThrowableCollector throwableCollector) {
		if (throwableCollector.isNotEmpty()) {
			return;
		}
		for (Method beforeSuiteMethod : lifecycleMethods.beforeSuite) {
			throwableCollector.execute(() -> ReflectionSupport.invokeMethod(beforeSuiteMethod, null));
			if (throwableCollector.isNotEmpty()) {
				return;
			}
		}
	}

	private @Nullable TestExecutionSummary executeTests(EngineExecutionListener executionListener,
			NamespacedHierarchicalStore<Namespace> requestLevelStore, CancellationToken cancellationToken,
			ThrowableCollector throwableCollector) {

		if (throwableCollector.isNotEmpty()) {
			return null;
		}

		// #2838: The discovery result from a suite may have been filtered by
		// post discovery filters from the launcher. The discovery result should
		// be pruned accordingly.
		LauncherDiscoveryResult discoveryResult = requireNonNull(this.launcherDiscoveryResult).withRetainedEngines(
			getChildren()::contains);

		return requireNonNull(launcher).execute(discoveryResult, executionListener, requestLevelStore,
			cancellationToken);
	}

	private void executeAfterSuiteMethods(ThrowableCollector throwableCollector) {
		for (Method afterSuiteMethod : lifecycleMethods.afterSuite) {
			throwableCollector.execute(() -> ReflectionSupport.invokeMethod(afterSuiteMethod, null));
		}
	}

	private TestExecutionResult computeTestExecutionResult(@Nullable TestExecutionSummary summary,
			ThrowableCollector throwableCollector) {
		var throwable = throwableCollector.getThrowable();
		if (throwable != null) {
			return TestExecutionResult.failed(throwable);
		}
		if (failIfNoTests && requireNonNull(summary).getTestsFoundCount() == 0) {
			return TestExecutionResult.failed(new NoTestsDiscoveredException(suiteClass));
		}
		return TestExecutionResult.successful();
	}

	@Override
	public boolean mayRegisterTests() {
		// While a suite will not register new tests after discovery, we pretend
		// it does. This allows the suite to fail if no tests were discovered.
		// Otherwise, the empty suite would be pruned.
		return true;
	}

	private static class LifecycleMethods {

		final List<Method> beforeSuite;
		final List<Method> afterSuite;

		LifecycleMethods(Class<?> suiteClass, DiscoveryIssueReporter issueReporter) {
			beforeSuite = LifecycleMethodUtils.findBeforeSuiteMethods(suiteClass, issueReporter);
			afterSuite = LifecycleMethodUtils.findAfterSuiteMethods(suiteClass, issueReporter);
		}
	}

	private record DiscoveryIssueForwardingListener(EngineDiscoveryListener discoveryListener,
			BiFunction<UniqueId, DiscoveryIssue, DiscoveryIssue> issueTransformer)
			implements LauncherDiscoveryListener {

		private static final Predicate<Segment> SUITE_SEGMENTS = where(Segment::getType, isEqual(SEGMENT_TYPE));

		static DiscoveryIssueForwardingListener create(UniqueId id, EngineDiscoveryListener discoveryListener) {
			boolean isNestedSuite = id.getSegments().stream().filter(SUITE_SEGMENTS).count() > 1;
			if (isNestedSuite) {
				return new DiscoveryIssueForwardingListener(discoveryListener, (__, issue) -> issue);
			}
			return new DiscoveryIssueForwardingListener(discoveryListener,
				(engineUniqueId, issue) -> issue.withMessage(message -> {
					String engineId = engineUniqueId.getLastSegment().getValue();
					if (SuiteEngineDescriptor.ENGINE_ID.equals(engineId)) {
						return message;
					}
					String suitePath = engineUniqueId.getSegments().stream() //
							.filter(SUITE_SEGMENTS) //
							.map(Segment::getValue) //
							.collect(joining(" > "));
					if (message.endsWith(".")) {
						message = message.substring(0, message.length() - 1);
					}
					return "[%s] %s (via @Suite %s).".formatted(engineId, message, suitePath);
				}));
		}

		@Override
		public void issueEncountered(UniqueId engineUniqueId, DiscoveryIssue issue) {
			DiscoveryIssue transformedIssue = this.issueTransformer.apply(engineUniqueId, issue);
			this.discoveryListener.issueEncountered(engineUniqueId, transformedIssue);
		}
	}
}
