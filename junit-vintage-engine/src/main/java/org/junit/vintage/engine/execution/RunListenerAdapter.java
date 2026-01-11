/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.execution;

import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.SEGMENT_TYPE_DYNAMIC;

import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.Ignore;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.descriptor.TestSourceProvider;
import org.junit.vintage.engine.descriptor.VintageTestDescriptor;
import org.junit.vintage.engine.support.UniqueIdReader;
import org.junit.vintage.engine.support.UniqueIdStringifier;

/**
 * @since 4.12
 */
class RunListenerAdapter extends RunListener {

	private final TestRun testRun;
	private final EngineExecutionListener listener;
	private final TestSourceProvider testSourceProvider;
	private final Function<Description, String> uniqueIdExtractor;

	RunListenerAdapter(TestRun testRun, EngineExecutionListener listener, TestSourceProvider testSourceProvider) {
		this.testRun = testRun;
		this.listener = listener;
		this.testSourceProvider = testSourceProvider;
		this.uniqueIdExtractor = new UniqueIdReader().andThen(new UniqueIdStringifier());
	}

	@Override
	public void testRunStarted(Description description) {
		if (description.isSuite() && !testRun.getRunnerTestDescriptor().isIgnored()) {
			fireExecutionStarted(testRun.getRunnerTestDescriptor(), EventType.REPORTED);
		}
	}

	@Override
	public void testSuiteStarted(Description description) {
		RunnerTestDescriptor runnerTestDescriptor = testRun.getRunnerTestDescriptor();
		// runnerTestDescriptor is reported in testRunStarted
		if (!runnerTestDescriptor.getDescription().equals(description)) {
			testStarted(lookupOrRegisterNextTestDescriptor(description), EventType.REPORTED);
		}
	}

	@Override
	public void testIgnored(Description description) {
		TestDescriptor testDescriptor = lookupOrRegisterNextTestDescriptor(description);
		String reason = determineReasonForIgnoredTest(testDescriptor, description).orElse("<unknown>");
		testIgnored(testDescriptor, reason);
	}

	@Override
	public void testStarted(Description description) {
		testStarted(lookupOrRegisterNextTestDescriptor(description), EventType.REPORTED);
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		handleFailure(failure, TestExecutionResult::aborted);
	}

	@Override
	public void testFailure(Failure failure) {
		handleFailure(failure, TestExecutionResult::failed);
	}

	@Override
	public void testFinished(Description description) {
		testFinished(lookupOrRegisterCurrentTestDescriptor(description));
	}

	@Override
	public void testSuiteFinished(Description description) {
		RunnerTestDescriptor runnerTestDescriptor = testRun.getRunnerTestDescriptor();
		// runnerTestDescriptor is reported in testRunFinished
		if (!runnerTestDescriptor.getDescription().equals(description)) {
			reportContainerFinished(lookupOrRegisterCurrentTestDescriptor(description));
		}
	}

	@Override
	public void testRunFinished(Result result) {
		testRunFinished();
	}

	void testRunFinished() {
		reportContainerFinished(testRun.getRunnerTestDescriptor());
	}

	private void reportContainerFinished(TestDescriptor containerTestDescriptor) {
		if (testRun.isNotSkipped(containerTestDescriptor)) {
			if (testRun.isNotStarted(containerTestDescriptor)) {
				fireExecutionStarted(containerTestDescriptor, EventType.SYNTHETIC);
			}
			testRun.getInProgressTestDescriptorsWithSyntheticStartEvents().stream() //
					.filter(this::canFinish) //
					.forEach(this::fireExecutionFinished);
			if (testRun.isNotFinished(containerTestDescriptor)) {
				fireExecutionFinished(containerTestDescriptor);
			}
		}
	}

	private TestDescriptor lookupOrRegisterNextTestDescriptor(Description description) {
		return lookupOrRegisterTestDescriptor(description, testRun::lookupNextTestDescriptor);
	}

	private TestDescriptor lookupOrRegisterCurrentTestDescriptor(Description description) {
		return lookupOrRegisterTestDescriptor(description, testRun::lookupCurrentTestDescriptor);
	}

	private TestDescriptor lookupOrRegisterTestDescriptor(Description description,
			Function<Description, Optional<VintageTestDescriptor>> lookup) {
		return lookup.apply(description).orElseGet(() -> registerDynamicTestDescriptor(description, lookup));
	}

	private VintageTestDescriptor registerDynamicTestDescriptor(Description description,
			Function<Description, Optional<VintageTestDescriptor>> lookup) {
		// workaround for dynamic children as used by Spock's Runner
		TestDescriptor parent = findParent(description, lookup);
		UniqueId uniqueId = parent.getUniqueId().append(SEGMENT_TYPE_DYNAMIC, uniqueIdExtractor.apply(description));
		VintageTestDescriptor dynamicDescriptor = new VintageTestDescriptor(uniqueId, description,
			testSourceProvider.findTestSource(description));
		parent.addChild(dynamicDescriptor);
		testRun.registerDynamicTest(dynamicDescriptor);
		dynamicTestRegistered(dynamicDescriptor);
		return dynamicDescriptor;
	}

	private TestDescriptor findParent(Description description,
			Function<Description, Optional<VintageTestDescriptor>> lookup) {
		// @formatter:off
		return Optional.ofNullable(description.getTestClass())
				.map(Description::createSuiteDescription)
				.flatMap(lookup)
				.orElseGet(testRun::getRunnerTestDescriptor);
		// @formatter:on
	}

	private void handleFailure(Failure failure, Function<Throwable, TestExecutionResult> resultCreator) {
		handleFailure(failure, resultCreator, lookupOrRegisterCurrentTestDescriptor(failure.getDescription()));
	}

	private void handleFailure(Failure failure, Function<Throwable, TestExecutionResult> resultCreator,
			TestDescriptor testDescriptor) {
		TestExecutionResult result = resultCreator.apply(failure.getException());
		testRun.storeResult(testDescriptor, result);
		if (testRun.isNotStarted(testDescriptor)) {
			testStarted(testDescriptor, EventType.SYNTHETIC);
		}
		if (testRun.isNotFinished(testDescriptor) && testDescriptor.isContainer()
				&& testRun.hasSyntheticStartEvent(testDescriptor)
				&& testRun.isDescendantOfRunnerTestDescriptor(testDescriptor)) {
			testFinished(testDescriptor);
		}
	}

	private void testIgnored(TestDescriptor testDescriptor, String reason) {
		fireExecutionFinishedForInProgressNonAncestorTestDescriptorsWithSyntheticStartEvents(testDescriptor);
		fireExecutionStartedIncludingUnstartedAncestors(testDescriptor.getParent());
		fireExecutionSkipped(testDescriptor, reason);
	}

	private Optional<String> determineReasonForIgnoredTest(TestDescriptor testDescriptor, Description description) {
		Optional<String> reason = getReason(description.getAnnotation(Ignore.class));
		if (reason.isPresent()) {
			return reason;
		}
		// Workaround for some runners (e.g. JUnit38ClassRunner) don't include the @Ignore annotation
		// in the description, so we read it from the test class directly
		return testDescriptor.getSource() //
				.filter(ClassSource.class::isInstance) //
				.map(source -> ((ClassSource) source).getJavaClass()) //
				.flatMap(testClass -> getReason(testClass.getAnnotation(Ignore.class)));
	}

	private static Optional<String> getReason(@Nullable Ignore annotation) {
		return Optional.ofNullable(annotation).map(Ignore::value);
	}

	private void dynamicTestRegistered(TestDescriptor testDescriptor) {
		fireExecutionStartedIncludingUnstartedAncestors(testDescriptor.getParent());
		listener.dynamicTestRegistered(testDescriptor);
	}

	private void testStarted(TestDescriptor testDescriptor, EventType eventType) {
		fireExecutionFinishedForInProgressNonAncestorTestDescriptorsWithSyntheticStartEvents(testDescriptor);
		fireExecutionStartedIncludingUnstartedAncestors(testDescriptor.getParent());
		fireExecutionStarted(testDescriptor, eventType);
	}

	private void fireExecutionFinishedForInProgressNonAncestorTestDescriptorsWithSyntheticStartEvents(
			TestDescriptor testDescriptor) {
		testRun.getInProgressTestDescriptorsWithSyntheticStartEvents().stream() //
				.filter(it -> !isAncestor(it, testDescriptor) && canFinish(it)) //
				.forEach(this::fireExecutionFinished);
	}

	private boolean isAncestor(TestDescriptor candidate, TestDescriptor testDescriptor) {
		Optional<TestDescriptor> parent = testDescriptor.getParent();
		if (parent.isEmpty()) {
			return false;
		}
		if (parent.get().equals(candidate)) {
			return true;
		}
		return isAncestor(candidate, parent.get());
	}

	private void testFinished(TestDescriptor descriptor) {
		fireExecutionFinished(descriptor);
	}

	private void fireExecutionStartedIncludingUnstartedAncestors(Optional<TestDescriptor> parent) {
		if (parent.isPresent() && canStart(parent.get())) {
			fireExecutionStartedIncludingUnstartedAncestors(parent.get().getParent());
			fireExecutionStarted(parent.get(), EventType.SYNTHETIC);
		}
	}

	private boolean canStart(TestDescriptor testDescriptor) {
		return testRun.isNotStarted(testDescriptor) //
				&& (testDescriptor.equals(testRun.getRunnerTestDescriptor())
						|| testRun.isDescendantOfRunnerTestDescriptor(testDescriptor));
	}

	private boolean canFinish(TestDescriptor testDescriptor) {
		return testRun.isNotFinished(testDescriptor) //
				&& testRun.isDescendantOfRunnerTestDescriptor(testDescriptor)
				&& testRun.areAllFinishedOrSkipped(testDescriptor.getChildren());
	}

	private void fireExecutionSkipped(TestDescriptor testDescriptor, String reason) {
		testRun.markSkipped(testDescriptor);
		listener.executionSkipped(testDescriptor, reason);
	}

	private void fireExecutionStarted(TestDescriptor testDescriptor, EventType eventType) {
		testRun.markStarted(testDescriptor, eventType);
		listener.executionStarted(testDescriptor);
	}

	private void fireExecutionFinished(TestDescriptor testDescriptor) {
		testRun.markFinished(testDescriptor);
		listener.executionFinished(testDescriptor, testRun.getStoredResultOrSuccessful(testDescriptor));
	}

}
