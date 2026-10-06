/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateComparisonProvider;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests for comparison testing with
 * {@link TestTemplateComparisonProvider}.
 *
 * @since 6.2
 */
class ComparisonTestingTests extends AbstractJupiterTestEngineTests {

	@BeforeEach
	void resetRecordedExecutions() {
		ComparisonTestCase.executions.clear();
	}

	@Test
	void eachInvocationIsComparedAcrossSubjects() {
		var results = executeTests(selectMethod(ComparisonTestCase.class, "template", "int, java.lang.String"));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(ComparisonTestCase.class), started()), //
			event(container("template"), started()), //
			event(dynamicTestRegistered("test-template-invocation:#1"), displayName("[1] 1")), //
			event(container("test-template-invocation:#1"), started()), //
			event(dynamicTestRegistered("comparison-subject:#1"), displayName("A")), //
			event(test("comparison-subject:#1"), started()), //
			event(test("comparison-subject:#1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("comparison-subject:#2"), displayName("B")), //
			event(test("comparison-subject:#2"), started()), //
			event(test("comparison-subject:#2"), finishedSuccessfully()), //
			event(container("test-template-invocation:#1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("test-template-invocation:#2"), displayName("[2] 2")), //
			event(container("test-template-invocation:#2"), started()), //
			event(dynamicTestRegistered("comparison-subject:#1"), displayName("A")), //
			event(test("comparison-subject:#1"), started()), //
			event(test("comparison-subject:#1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("comparison-subject:#2"), displayName("B")), //
			event(test("comparison-subject:#2"), started()), //
			event(test("comparison-subject:#2"), finishedSuccessfully()), //
			event(container("test-template-invocation:#2"), finishedSuccessfully()), //
			event(container("template"), finishedSuccessfully()), //
			event(container(ComparisonTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		assertThat(ComparisonTestCase.executions).containsExactly( //
			"beforeEach:A", "template:1:A", "beforeEach:B", "template:1:B", //
			"beforeEach:A", "template:2:A", "beforeEach:B", "template:2:B");
	}

	@Test
	void parentRelationshipIsEstablished() {
		var results = executeTests(selectMethod(ComparisonTestCase.class, "template", "int, java.lang.String"));

		var templateDescriptor = findTestDescriptor(results, container("template"));
		var invocationDescriptor = findTestDescriptor(results, container("test-template-invocation:#1"));
		var subjectDescriptor = findTestDescriptor(results, test("comparison-subject:#1"));

		assertThat(invocationDescriptor.getParent()).hasValue(templateDescriptor);
		assertThat(subjectDescriptor.getParent()).hasValue(invocationDescriptor);
	}

	@Test
	void legacyReportingNamesIncludeInvocationAndSubjectIndex() {
		var results = executeTests(selectMethod(ComparisonTestCase.class, "template", "int, java.lang.String"));

		// @formatter:off
		var legacyReportingNames = results.allEvents().dynamicallyRegistered()
				.map(Event::getTestDescriptor)
				.map(TestDescriptor::getLegacyReportingName);
		// @formatter:on
		assertThat(legacyReportingNames).containsExactly( //
			"template(int, String)[1]", "template(int, String)[1][1]", "template(int, String)[1][2]", //
			"template(int, String)[2]", "template(int, String)[2][1]", "template(int, String)[2][2]");
	}

	@Test
	void subjectsAreExecutedBelowTemplateWithoutInvocationContextProvider() {
		var results = executeTests(
			selectMethod(ComparisonTestCase.class, "templateWithoutInvocationContextProvider", "java.lang.String"));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(ComparisonTestCase.class), started()), //
			event(container("templateWithoutInvocationContextProvider"), started()), //
			event(dynamicTestRegistered("comparison-subject:#1"), displayName("A")), //
			event(test("comparison-subject:#1"), started()), //
			event(test("comparison-subject:#1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("comparison-subject:#2"), displayName("B")), //
			event(test("comparison-subject:#2"), started()), //
			event(test("comparison-subject:#2"), finishedSuccessfully()), //
			event(container("templateWithoutInvocationContextProvider"), finishedSuccessfully()), //
			event(container(ComparisonTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		assertThat(ComparisonTestCase.executions).containsExactly( //
			"beforeEach:A", "templateWithoutInvocationContextProvider:A", //
			"beforeEach:B", "templateWithoutInvocationContextProvider:B");
	}

	@Test
	void unsupportedTemplateIsExecutedWithoutComparison() {
		var results = executeTests(selectMethod(NotComparedTestCase.class, "templateWithoutComparison", "int"));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(NotComparedTestCase.class), started()), //
			event(container("templateWithoutComparison"), started()), //
			event(dynamicTestRegistered("test-template-invocation:#1"), displayName("[1] 1")), //
			event(test("test-template-invocation:#1"), started()), //
			event(test("test-template-invocation:#1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("test-template-invocation:#2"), displayName("[2] 2")), //
			event(test("test-template-invocation:#2"), started()), //
			event(test("test-template-invocation:#2"), finishedSuccessfully()), //
			event(container("templateWithoutComparison"), finishedSuccessfully()), //
			event(container(NotComparedTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void singleSubjectIsExecutedWhenDiscoveredByUniqueId() {
		UniqueId uniqueId = discoverUniqueId(
			selectMethod(ComparisonTestCase.class, "template", "int, java.lang.String")) //
					.append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2") //
					.append(TestTemplateInvocationTestDescriptor.COMPARISON_SUBJECT_SEGMENT_TYPE, "#1");

		var results = executeTests(selectUniqueId(uniqueId));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(ComparisonTestCase.class), started()), //
			event(container("template"), started()), //
			event(dynamicTestRegistered("test-template-invocation:#2"), displayName("[2] 2")), //
			event(container("test-template-invocation:#2"), started()), //
			event(dynamicTestRegistered("comparison-subject:#1"), displayName("A")), //
			event(test("comparison-subject:#1"), started()), //
			event(test("comparison-subject:#1"), finishedSuccessfully()), //
			event(container("test-template-invocation:#2"), finishedSuccessfully()), //
			event(container("template"), finishedSuccessfully()), //
			event(container(ComparisonTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		assertThat(ComparisonTestCase.executions).containsExactly("beforeEach:A", "template:2:A");
	}

	@Test
	void parameterizedTestIsComparedAcrossSubjects() {
		var results = executeTests(
			selectMethod(ComparisonTestCase.class, "parameterizedTest", "int, java.lang.String"));

		results.testEvents().assertStatistics(stats -> stats.started(4).succeeded(4));
		assertThat(ComparisonTestCase.executions).containsExactly( //
			"beforeEach:A", "parameterizedTest:1:A", "beforeEach:B", "parameterizedTest:1:B", //
			"beforeEach:A", "parameterizedTest:2:A", "beforeEach:B", "parameterizedTest:2:B");
	}

	@Test
	void repeatedTestIsComparedAcrossSubjects() {
		var results = executeTests(selectMethod(ComparisonTestCase.class, "repeatedTest",
			"org.junit.jupiter.api.RepetitionInfo, java.lang.String"));

		results.testEvents().assertStatistics(stats -> stats.started(4).succeeded(4));
		assertThat(ComparisonTestCase.executions).containsExactly( //
			"beforeEach:A", "repeatedTest:1:A", "beforeEach:B", "repeatedTest:1:B", //
			"beforeEach:A", "repeatedTest:2:A", "beforeEach:B", "repeatedTest:2:B");
	}

	@Test
	void invocationWithoutSubjectsFails() {
		var results = executeTests(selectMethod(NoSubjectsTestCase.class, "template", "int"));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(NoSubjectsTestCase.class), started()), //
			event(container("template"), started()), //
			event(dynamicTestRegistered("test-template-invocation:#1")), //
			event(container("test-template-invocation:#1"), started()), //
			event(container("test-template-invocation:#1"), finishedWithFailure(message(
				"Provider [NoSubjectsProvider] did not provide any comparison subjects, but was expected to do so."))), //
			event(dynamicTestRegistered("test-template-invocation:#2")), //
			event(container("test-template-invocation:#2"), started()), //
			event(container("test-template-invocation:#2"), finishedWithFailure(message(
				"Provider [NoSubjectsProvider] did not provide any comparison subjects, but was expected to do so."))), //
			event(container("template"), finishedSuccessfully()), //
			event(container(NoSubjectsTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	private UniqueId discoverUniqueId(MethodSelector selector) {
		var descendants = discoverTests(selector).getEngineDescriptor().getDescendants();
		return descendants.stream().reduce((first, second) -> second).orElseThrow().getUniqueId();
	}

	private static TestDescriptor findTestDescriptor(EngineExecutionResults results, Condition<Event> condition) {
		Events events = results.allEvents();
		return events.filter(condition::matches).findFirst().map(Event::getTestDescriptor).orElseThrow();
	}

	// -------------------------------------------------------------------------

	@ExtendWith(TwoSubjectsProvider.class)
	static class ComparisonTestCase {

		static final List<String> executions = new ArrayList<>();

		@BeforeEach
		void beforeEach(String subject) {
			executions.add("beforeEach:" + subject);
		}

		@TestTemplate
		@ExtendWith(TwoInvocationsProvider.class)
		void template(int value, String subject) {
			executions.add("template:" + value + ":" + subject);
		}

		@TestTemplate
		void templateWithoutInvocationContextProvider(String subject) {
			executions.add("templateWithoutInvocationContextProvider:" + subject);
		}

		@ParameterizedTest
		@ValueSource(ints = { 1, 2 })
		void parameterizedTest(int value, String subject) {
			executions.add("parameterizedTest:" + value + ":" + subject);
		}

		@RepeatedTest(2)
		void repeatedTest(RepetitionInfo repetitionInfo, String subject) {
			executions.add("repeatedTest:" + repetitionInfo.getCurrentRepetition() + ":" + subject);
		}
	}

	@ExtendWith(TwoSubjectsProvider.class)
	static class NotComparedTestCase {

		@TestTemplate
		@ExtendWith(TwoInvocationsProvider.class)
		void templateWithoutComparison(int value) {
			assertEquals(value, value);
		}
	}

	@ExtendWith(NoSubjectsProvider.class)
	static class NoSubjectsTestCase {

		@TestTemplate
		@ExtendWith(TwoInvocationsProvider.class)
		void template(int value) {
		}
	}

	/**
	 * Provides two invocations, each resolving {@code int} parameters to its value.
	 */
	static class TwoInvocationsProvider implements TestTemplateInvocationContextProvider {

		@Override
		public boolean supportsTestTemplate(ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
			return Stream.of(1, 2).map(value -> new TestTemplateInvocationContext() {
				@Override
				public String getDisplayName(int invocationIndex) {
					return "[" + invocationIndex + "] " + value;
				}

				@Override
				public List<Extension> getAdditionalExtensions() {
					return List.of(resolver(int.class, value));
				}
			});
		}
	}

	/**
	 * Provides two comparison subjects, each resolving {@code String} parameters to its name.
	 */
	static class TwoSubjectsProvider implements TestTemplateComparisonProvider {

		@Override
		public boolean supportsComparison(ExtensionContext context) {
			return hasParameterOfType(context, String.class);
		}

		@Override
		public Stream<TestTemplateInvocationContext> provideComparisonSubjects(ExtensionContext context) {
			return Stream.of("A", "B").map(name -> new TestTemplateInvocationContext() {
				@Override
				public String getDisplayName(int invocationIndex) {
					return name;
				}

				@Override
				public List<Extension> getAdditionalExtensions() {
					return List.of(resolver(String.class, name));
				}
			});
		}
	}

	static class NoSubjectsProvider implements TestTemplateComparisonProvider {

		@Override
		public boolean supportsComparison(ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<TestTemplateInvocationContext> provideComparisonSubjects(ExtensionContext context) {
			return Stream.empty();
		}
	}

	private static boolean hasParameterOfType(ExtensionContext context, Class<?> type) {
		return context.getTestMethod().map(Method::getParameterTypes).map(Arrays::asList) //
				.filter(types -> types.contains(type)).isPresent();
	}

	private static ParameterResolver resolver(Class<?> type, Object value) {
		return new ParameterResolver() {
			@Override
			public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
				return parameterContext.getParameter().getType() == type;
			}

			@Override
			public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
				return value;
			}
		};
	}

}
