/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.TagFilter.excludeTags;
import static org.junit.platform.launcher.core.OutputDirectoryProviders.hierarchicalOutputDirectoryProvider;
import static org.junit.platform.suite.engine.SuiteEngineDescriptor.ENGINE_ID;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.core.NamespacedHierarchicalStoreProviders;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.engine.testcases.ConfigurationSensitiveTestCase;
import org.junit.platform.suite.engine.testcases.DynamicTestsTestCase;
import org.junit.platform.suite.engine.testcases.ErroneousTestCase;
import org.junit.platform.suite.engine.testcases.JUnit4TestsTestCase;
import org.junit.platform.suite.engine.testcases.MultipleTestsTestCase;
import org.junit.platform.suite.engine.testcases.SingleTestTestCase;
import org.junit.platform.suite.engine.testcases.TaggedTestTestCase;
import org.junit.platform.suite.engine.testsuites.AbstractSuite;
import org.junit.platform.suite.engine.testsuites.ConfigurationSuite;
import org.junit.platform.suite.engine.testsuites.CyclicSuite;
import org.junit.platform.suite.engine.testsuites.DynamicSuite;
import org.junit.platform.suite.engine.testsuites.EmptyCyclicSuite;
import org.junit.platform.suite.engine.testsuites.EmptyDynamicTestSuite;
import org.junit.platform.suite.engine.testsuites.EmptyDynamicTestWithFailIfNoTestFalseSuite;
import org.junit.platform.suite.engine.testsuites.EmptyTestCaseSuite;
import org.junit.platform.suite.engine.testsuites.EmptyTestCaseWithFailIfNoTestFalseSuite;
import org.junit.platform.suite.engine.testsuites.ErroneousTestSuite;
import org.junit.platform.suite.engine.testsuites.InheritedSuite;
import org.junit.platform.suite.engine.testsuites.MultiEngineSuite;
import org.junit.platform.suite.engine.testsuites.MultipleSuite;
import org.junit.platform.suite.engine.testsuites.NestedSuite;
import org.junit.platform.suite.engine.testsuites.SelectByIdentifierSuite;
import org.junit.platform.suite.engine.testsuites.SelectClassesSuite;
import org.junit.platform.suite.engine.testsuites.SelectMethodsSuite;
import org.junit.platform.suite.engine.testsuites.SuiteDisplayNameSuite;
import org.junit.platform.suite.engine.testsuites.SuiteSuite;
import org.junit.platform.suite.engine.testsuites.SuiteWithErroneousTestSuite;
import org.junit.platform.suite.engine.testsuites.ThreePartCyclicSuite;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * @since 1.8
 */
class SuiteEngineTests {

	@TempDir
	private Path outputDir;

	@ParameterizedTest
	@ValueSource(classes = { SelectClassesSuite.class, InheritedSuite.class })
	void selectClasses(Class<?> suiteClass) {
		// @formatter:off
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(suiteClass))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir));

		assertThat(testKit.discover().getDiscoveryIssues())
				.isEmpty();

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(suiteClass.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectMethods() {
		// @formatter:off
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(SelectMethodsSuite.class));

		assertThat(testKit.discover().getDiscoveryIssues())
				.isEmpty();

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(MultipleTestsTestCase.class.getName(), "test()"), finishedSuccessfully()))
				.doNotHave(event(test(MultipleTestsTestCase.class.getName(), "test2()")));
		// @formatter:on
	}

	@Test
	void suiteDisplayName() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(SuiteDisplayNameSuite.class))
				.execute()
				.allEvents()
				.assertThatEvents()
				.haveExactly(1, event(container(displayName("Suite Display Name")), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void abstractSuiteIsNotExecuted() {
		// @formatter:off
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(AbstractSuite.class));

		assertThat(testKit.discover().getDiscoveryIssues())
				.isEmpty();

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Test
	void privateSuiteIsNotExecuted() {
		// @formatter:off
		var message = "@Suite class '%s' must not be private. It will not be executed."
				.formatted(PrivateSuite.class.getName());
		var issue = DiscoveryIssue.builder(Severity.WARNING, message)
				.source(ClassSource.from(PrivateSuite.class))
				.build();
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(PrivateSuite.class));

		assertThat(testKit.discover().getDiscoveryIssues())
				.containsExactly(issue);

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Test
	void abstractPrivateSuiteIsNotExecuted() {
		// @formatter:off
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(AbstractPrivateSuite.class));

		assertThat(testKit.discover().getDiscoveryIssues())
				.isEmpty();

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(classes = { InnerSuite.class, AbstractInnerSuite.class })
	void innerSuiteIsNotExecuted(Class<?> suiteClass) {
		// @formatter:off
		var message = "@Suite class '%s' must not be an inner class. Did you forget to declare it static? It will not be executed."
				.formatted(suiteClass.getName());
		var issue = DiscoveryIssue.builder(Severity.WARNING, message)
				.source(ClassSource.from(suiteClass))
				.build();
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(suiteClass));

		assertThat(testKit.discover().getDiscoveryIssues())
				.containsExactly(issue);

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Test
	void localSuiteIsNotExecuted() {

		@Suite
		@SelectClasses(names = "org.junit.platform.suite.engine.testcases.SingleTestTestCase")
		class LocalSuite {
		}

		// @formatter:off
		var message = "@Suite class '%s' must not be a local class. It will not be executed."
				.formatted(LocalSuite.class.getName());
		var issue = DiscoveryIssue.builder(Severity.WARNING, message)
				.source(ClassSource.from(LocalSuite.class))
				.build();
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(LocalSuite.class));

		assertThat(testKit.discover().getDiscoveryIssues())
				.containsExactly(issue);

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Test
	void anonymousSuiteIsNotExecuted() {
		var object = new Object() {
		};

		// @formatter:off
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(object.getClass()));

		assertThat(testKit.discover().getDiscoveryIssues())
				.isEmpty();

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Test
	void nestedSuiteIsNotExecuted() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(NestedSuite.class))
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Test
	void dynamicSuite() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(DynamicSuite.class))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(2, event(test(DynamicTestsTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void suiteSuite() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(SuiteSuite.class))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(SuiteSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(SelectClassesSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectClassesByUniqueId() {
		// @formatter:off
		UniqueId uniqId = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, SelectClassesSuite.class.getName());
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectUniqueId(uniqId))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(SelectClassesSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectMethodInTestPlanByUniqueId() {
		// @formatter:off
		UniqueId uniqueId = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, MultipleSuite.class.getName())
				.append("engine", JupiterEngineDescriptor.ENGINE_ID)
				.append(ClassTestDescriptor.SEGMENT_TYPE, MultipleTestsTestCase.class.getName())
				.append(TestMethodTestDescriptor.SEGMENT_TYPE, "test()");

		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectUniqueId(uniqueId))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(MultipleSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(MultipleTestsTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectSuiteByUniqueId() {
		// @formatter:off
		UniqueId uniqueId = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, MultipleSuite.class.getName());

		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectUniqueId(uniqueId))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(2, event(test(MultipleSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(2, event(test(MultipleTestsTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectMethodAndSuiteInTestPlanByUniqueId() {
		// @formatter:off
		UniqueId uniqueId = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, MultipleSuite.class.getName())
				.append("engine", JupiterEngineDescriptor.ENGINE_ID)
				.append(ClassTestDescriptor.SEGMENT_TYPE, MultipleTestsTestCase.class.getName())
				.append(TestMethodTestDescriptor.SEGMENT_TYPE, "test()");

		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectUniqueId(uniqueId))
				.selectors(selectClass(SelectClassesSuite.class))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(SelectClassesSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(MultipleSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(MultipleTestsTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectMethodsInTestPlanByUniqueId() {
		// @formatter:off
		UniqueId uniqueId = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, MultipleSuite.class.getName())
				.append("engine", JupiterEngineDescriptor.ENGINE_ID)
				.append(ClassTestDescriptor.SEGMENT_TYPE, MultipleTestsTestCase.class.getName())
				.append(TestMethodTestDescriptor.SEGMENT_TYPE, "test()");

		UniqueId uniqueId2 = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, MultipleSuite.class.getName())
				.append("engine", JupiterEngineDescriptor.ENGINE_ID)
				.append(ClassTestDescriptor.SEGMENT_TYPE, MultipleTestsTestCase.class.getName())
				.append(TestMethodTestDescriptor.SEGMENT_TYPE, "test2()");

		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectUniqueId(uniqueId))
				.selectors(selectUniqueId(uniqueId2))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(2, event(test(MultipleSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(2, event(test(MultipleTestsTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectConfigurationSensitiveMethodsInTestPlanByUniqueId() {
		// @formatter:off
		var uniqueId1 = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, ConfigurationSuite.class.getName())
				.append("engine", JupiterEngineDescriptor.ENGINE_ID)
				.append(ClassTestDescriptor.SEGMENT_TYPE, ConfigurationSensitiveTestCase.class.getName())
				.append(TestMethodTestDescriptor.SEGMENT_TYPE, "test1()");

		var uniqueId2 = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, ConfigurationSuite.class.getName())
				.append("engine", JupiterEngineDescriptor.ENGINE_ID)
				.append(ClassTestDescriptor.SEGMENT_TYPE, ConfigurationSensitiveTestCase.class.getName())
				.append(TestMethodTestDescriptor.SEGMENT_TYPE, "test2()");

		EngineTestKit.engine(ENGINE_ID)
				.selectors(
					selectUniqueId(uniqueId1),
					selectUniqueId(uniqueId2)
				)
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(ConfigurationSuite.class.getName(), "test1()"), finishedSuccessfully()))
				.haveExactly(1, event(test(ConfigurationSuite.class.getName(), "test2()"), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void postDiscoveryCanRemoveTestDescriptorsInSuite() {
		// @formatter:off
		PostDiscoveryFilter postDiscoveryFilter = testDescriptor -> testDescriptor.getSource()
				.filter(MethodSource.class::isInstance)
				.map(MethodSource.class::cast)
				.filter(classSource -> SingleTestTestCase.class.equals(classSource.getJavaClass()))
				.map(classSource -> FilterResult.excluded("Was a test in SimpleTest"))
				.orElseGet(() -> FilterResult.included("Was not a test in SimpleTest"));

		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(SelectClassesSuite.class))
				.filters(postDiscoveryFilter)
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Test
	void emptySuiteFails() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(EmptyTestCaseSuite.class))
				.execute()
				.containerEvents()
				.assertThatEvents()
				.haveExactly(1, event(container(EmptyTestCaseSuite.class), finishedWithFailure(instanceOf(NoTestsDiscoveredException.class))));
		// @formatter:on
	}

	@Test
	void emptySuitePassesWhenFailIfNoTestIsFalse() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(EmptyTestCaseWithFailIfNoTestFalseSuite.class))
				.execute()
				.containerEvents()
				.assertThatEvents()
				.haveExactly(1, event(engine(), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void emptyDynamicSuiteFails() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(EmptyDynamicTestSuite.class))
				.execute()
				.containerEvents()
				.assertThatEvents()
				.haveExactly(1, event(container(EmptyDynamicTestSuite.class), finishedWithFailure(instanceOf(NoTestsDiscoveredException.class))));
		// @formatter:on
	}

	@Test
	void emptyDynamicSuitePassesWhenFailIfNoTestIsFalse() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(EmptyDynamicTestWithFailIfNoTestFalseSuite.class))
				.execute()
				.allEvents()
				.assertThatEvents()
				.haveAtLeastOne(event(container(EmptyDynamicTestWithFailIfNoTestFalseSuite.class), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void pruneAfterPostDiscoveryFilters() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(MultiEngineSuite.class))
				.filters(excludeTags("excluded"))
				.execute()
				.allEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(JUnit4TestsTestCase.class.getName()), finishedSuccessfully()))
				.doNotHave(test(TaggedTestTestCase.class.getName()))
				.doNotHave(container("junit-jupiter"));
		// @formatter:on
	}

	@Test
	void cyclicSuite() {
		// @formatter:off
		var expectedUniqueId = UniqueId.forEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, CyclicSuite.class.getName())
				.appendEngine(ENGINE_ID)
				.append(SuiteTestDescriptor.SEGMENT_TYPE, CyclicSuite.class.getName());
		var message = "The suite configuration of [%s] resulted in a cycle [%s] and will not be discovered a second time."
				.formatted(CyclicSuite.class.getName(), expectedUniqueId);
		var issue = DiscoveryIssue.builder(Severity.INFO, message)
				.source(ClassSource.from(CyclicSuite.class))
				.build();

		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(CyclicSuite.class))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir));

		assertThat(testKit.discover().getDiscoveryIssues())
				.containsExactly(issue);

		testKit
				.execute()
				.allEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void emptyCyclicSuite() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(EmptyCyclicSuite.class))
				.execute()
				.allEvents()
				.assertThatEvents()
				.haveExactly(1, event(container(EmptyCyclicSuite.class), finishedWithFailure(message(
						"Suite [org.junit.platform.suite.engine.testsuites.EmptyCyclicSuite] did not discover any tests"
				))));
		// @formatter:on
	}

	@Test
	void threePartCyclicSuite() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(ThreePartCyclicSuite.PartA.class))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir))
				.execute()
				.allEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void selectByIdentifier() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(SelectByIdentifierSuite.class))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(SelectByIdentifierSuite.class.getName()), finishedSuccessfully()))
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on
	}

	@Test
	void passesOutputDirectoryProviderToEnginesInSuite() {
		// @formatter:off
		EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(SelectClassesSuite.class))
				.outputDirectoryProvider(hierarchicalOutputDirectoryProvider(outputDir))
				.execute()
				.testEvents()
				.assertThatEvents()
				.haveExactly(1, event(test(SingleTestTestCase.class.getName()), finishedSuccessfully()));
		// @formatter:on

		assertThat(outputDir).isDirectoryRecursivelyContaining("glob:**/test.txt");
	}

	@Test
	void discoveryIssueOfNestedTestEnginesAreReported() throws Exception {
		// @formatter:off
		var testKit = EngineTestKit.engine(ENGINE_ID)
				.selectors(selectClass(SuiteWithErroneousTestSuite.class));

		var discoveryIssues = testKit.discover().getDiscoveryIssues();
		assertThat(discoveryIssues).hasSize(1);

		var issue = discoveryIssues.getFirst();
		assertThat(issue.message()) //
				.startsWith("[junit-jupiter] @BeforeAll method") //
				.endsWith(" (via @Suite %s > %s).".formatted(SuiteWithErroneousTestSuite.class.getName(),
						ErroneousTestSuite.class.getName()));

		var method = ErroneousTestCase.class.getDeclaredMethod("nonStaticLifecycleMethod");
		assertThat(issue.source()).contains(MethodSource.from(method));

		testKit
				.execute()
				.testEvents()
				.assertThatEvents()
				.isEmpty();
		// @formatter:on
	}

	@Suite
	@SelectClasses(SingleTestTestCase.class)
	abstract private static class AbstractPrivateSuite {
	}

	@Test
	void suiteEnginePassesRequestLevelStoreToSuiteTestDescriptors() {
		UniqueId engineId = UniqueId.forEngine(SuiteEngineDescriptor.ENGINE_ID);
		SuiteEngineDescriptor engineDescriptor = new SuiteEngineDescriptor(engineId);

		SuiteTestDescriptor mockDescriptor = mock(SuiteTestDescriptor.class);
		engineDescriptor.addChild(mockDescriptor);

		EngineExecutionListener listener = mock(EngineExecutionListener.class);
		NamespacedHierarchicalStore<Namespace> requestLevelStore = NamespacedHierarchicalStoreProviders.dummyNamespacedHierarchicalStore();
		CancellationToken cancellationToken = CancellationToken.create();

		ExecutionRequest request = ExecutionRequest.create(engineDescriptor, listener,
			mock(ConfigurationParameters.class), mock(OutputDirectoryProvider.class), requestLevelStore,
			cancellationToken);

		new SuiteTestEngine().execute(request);

		verify(mockDescriptor).execute(same(listener), same(requestLevelStore), same(cancellationToken));
	}

	@Suite
	@SelectClasses(SingleTestTestCase.class)
	private static class PrivateSuite {
	}

	@Suite
	@SelectClasses(names = "org.junit.platform.suite.engine.testcases.SingleTestTestCase")
	abstract class AbstractInnerSuite {
	}

	@Suite
	@SelectClasses(names = "org.junit.platform.suite.engine.testcases.SingleTestTestCase")
	class InnerSuite {
	}

}
