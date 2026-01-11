/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.includedIf;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.TagFilter.excludeTags;
import static org.junit.platform.launcher.TagFilter.includeTags;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.ENGINE_ID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.internal.runners.SuiteMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.runners.Suite;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.samples.junit3.JUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails;
import org.junit.vintage.engine.samples.junit3.PlainJUnit3TestCaseWithSingleTestWhichFails;
import org.junit.vintage.engine.samples.junit4.Categories;
import org.junit.vintage.engine.samples.junit4.EnclosedJUnit4TestCase;
import org.junit.vintage.engine.samples.junit4.JUnit4SuiteOfSuiteWithFilterableChildRunner;
import org.junit.vintage.engine.samples.junit4.JUnit4SuiteWithTwoTestCases;
import org.junit.vintage.engine.samples.junit4.JUnit4TestCaseWithNotFilterableRunner;
import org.junit.vintage.engine.samples.junit4.NotFilterableRunner;
import org.junit.vintage.engine.samples.junit4.ParameterizedTestCase;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithFiveTestMethods;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithTwoTestMethods;

/**
 * @since 5.1
 */
class VintageLauncherIntegrationTests {

	@Test
	void executesOnlyTaggedMethodOfRegularTestClass() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters(includeTags(Categories.Failing.class.getName()));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(2);

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), "failingTest");
	}

	@Test
	void executesIncludedTaggedMethodOfNestedTestClass() {
		Class<?> testClass = EnclosedJUnit4TestCase.class;
		Class<?> nestedTestClass = EnclosedJUnit4TestCase.NestedClass.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters(includeTags(Categories.Failing.class.getName()));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), nestedTestClass.getName(),
					"failingTest");
	}

	@Test
	void executesOnlyNotExcludedTaggedMethodOfNestedTestClass() {
		Class<?> testClass = EnclosedJUnit4TestCase.class;
		Class<?> nestedTestClass = EnclosedJUnit4TestCase.NestedClass.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Failing.class.getName()));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), nestedTestClass.getName(),
					"successfulTest");
	}

	@Test
	void removesWholeSubtree() {
		Class<?> testClass = EnclosedJUnit4TestCase.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Plain.class.getName()));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage");
	}

	@Test
	void removesCompleteClassIfNoMethodHasMatchingTags() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters(includeTags("wrong-tag"));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	@Test
	void removesCompleteClassIfItHasExcludedTag() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Plain.class.getName()));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	@Test
	void executesAllTestsForNotFilterableRunner(@TrackLogRecords LogRecordListener logRecordListener) {
		Class<?> testClass = JUnit4TestCaseWithNotFilterableRunner.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters((PostDiscoveryFilter) descriptor -> includedIf(descriptor.getDisplayName().contains("#1")));

		var testPlan = discover(request);
		logRecordListener.clear();
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), "Test #0", "Test #1");
		assertThat(logRecordListener.stream(RunnerTestDescriptor.class, Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly(
					"Runner " + NotFilterableRunner.class.getName() + " (used on class " + testClass.getName() + ")" //
							+ " does not support filtering and will therefore be run completely.");
	}

	@Test
	void executesAllTestsForNotFilterableChildRunnerOfSuite(@TrackLogRecords LogRecordListener logRecordListener) {
		Class<?> suiteClass = JUnit4SuiteOfSuiteWithFilterableChildRunner.class;
		Class<?> testClass = JUnit4TestCaseWithNotFilterableRunner.class;
		var request = request() //
				.selectors(selectClass(suiteClass)) //
				.filters((PostDiscoveryFilter) descriptor -> includedIf(descriptor.getDisplayName().contains("#1")));

		var testPlan = discover(request);
		logRecordListener.clear();
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(4);

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", suiteClass.getSimpleName(), testClass.getName(), "Test #0",
					"Test #1");
		assertThat(logRecordListener.stream(RunnerTestDescriptor.class, Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly("Runner " + Suite.class.getName() + " (used on class " + suiteClass.getName() + ")" //
						+ " was not able to satisfy all filter requests.");
	}

	@Test
	void executesAllTestsWhenFilterDidNotExcludeTestForJUnit3Suite(
			@TrackLogRecords LogRecordListener logRecordListener) {
		Class<?> suiteClass = JUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails.class;
		Class<?> testClass = PlainJUnit3TestCaseWithSingleTestWhichFails.class;
		var request = request() //
				.selectors(selectClass(suiteClass)) //
				.filters((PostDiscoveryFilter) descriptor -> excluded("not today"));

		var testPlan = discover(request);
		logRecordListener.clear();
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", suiteClass.getSimpleName(), testClass.getName(), "test");
		assertThat(logRecordListener.stream(RunnerTestDescriptor.class, Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly(
					"Runner " + SuiteMethod.class.getName() + " (used on class " + suiteClass.getName() + ")" //
							+ " was not able to satisfy all filter requests.");
	}

	@Test
	void executesOnlyTaggedMethodsForSuite() {
		Class<?> suiteClass = JUnit4SuiteWithTwoTestCases.class;
		Class<?> testClass = PlainJUnit4TestCaseWithTwoTestMethods.class;
		var request = request() //
				.selectors(selectClass(suiteClass)) //
				.filters(includeTags(Categories.Successful.class.getName()));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", suiteClass.getSimpleName(), testClass.getName(),
					"successfulTest");
	}

	@Test
	void removesCompleteClassWithNotFilterableRunnerIfItHasExcludedTag() {
		Class<?> testClass = JUnit4TestCaseWithNotFilterableRunner.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Successful.class.getName()));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	@Test
	void filtersOutAllDescendantsOfParameterizedTestCase() {
		Class<?> testClass = ParameterizedTestCase.class;
		var request = request() //
				.selectors(selectClass(testClass)) //
				.filters((PostDiscoveryFilter) descriptor -> excluded("excluded"));

		var testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		var results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	private TestPlan discover(LauncherDiscoveryRequestBuilder requestBuilder) {
		var launcher = LauncherFactory.create();
		return launcher.discover(toDiscoveryRequest(requestBuilder).build());
	}

	private Map<TestIdentifier, TestExecutionResult> execute(LauncherDiscoveryRequestBuilder requestBuilder) {
		Map<TestIdentifier, TestExecutionResult> results = new LinkedHashMap<>();
		var launcher = LauncherFactory.create();
		var listener = new TestExecutionListener() {
			@Override
			public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
				results.put(testIdentifier, testExecutionResult);
			}
		};
		var executionRequest = toDiscoveryRequest(requestBuilder) //
				.forExecution() //
				.listeners(listener) //
				.build();
		launcher.execute(executionRequest);
		return results;
	}

	private LauncherDiscoveryRequestBuilder toDiscoveryRequest(LauncherDiscoveryRequestBuilder requestBuilder) {
		return requestBuilder //
				.filters(includeEngines(ENGINE_ID)) //
				.enableImplicitConfigurationParameters(false);
	}

}
