/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectIteration;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.TagFilter.excludeTags;
import static org.junit.platform.launcher.TagFilter.includeTags;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.legacyReportingName;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.EventConditions.uniqueId;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.suppressed;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.engine.descriptor.ClassTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTemplateTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/**
 * @since 5.13
 */
public class ClassTemplateInvocationTests extends AbstractJupiterTestEngineTests {

	@ParameterizedTest
	@ValueSource(strings = { //
			"class:%s", //
			"uid:[engine:junit-jupiter]/[class-template:%s]" //
	})
	void executesClassTemplateClassTwice(String selectorIdentifierTemplate) {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoInvocationsTestCase.class.getName());
		var invocationId1 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var invocation1MethodAId = invocationId1.append(TestMethodTestDescriptor.SEGMENT_TYPE, "a()");
		var invocation1NestedClassId = invocationId1.append(NestedClassTestDescriptor.SEGMENT_TYPE, "NestedTestCase");
		var invocation1NestedMethodBId = invocation1NestedClassId.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var invocation2MethodAId = invocationId2.append(TestMethodTestDescriptor.SEGMENT_TYPE, "a()");
		var invocation2NestedClassId = invocationId2.append(NestedClassTestDescriptor.SEGMENT_TYPE, "NestedTestCase");
		var invocation2NestedMethodBId = invocation2NestedClassId.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");

		var results = executeTests(DiscoverySelectors.parse(
			selectorIdentifierTemplate.formatted(TwoInvocationsTestCase.class.getName())).orElseThrow());

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId1)), displayName("[1] A of TwoInvocationsTestCase"),
				legacyReportingName("%s[1]".formatted(TwoInvocationsTestCase.class.getName()))), //
			event(container(uniqueId(invocationId1)), started()), //
			event(dynamicTestRegistered(uniqueId(invocation1MethodAId))), //
			event(dynamicTestRegistered(uniqueId(invocation1NestedClassId))), //
			event(dynamicTestRegistered(uniqueId(invocation1NestedMethodBId))), //
			event(test(uniqueId(invocation1MethodAId)), started()), //
			event(test(uniqueId(invocation1MethodAId)), finishedSuccessfully()), //
			event(container(uniqueId(invocation1NestedClassId)), started()), //
			event(test(uniqueId(invocation1NestedMethodBId)), started()), //
			event(test(uniqueId(invocation1NestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(invocation1NestedClassId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId1)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)), displayName("[2] B of TwoInvocationsTestCase"),
				legacyReportingName("%s[2]".formatted(TwoInvocationsTestCase.class.getName()))), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(invocation2MethodAId))), //
			event(dynamicTestRegistered(uniqueId(invocation2NestedClassId))), //
			event(dynamicTestRegistered(uniqueId(invocation2NestedMethodBId))), //
			event(test(uniqueId(invocation2MethodAId)), started()), //
			event(test(uniqueId(invocation2MethodAId)), finishedSuccessfully()), //
			event(container(uniqueId(invocation2NestedClassId)), started()), //
			event(test(uniqueId(invocation2NestedMethodBId)), started()), //
			event(test(uniqueId(invocation2NestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(invocation2NestedClassId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void classTemplateAnnotationIsInherited() {
		var results = executeTestsForClass(InheritedTwoInvocationsTestCase.class);

		results.allEvents().assertStatistics(stats -> stats.started(12).succeeded(12));
	}

	@Test
	void executesOnlySelectedMethodsDeclaredInClassTemplate() {
		var results = executeTests(selectMethod(TwoInvocationsTestCase.class, "a"));

		results.testEvents() //
				.assertStatistics(stats -> stats.started(2).succeeded(2)) //
				.assertEventsMatchLoosely(event(test(displayName("a()")), finishedSuccessfully()));
	}

	@Test
	void executesOnlySelectedMethodsDeclaredInNestedClassOfClassTemplate() {
		var results = executeTests(selectNestedMethod(List.of(TwoInvocationsTestCase.class),
			TwoInvocationsTestCase.NestedTestCase.class, "b"));

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2)) //
				.assertEventsMatchLoosely(event(test(displayName("b()")), finishedSuccessfully()));
	}

	@Test
	void executesOnlyTestsPassingPostDiscoveryFilter() {
		var results = executeTests(request -> request //
				.selectors(selectClass(TwoInvocationsTestCase.class)) //
				.filters(includeTags("nested")));

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2)) //
				.assertEventsMatchLoosely(event(test(displayName("b()")), finishedSuccessfully()));
	}

	@Test
	void prunesEmptyNestedTestClasses() {
		var results = executeTests(request -> request //
				.selectors(selectClass(TwoInvocationsTestCase.class)) //
				.filters(excludeTags("nested")));

		results.containerEvents().assertThatEvents() //
				.noneMatch(container(TwoInvocationsTestCase.NestedTestCase.class.getSimpleName())::matches);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2)) //
				.assertEventsMatchLoosely(event(test(displayName("a()")), finishedSuccessfully()));
	}

	@Test
	void executesNestedClassTemplateClassTwiceWithClassSelectorForEnclosingClass() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classId = engineId.append(ClassTestDescriptor.SEGMENT_TYPE,
			NestedClassTemplateWithTwoInvocationsTestCase.class.getName());
		var methodAId = classId.append(TestMethodTestDescriptor.SEGMENT_TYPE, "a()");
		var nestedClassTemplateId = classId.append(ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE,
			"NestedTestCase");
		var invocationId1 = nestedClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var invocation1NestedMethodBId = invocationId1.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");
		var invocationId2 = nestedClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var invocation2NestedMethodBId = invocationId2.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");

		var results = executeTestsForClass(NestedClassTemplateWithTwoInvocationsTestCase.class);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classId)), started()), //

			event(test(uniqueId(methodAId)), started()), //
			event(test(uniqueId(methodAId)), finishedSuccessfully()), //

			event(container(uniqueId(nestedClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId1)), displayName("[1] A of NestedTestCase"),
				legacyReportingName(
					"%s[1]".formatted(NestedClassTemplateWithTwoInvocationsTestCase.NestedTestCase.class.getName()))), //
			event(container(uniqueId(invocationId1)), started()), //
			event(dynamicTestRegistered(uniqueId(invocation1NestedMethodBId))), //
			event(test(uniqueId(invocation1NestedMethodBId)), started()), //
			event(test(uniqueId(invocation1NestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId1)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)), displayName("[2] B of NestedTestCase"),
				legacyReportingName(
					"%s[2]".formatted(NestedClassTemplateWithTwoInvocationsTestCase.NestedTestCase.class.getName()))), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(invocation2NestedMethodBId))), //
			event(test(uniqueId(invocation2NestedMethodBId)), started()), //
			event(test(uniqueId(invocation2NestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(nestedClassTemplateId)), finishedSuccessfully()), //

			event(container(uniqueId(classId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void executesNestedClassTemplateClassTwiceWithNestedClassSelector() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classId = engineId.append(ClassTestDescriptor.SEGMENT_TYPE,
			NestedClassTemplateWithTwoInvocationsTestCase.class.getName());
		var nestedClassTemplateId = classId.append(ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE,
			"NestedTestCase");
		var invocationId1 = nestedClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var invocation1NestedMethodBId = invocationId1.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");
		var invocationId2 = nestedClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var invocation2NestedMethodBId = invocationId2.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");

		var results = executeTestsForClass(NestedClassTemplateWithTwoInvocationsTestCase.NestedTestCase.class);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classId)), started()), //

			event(container(uniqueId(nestedClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId1)), displayName("[1] A of NestedTestCase")), //
			event(container(uniqueId(invocationId1)), started()), //
			event(dynamicTestRegistered(uniqueId(invocation1NestedMethodBId))), //
			event(test(uniqueId(invocation1NestedMethodBId)), started()), //
			event(test(uniqueId(invocation1NestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId1)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)), displayName("[2] B of NestedTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(invocation2NestedMethodBId))), //
			event(test(uniqueId(invocation2NestedMethodBId)), started()), //
			event(test(uniqueId(invocation2NestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(nestedClassTemplateId)), finishedSuccessfully()), //

			event(container(uniqueId(classId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void executesNestedClassTemplatesTwiceEach() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var outerClassTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoTimesTwoInvocationsTestCase.class.getName());

		var outerInvocation1Id = outerClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var outerInvocation1NestedClassTemplateId = outerInvocation1Id.append(
			ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE, "NestedTestCase");
		var outerInvocation1InnerInvocation1Id = outerInvocation1NestedClassTemplateId.append(
			ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var outerInvocation1InnerInvocation1NestedMethodId = outerInvocation1InnerInvocation1Id.append(
			TestMethodTestDescriptor.SEGMENT_TYPE, "test()");
		var outerInvocation1InnerInvocation2Id = outerInvocation1NestedClassTemplateId.append(
			ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation1InnerInvocation2NestedMethodId = outerInvocation1InnerInvocation2Id.append(
			TestMethodTestDescriptor.SEGMENT_TYPE, "test()");

		var outerInvocation2Id = outerClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation2NestedClassTemplateId = outerInvocation2Id.append(
			ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE, "NestedTestCase");
		var outerInvocation2InnerInvocation1Id = outerInvocation2NestedClassTemplateId.append(
			ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var outerInvocation2InnerInvocation1NestedMethodId = outerInvocation2InnerInvocation1Id.append(
			TestMethodTestDescriptor.SEGMENT_TYPE, "test()");
		var outerInvocation2InnerInvocation2Id = outerInvocation2NestedClassTemplateId.append(
			ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation2InnerInvocation2NestedMethodId = outerInvocation2InnerInvocation2Id.append(
			TestMethodTestDescriptor.SEGMENT_TYPE, "test()");

		var results = executeTestsForClass(TwoTimesTwoInvocationsTestCase.class);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(outerClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation1Id)),
				displayName("[1] A of TwoTimesTwoInvocationsTestCase")), //
			event(container(uniqueId(outerInvocation1Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation1NestedClassTemplateId))), //
			event(container(uniqueId(outerInvocation1NestedClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation1InnerInvocation1Id)),
				displayName("[1] A of NestedTestCase")), //
			event(container(uniqueId(outerInvocation1InnerInvocation1Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation1InnerInvocation1NestedMethodId))), //
			event(test(uniqueId(outerInvocation1InnerInvocation1NestedMethodId)), started()), //
			event(test(uniqueId(outerInvocation1InnerInvocation1NestedMethodId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation1InnerInvocation1Id)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation1InnerInvocation2Id)),
				displayName("[2] B of NestedTestCase")), //
			event(container(uniqueId(outerInvocation1InnerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation1InnerInvocation2NestedMethodId))), //
			event(test(uniqueId(outerInvocation1InnerInvocation2NestedMethodId)), started()), //
			event(test(uniqueId(outerInvocation1InnerInvocation2NestedMethodId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation1InnerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerInvocation1NestedClassTemplateId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation1Id)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation2Id)),
				displayName("[2] B of TwoTimesTwoInvocationsTestCase")), //
			event(container(uniqueId(outerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation2NestedClassTemplateId))), //
			event(container(uniqueId(outerInvocation2NestedClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation1Id)),
				displayName("[1] A of NestedTestCase")), //
			event(container(uniqueId(outerInvocation2InnerInvocation1Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation1NestedMethodId))), //
			event(test(uniqueId(outerInvocation2InnerInvocation1NestedMethodId)), started()), //
			event(test(uniqueId(outerInvocation2InnerInvocation1NestedMethodId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation2InnerInvocation1Id)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation2Id)),
				displayName("[2] B of NestedTestCase")), //
			event(container(uniqueId(outerInvocation2InnerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation2NestedMethodId))), //
			event(test(uniqueId(outerInvocation2InnerInvocation2NestedMethodId)), started()), //
			event(test(uniqueId(outerInvocation2InnerInvocation2NestedMethodId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation2InnerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerInvocation2NestedClassTemplateId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerClassTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void invocationContextProviderCanRegisterAdditionalExtensions() {
		var results = executeTestsForClass(AdditionalExtensionRegistrationTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
	}

	@Test
	void eachInvocationHasSeparateExtensionContext() {
		var results = executeTestsForClass(SeparateExtensionContextTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
	}

	@Test
	void supportsTestTemplateMethodsInsideClassTemplateClasses() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			CombinationWithTestTemplateTestCase.class.getName());
		var invocationId1 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var testTemplateId1 = invocationId1.append(TestTemplateTestDescriptor.SEGMENT_TYPE, "test(int)");
		var testTemplate1InvocationId1 = testTemplateId1.append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE,
			"#1");
		var testTemplate1InvocationId2 = testTemplateId1.append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE,
			"#2");
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var testTemplateId2 = invocationId2.append(TestTemplateTestDescriptor.SEGMENT_TYPE, "test(int)");
		var testTemplate2InvocationId1 = testTemplateId2.append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE,
			"#1");
		var testTemplate2InvocationId2 = testTemplateId2.append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE,
			"#2");

		var results = executeTestsForClass(CombinationWithTestTemplateTestCase.class);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId1)),
				displayName("[1] A of CombinationWithTestTemplateTestCase")), //
			event(container(uniqueId(invocationId1)), started()), //
			event(dynamicTestRegistered(uniqueId(testTemplateId1))), //
			event(container(uniqueId(testTemplateId1)), started()), //
			event(dynamicTestRegistered(uniqueId(testTemplate1InvocationId1))), //
			event(test(uniqueId(testTemplate1InvocationId1)), started()), //
			event(test(uniqueId(testTemplate1InvocationId1)), finishedSuccessfully()), //
			event(dynamicTestRegistered(uniqueId(testTemplate1InvocationId2))), //
			event(test(uniqueId(testTemplate1InvocationId2)), started()), //
			event(test(uniqueId(testTemplate1InvocationId2)), finishedSuccessfully()), //
			event(container(uniqueId(testTemplateId1)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId1)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)),
				displayName("[2] B of CombinationWithTestTemplateTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testTemplateId2))), //
			event(container(uniqueId(testTemplateId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testTemplate2InvocationId1))), //
			event(test(uniqueId(testTemplate2InvocationId1)), started()), //
			event(test(uniqueId(testTemplate2InvocationId1)), finishedSuccessfully()), //
			event(dynamicTestRegistered(uniqueId(testTemplate2InvocationId2))), //
			event(test(uniqueId(testTemplate2InvocationId2)), started()), //
			event(test(uniqueId(testTemplate2InvocationId2)), finishedSuccessfully()), //
			event(container(uniqueId(testTemplateId2)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void testTemplateInvocationInsideClassTemplateClassCanBeSelectedByUniqueId() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			CombinationWithTestTemplateTestCase.class.getName());
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var testTemplateId2 = invocationId2.append(TestTemplateTestDescriptor.SEGMENT_TYPE, "test(int)");
		var testTemplate2InvocationId2 = testTemplateId2.append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE,
			"#2");

		var results = executeTests(selectUniqueId(testTemplate2InvocationId2));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)),
				displayName("[2] B of CombinationWithTestTemplateTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testTemplateId2))), //
			event(container(uniqueId(testTemplateId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testTemplate2InvocationId2))), //
			event(test(uniqueId(testTemplate2InvocationId2)), started()), //
			event(test(uniqueId(testTemplate2InvocationId2)), finishedSuccessfully()), //
			event(container(uniqueId(testTemplateId2)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void supportsTestFactoryMethodsInsideClassTemplateClasses() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			CombinationWithTestFactoryTestCase.class.getName());
		var invocationId1 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var testFactoryId1 = invocationId1.append(TestFactoryTestDescriptor.SEGMENT_TYPE, "test()");
		var testFactory1DynamicTestId1 = testFactoryId1.append(TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE,
			"#1");
		var testFactory1DynamicTestId2 = testFactoryId1.append(TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE,
			"#2");
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var testFactoryId2 = invocationId2.append(TestFactoryTestDescriptor.SEGMENT_TYPE, "test()");
		var testFactory2DynamicTestId1 = testFactoryId2.append(TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE,
			"#1");
		var testFactory2DynamicTestId2 = testFactoryId2.append(TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE,
			"#2");

		var results = executeTestsForClass(CombinationWithTestFactoryTestCase.class);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId1)),
				displayName("[1] A of CombinationWithTestFactoryTestCase")), //
			event(container(uniqueId(invocationId1)), started()), //
			event(dynamicTestRegistered(uniqueId(testFactoryId1))), //
			event(container(uniqueId(testFactoryId1)), started()), //
			event(dynamicTestRegistered(uniqueId(testFactory1DynamicTestId1))), //
			event(test(uniqueId(testFactory1DynamicTestId1)), started()), //
			event(test(uniqueId(testFactory1DynamicTestId1)), finishedSuccessfully()), //
			event(dynamicTestRegistered(uniqueId(testFactory1DynamicTestId2))), //
			event(test(uniqueId(testFactory1DynamicTestId2)), started()), //
			event(test(uniqueId(testFactory1DynamicTestId2)), finishedSuccessfully()), //
			event(container(uniqueId(testFactoryId1)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId1)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)),
				displayName("[2] B of CombinationWithTestFactoryTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testFactoryId2))), //
			event(container(uniqueId(testFactoryId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testFactory2DynamicTestId1))), //
			event(test(uniqueId(testFactory2DynamicTestId1)), started()), //
			event(test(uniqueId(testFactory2DynamicTestId1)), finishedSuccessfully()), //
			event(dynamicTestRegistered(uniqueId(testFactory2DynamicTestId2))), //
			event(test(uniqueId(testFactory2DynamicTestId2)), started()), //
			event(test(uniqueId(testFactory2DynamicTestId2)), finishedSuccessfully()), //
			event(container(uniqueId(testFactoryId2)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void specificDynamicTestInsideClassTemplateClassCanBeSelectedByUniqueId() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			CombinationWithTestFactoryTestCase.class.getName());
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var testFactoryId2 = invocationId2.append(TestFactoryTestDescriptor.SEGMENT_TYPE, "test()");
		var testFactory2DynamicTestId2 = testFactoryId2.append(TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE,
			"#2");

		var results = executeTests(selectUniqueId(testFactory2DynamicTestId2));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)),
				displayName("[2] B of CombinationWithTestFactoryTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testFactoryId2))), //
			event(container(uniqueId(testFactoryId2)), started()), //
			event(dynamicTestRegistered(uniqueId(testFactory2DynamicTestId2))), //
			event(test(uniqueId(testFactory2DynamicTestId2)), started()), //
			event(test(uniqueId(testFactory2DynamicTestId2)), finishedSuccessfully()), //
			event(container(uniqueId(testFactoryId2)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void failsIfProviderReturnsZeroInvocationContextWithoutOptIn() {
		var results = executeTestsForClass(InvalidZeroInvocationTestCase.class);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(InvalidZeroInvocationTestCase.class), started()), //
			event(container(InvalidZeroInvocationTestCase.class),
				finishedWithFailure(
					message("Provider [Ext] did not provide any invocation contexts, but was expected to do so. "
							+ "You may override mayReturnZeroClassTemplateInvocationContexts() to allow this."))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void succeedsIfProviderReturnsZeroInvocationContextWithOptIn() {
		var results = executeTestsForClass(ValidZeroInvocationTestCase.class);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(ValidZeroInvocationTestCase.class), started()), //
			event(container(ValidZeroInvocationTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@ParameterizedTest
	@ValueSource(classes = { NoProviderRegisteredTestCase.class, NoSupportingProviderRegisteredTestCase.class })
	void failsIfNoSupportingProviderIsRegistered(Class<?> testClass) {
		var results = executeTestsForClass(testClass);

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(
					message("You must register at least one ClassTemplateInvocationContextProvider that supports "
							+ "@ClassTemplate class [" + testClass.getName() + "]"))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void classTemplateInvocationCanBeSelectedByUniqueId() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoInvocationsTestCase.class.getName());
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var methodAId = invocationId2.append(TestMethodTestDescriptor.SEGMENT_TYPE, "a()");
		var nestedClassId = invocationId2.append(NestedClassTestDescriptor.SEGMENT_TYPE, "NestedTestCase");
		var nestedMethodBId = nestedClassId.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");

		var results = executeTests(selectUniqueId(invocationId2));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)), displayName("[2] B of TwoInvocationsTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(methodAId))), //
			event(dynamicTestRegistered(uniqueId(nestedClassId))), //
			event(dynamicTestRegistered(uniqueId(nestedMethodBId))), //
			event(test(uniqueId(methodAId)), started()), //
			event(test(uniqueId(methodAId)), finishedSuccessfully()), //
			event(container(uniqueId(nestedClassId)), started()), //
			event(test(uniqueId(nestedMethodBId)), started()), //
			event(test(uniqueId(nestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(nestedClassId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void classTemplateInvocationCanBeSelectedByIteration() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoInvocationsTestCase.class.getName());
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var methodAId = invocationId2.append(TestMethodTestDescriptor.SEGMENT_TYPE, "a()");
		var nestedClassId = invocationId2.append(NestedClassTestDescriptor.SEGMENT_TYPE, "NestedTestCase");
		var nestedMethodBId = nestedClassId.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");

		var results = executeTests(selectIteration(selectClass(TwoInvocationsTestCase.class), 1));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)), displayName("[2] B of TwoInvocationsTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(methodAId))), //
			event(dynamicTestRegistered(uniqueId(nestedClassId))), //
			event(dynamicTestRegistered(uniqueId(nestedMethodBId))), //
			event(test(uniqueId(methodAId)), started()), //
			event(test(uniqueId(methodAId)), finishedSuccessfully()), //
			event(container(uniqueId(nestedClassId)), started()), //
			event(test(uniqueId(nestedMethodBId)), started()), //
			event(test(uniqueId(nestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(nestedClassId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@ParameterizedTest
	@ValueSource(strings = { //
			"class:org.junit.jupiter.engine.ClassTemplateInvocationTests$TwoInvocationsTestCase", //
			"uid:[engine:junit-jupiter]/[class-template:org.junit.jupiter.engine.ClassTemplateInvocationTests$TwoInvocationsTestCase]" //
	})
	void executesAllInvocationsForRedundantSelectors(String classTemplateSelectorIdentifier) {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoInvocationsTestCase.class.getName());
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");

		var results = executeTests(selectUniqueId(invocationId2),
			DiscoverySelectors.parse(classTemplateSelectorIdentifier).orElseThrow());

		results.testEvents().assertStatistics(stats -> stats.started(4).succeeded(4));
	}

	@Test
	void methodInClassTemplateInvocationCanBeSelectedByUniqueId() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoInvocationsTestCase.class.getName());
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var methodAId = invocationId2.append(TestMethodTestDescriptor.SEGMENT_TYPE, "a()");

		var results = executeTests(selectUniqueId(methodAId));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)), displayName("[2] B of TwoInvocationsTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(methodAId))), //
			event(test(uniqueId(methodAId)), started()), //
			event(test(uniqueId(methodAId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nestedMethodInClassTemplateInvocationCanBeSelectedByUniqueId() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var classTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoInvocationsTestCase.class.getName());
		var invocationId2 = classTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var nestedClassId = invocationId2.append(NestedClassTestDescriptor.SEGMENT_TYPE, "NestedTestCase");
		var nestedMethodBId = nestedClassId.append(TestMethodTestDescriptor.SEGMENT_TYPE, "b()");

		var results = executeTests(selectUniqueId(nestedMethodBId));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(classTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(invocationId2)), displayName("[2] B of TwoInvocationsTestCase")), //
			event(container(uniqueId(invocationId2)), started()), //
			event(dynamicTestRegistered(uniqueId(nestedClassId))), //
			event(dynamicTestRegistered(uniqueId(nestedMethodBId))), //
			event(container(uniqueId(nestedClassId)), started()), //
			event(test(uniqueId(nestedMethodBId)), started()), //
			event(test(uniqueId(nestedMethodBId)), finishedSuccessfully()), //
			event(container(uniqueId(nestedClassId)), finishedSuccessfully()), //
			event(container(uniqueId(invocationId2)), finishedSuccessfully()), //

			event(container(uniqueId(classTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nestedClassTemplateInvocationCanBeSelectedByUniqueId() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var outerClassTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoTimesTwoInvocationsWithMultipleMethodsTestCase.class.getName());
		var outerInvocation2Id = outerClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation2NestedClassTemplateId = outerInvocation2Id.append(
			ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE, "NestedTestCase");
		var outerInvocation2InnerInvocation2Id = outerInvocation2NestedClassTemplateId.append(
			ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation2InnerInvocation2NestedMethodId = outerInvocation2InnerInvocation2Id.append(
			TestMethodTestDescriptor.SEGMENT_TYPE, "b()");

		var results = executeTests(selectUniqueId(outerInvocation2InnerInvocation2NestedMethodId));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(outerClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation2Id)),
				displayName("[2] B of TwoTimesTwoInvocationsWithMultipleMethodsTestCase")), //
			event(container(uniqueId(outerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation2NestedClassTemplateId))), //
			event(container(uniqueId(outerInvocation2NestedClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation2Id)),
				displayName("[2] B of NestedTestCase")), //
			event(container(uniqueId(outerInvocation2InnerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation2NestedMethodId))), //
			event(test(uniqueId(outerInvocation2InnerInvocation2NestedMethodId)), started()), //
			event(test(uniqueId(outerInvocation2InnerInvocation2NestedMethodId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation2InnerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerInvocation2NestedClassTemplateId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerClassTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nestedClassTemplateInvocationCanBeSelectedByIteration() {
		var engineId = UniqueId.forEngine(JupiterEngineDescriptor.ENGINE_ID);
		var outerClassTemplateId = engineId.append(ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			TwoTimesTwoInvocationsTestCase.class.getName());
		var outerInvocation1Id = outerClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#1");
		var outerInvocation1NestedClassTemplateId = outerInvocation1Id.append(
			ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE, "NestedTestCase");
		var outerInvocation1InnerInvocation2Id = outerInvocation1NestedClassTemplateId.append(
			ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation1InnerInvocation2NestedMethodId = outerInvocation1InnerInvocation2Id.append(
			TestMethodTestDescriptor.SEGMENT_TYPE, "test()");
		var outerInvocation2Id = outerClassTemplateId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation2NestedClassTemplateId = outerInvocation2Id.append(
			ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE, "NestedTestCase");
		var outerInvocation2InnerInvocation2Id = outerInvocation2NestedClassTemplateId.append(
			ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#2");
		var outerInvocation2InnerInvocation2NestedMethodId = outerInvocation2InnerInvocation2Id.append(
			TestMethodTestDescriptor.SEGMENT_TYPE, "test()");

		var results = executeTests(selectIteration(selectNestedClass(List.of(TwoTimesTwoInvocationsTestCase.class),
			TwoTimesTwoInvocationsTestCase.NestedTestCase.class), 1));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(uniqueId(outerClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation1Id)),
				displayName("[1] A of TwoTimesTwoInvocationsTestCase")), //
			event(container(uniqueId(outerInvocation1Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation1NestedClassTemplateId))), //
			event(container(uniqueId(outerInvocation1NestedClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation1InnerInvocation2Id)),
				displayName("[2] B of NestedTestCase")), //
			event(container(uniqueId(outerInvocation1InnerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation1InnerInvocation2NestedMethodId))), //
			event(test(uniqueId(outerInvocation1InnerInvocation2NestedMethodId)), started()), //
			event(test(uniqueId(outerInvocation1InnerInvocation2NestedMethodId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation1InnerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerInvocation1NestedClassTemplateId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation1Id)), finishedSuccessfully()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation2Id)),
				displayName("[2] B of TwoTimesTwoInvocationsTestCase")), //
			event(container(uniqueId(outerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation2NestedClassTemplateId))), //
			event(container(uniqueId(outerInvocation2NestedClassTemplateId)), started()), //

			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation2Id)),
				displayName("[2] B of NestedTestCase")), //
			event(container(uniqueId(outerInvocation2InnerInvocation2Id)), started()), //
			event(dynamicTestRegistered(uniqueId(outerInvocation2InnerInvocation2NestedMethodId))), //
			event(test(uniqueId(outerInvocation2InnerInvocation2NestedMethodId)), started()), //
			event(test(uniqueId(outerInvocation2InnerInvocation2NestedMethodId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation2InnerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerInvocation2NestedClassTemplateId)), finishedSuccessfully()), //
			event(container(uniqueId(outerInvocation2Id)), finishedSuccessfully()), //

			event(container(uniqueId(outerClassTemplateId)), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void executesLifecycleCallbacksInNestedClassTemplates() {
		var results = executeTestsForClass(TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase.class);

		results.containerEvents().assertStatistics(stats -> stats.started(10).succeeded(10));
		results.testEvents().assertStatistics(stats -> stats.started(8).succeeded(8));

		// @formatter:off
		assertThat(allReportEntryValues(results)).containsExactly(
			"beforeAll: TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase",
				"beforeClassTemplateInvocation: TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase",
					"beforeAll: NestedTestCase",
						"beforeClassTemplateInvocation: NestedTestCase",
							"beforeEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test1 [NestedTestCase]",
									"test1",
								"afterEach: test1 [NestedTestCase]",
							"afterEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
							"beforeEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test2 [NestedTestCase]",
									"test2",
								"afterEach: test2 [NestedTestCase]",
							"afterEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
						"afterClassTemplateInvocation: NestedTestCase",
						"beforeClassTemplateInvocation: NestedTestCase",
							"beforeEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test1 [NestedTestCase]",
									"test1",
								"afterEach: test1 [NestedTestCase]",
							"afterEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
							"beforeEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test2 [NestedTestCase]",
									"test2",
								"afterEach: test2 [NestedTestCase]",
							"afterEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
						"afterClassTemplateInvocation: NestedTestCase",
					"afterAll: NestedTestCase",
				"afterClassTemplateInvocation: TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase",
				"beforeClassTemplateInvocation: TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase",
					"beforeAll: NestedTestCase",
						"beforeClassTemplateInvocation: NestedTestCase",
							"beforeEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test1 [NestedTestCase]",
									"test1",
								"afterEach: test1 [NestedTestCase]",
							"afterEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
							"beforeEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test2 [NestedTestCase]",
									"test2",
								"afterEach: test2 [NestedTestCase]",
							"afterEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
						"afterClassTemplateInvocation: NestedTestCase",
						"beforeClassTemplateInvocation: NestedTestCase",
							"beforeEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test1 [NestedTestCase]",
									"test1",
								"afterEach: test1 [NestedTestCase]",
							"afterEach: test1 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
							"beforeEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
								"beforeEach: test2 [NestedTestCase]",
									"test2",
								"afterEach: test2 [NestedTestCase]",
							"afterEach: test2 [TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase]",
						"afterClassTemplateInvocation: NestedTestCase",
					"afterAll: NestedTestCase",
				"afterClassTemplateInvocation: TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase",
			"afterAll: TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase"
		);
		// @formatter:on
	}

	@Test
	void guaranteesWrappingBehaviorForCallbacks() {
		var results = executeTestsForClass(CallbackWrappingBehaviorTestCase.class);

		results.containerEvents().assertStatistics(stats -> stats.started(4).succeeded(4));
		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));

		// @formatter:off
		assertThat(allReportEntryValues(results)).containsExactly(
				"1st -> beforeClassTemplateInvocation: CallbackWrappingBehaviorTestCase",
				"2nd -> beforeClassTemplateInvocation: CallbackWrappingBehaviorTestCase",
				"test",
				"2nd -> afterClassTemplateInvocation: CallbackWrappingBehaviorTestCase",
				"1st -> afterClassTemplateInvocation: CallbackWrappingBehaviorTestCase",
				"1st -> beforeClassTemplateInvocation: CallbackWrappingBehaviorTestCase",
				"2nd -> beforeClassTemplateInvocation: CallbackWrappingBehaviorTestCase",
				"test",
				"2nd -> afterClassTemplateInvocation: CallbackWrappingBehaviorTestCase",
				"1st -> afterClassTemplateInvocation: CallbackWrappingBehaviorTestCase"
		);
		// @formatter:on
	}

	@Test
	void propagatesExceptionsFromCallbacks() {

		var results = executeTestsForClass(CallbackExceptionBehaviorTestCase.class);

		results.allEvents().assertStatistics(stats -> stats.started(4).failed(2).succeeded(2));

		results.containerEvents().assertThatEvents() //
				.haveExactly(2, finishedWithFailure( //
					message("2nd -> afterClassTemplateInvocation: CallbackExceptionBehaviorTestCase"), //
					suppressed(0, message("1st -> beforeClassTemplateInvocation: CallbackExceptionBehaviorTestCase")), //
					suppressed(1, message("1st -> afterClassTemplateInvocation: CallbackExceptionBehaviorTestCase"))));

		assertThat(allReportEntryValues(results).distinct()) //
				.containsExactly("1st -> beforeClassTemplateInvocation: CallbackExceptionBehaviorTestCase", //
					"2nd -> afterClassTemplateInvocation: CallbackExceptionBehaviorTestCase", //
					"1st -> afterClassTemplateInvocation: CallbackExceptionBehaviorTestCase");
	}

	@Test
	void templateWithPreparations() {
		var results = executeTestsForClass(ClassTemplateWithPreparationsTestCase.class);

		results.allEvents().assertStatistics(stats -> stats.started(6).succeeded(6));
		assertTrue(CustomCloseableResource.closed, "resource in store was closed");
	}

	@Test
	void propagatesTagsFromEnclosingClassesToNestedClassTemplates() {
		var request = defaultRequest() //
				.selectors(selectClass(NestedClassTemplateWithTagOnEnclosingClassTestCase.class)) //
				.build();
		var engineDescriptor = discoverTestsWithoutIssues(request);
		var classDescriptor = getOnlyElement(engineDescriptor.getChildren());
		var nestedClassTemplateDescriptor = getOnlyElement(classDescriptor.getChildren());

		assertThat(classDescriptor.getTags()).extracting(TestTag::getName) //
				.containsExactly("top-level");
		assertThat(nestedClassTemplateDescriptor.getTags()).extracting(TestTag::getName) //
				.containsExactlyInAnyOrder("top-level", "nested");
	}

	@Test
	void ignoresComposedAnnotations() {
		var request = defaultRequest() //
				.selectors(selectClass(ParameterizedClass.class)) //
				.build();

		var engineDescriptor = discoverTestsWithoutIssues(request);

		assertThat(engineDescriptor.getDescendants()).isEmpty();
	}

	@Test
	void classTemplateWithResourceLockCollectsExclusiveResources() {
		var results = discoverTestsForClass(ClassTemplateWithResourceLockTestCase.class);
		var classTemplateDescriptor = (ClassTemplateTestDescriptor) getOnlyElement(
			results.getEngineDescriptor().getChildren());

		assertThat(classTemplateDescriptor.getExclusiveResources()).extracting(
			ExclusiveResource::getKey).containsExactly("test-resource");
	}

	@Test
	void classTemplateWithResourceLockExecutesSuccessfully() {
		var results = executeTestsForClass(ClassTemplateWithResourceLockTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
		results.containerEvents().assertStatistics(stats -> stats.started(4).succeeded(4));
	}

	// -------------------------------------------------------------------

	private static Stream<String> allReportEntryValues(EngineExecutionResults results) {
		return results.allEvents().reportingEntryPublished() //
				.map(event -> event.getRequiredPayload(ReportEntry.class)) //
				.map(ReportEntry::getKeyValuePairs) //
				.map(Map::values) //
				.flatMap(Collection::stream);
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	static class TwoInvocationsTestCase {
		@Test
		void a() {
		}

		@Nested
		class NestedTestCase {
			@Test
			@Tag("nested")
			void b() {
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class NestedClassTemplateWithTwoInvocationsTestCase {
		@Test
		void a() {
		}

		@Nested
		@ClassTemplate
		@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
		class NestedTestCase {
			@Test
			void b() {
			}
		}
	}

	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	@ClassTemplate
	static class TwoTimesTwoInvocationsTestCase {
		@Nested
		@ClassTemplate
		class NestedTestCase {
			@Test
			void test() {
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	static class TwoInvocationsWithExtensionTestCase {
		@Test
		void a() {
		}

		@Nested
		class NestedTestCase {
			@Test
			@Tag("nested")
			void b() {
			}
		}
	}

	static class TwoInvocationsClassTemplateInvocationContextProvider
			implements ClassTemplateInvocationContextProvider {

		@Override
		public boolean supportsClassTemplate(ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<Ctx> provideClassTemplateInvocationContexts(ExtensionContext context) {
			var suffix = " of %s".formatted(context.getRequiredTestClass().getSimpleName());
			return Stream.of(new Ctx("A" + suffix), new Ctx("B" + suffix));
		}

		record Ctx(String displayName) implements ClassTemplateInvocationContext {
			@Override
			public String getDisplayName(int invocationIndex) {
				var defaultDisplayName = ClassTemplateInvocationContext.super.getDisplayName(invocationIndex);
				return "%s %s".formatted(defaultDisplayName, displayName);
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(AdditionalExtensionRegistrationTestCase.Ext.class)
	static class AdditionalExtensionRegistrationTestCase {

		@Test
		void test(Data data) {
			assertNotNull(data);
			assertNotNull(data.value());
		}

		static class Ext implements ClassTemplateInvocationContextProvider {
			@Override
			public boolean supportsClassTemplate(ExtensionContext context) {
				return true;
			}

			@Override
			public Stream<Ctx> provideClassTemplateInvocationContexts(ExtensionContext context) {
				return Stream.of(new Data("A"), new Data("B")).map(Ctx::new);
			}
		}

		record Ctx(Data data) implements ClassTemplateInvocationContext {
			@Override
			public String getDisplayName(int invocationIndex) {
				return this.data.value();
			}

			@Override
			public List<Extension> getAdditionalExtensions() {
				return List.of(new ParameterResolver() {
					@Override
					public boolean supportsParameter(ParameterContext parameterContext,
							ExtensionContext extensionContext) throws ParameterResolutionException {
						return Data.class.equals(parameterContext.getParameter().getType());
					}

					@Override
					public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
							throws ParameterResolutionException {
						return Ctx.this.data;
					}
				});
			}
		}

		record Data(String value) {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	@ExtendWith(SeparateExtensionContextTestCase.SomeResourceExtension.class)
	static class SeparateExtensionContextTestCase {

		@Test
		void test(SomeResource someResource) {
			assertFalse(someResource.closed);
		}

		static class SomeResourceExtension implements BeforeAllCallback, ParameterResolver {

			@Override
			public void beforeAll(ExtensionContext context) throws Exception {
				context.getStore(Namespace.GLOBAL).put("someResource", new SomeResource());
			}

			@Override
			public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
					throws ParameterResolutionException {
				var parentContext = extensionContext.getParent().orElseThrow();
				assertAll( //
					() -> assertEquals(SeparateExtensionContextTestCase.class, parentContext.getRequiredTestClass()), //
					() -> assertEquals(SeparateExtensionContextTestCase.class,
						parentContext.getElement().orElseThrow()), //
					() -> assertEquals(TestInstance.Lifecycle.PER_METHOD,
						parentContext.getTestInstanceLifecycle().orElseThrow()) //
				);
				return SomeResource.class.equals(parameterContext.getParameter().getType());
			}

			@Override
			public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
					throws ParameterResolutionException {
				return extensionContext.getStore(Namespace.GLOBAL).get("someResource");
			}
		}

		static class SomeResource implements AutoCloseable {
			private boolean closed;

			@Override
			public void close() {
				this.closed = true;
			}
		}
	}

	@ClassTemplate
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	static class CombinationWithTestTemplateTestCase {

		@ParameterizedTest
		@ValueSource(ints = { 1, 2 })
		void test(int i) {
			assertNotEquals(0, i);
		}
	}

	@ClassTemplate
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	static class CombinationWithTestFactoryTestCase {

		@TestFactory
		Stream<DynamicTest> test() {
			return IntStream.of(1, 2) //
					.mapToObj(i -> dynamicTest("test" + i, () -> assertNotEquals(0, i)));
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(InvalidZeroInvocationTestCase.Ext.class)
	static class InvalidZeroInvocationTestCase {

		@Test
		void test() {
			fail("should not be called");
		}

		static class Ext implements ClassTemplateInvocationContextProvider {

			@Override
			public boolean supportsClassTemplate(ExtensionContext context) {
				return true;
			}

			@Override
			public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
					ExtensionContext context) {
				return Stream.empty();
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(ValidZeroInvocationTestCase.Ext.class)
	static class ValidZeroInvocationTestCase {

		@Test
		void test() {
			fail("should not be called");
		}

		static class Ext implements ClassTemplateInvocationContextProvider {

			@Override
			public boolean supportsClassTemplate(ExtensionContext context) {
				return true;
			}

			@Override
			public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
					ExtensionContext context) {
				return Stream.empty();
			}

			@Override
			public boolean mayReturnZeroClassTemplateInvocationContexts(ExtensionContext context) {
				return true;
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	static class NoProviderRegisteredTestCase {

		@Test
		void test() {
			fail("should not be called");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(NoSupportingProviderRegisteredTestCase.Ext.class)
	static class NoSupportingProviderRegisteredTestCase {

		@Test
		void test() {
			fail("should not be called");
		}

		static class Ext implements ClassTemplateInvocationContextProvider {

			@Override
			public boolean supportsClassTemplate(ExtensionContext context) {
				return false;
			}

			@Override
			public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
					ExtensionContext context) {
				throw new RuntimeException("should not be called");
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	@ClassTemplate
	static class TwoTimesTwoInvocationsWithMultipleMethodsTestCase {

		@Test
		void test() {
		}

		@Nested
		@ClassTemplate
		class NestedTestCase {
			@Test
			void a() {
			}

			@Test
			void b() {
			}
		}

		@Nested
		@ClassTemplate
		class AnotherNestedTestCase {
			@Test
			void test() {
			}
		}
	}

	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	@ExtendWith(ClassTemplateInvocationCallbacks.class)
	@ClassTemplate
	static class TwoTimesTwoInvocationsWithLifecycleCallbacksTestCase extends LifecycleCallbacks {

		@Nested
		@ClassTemplate
		class NestedTestCase extends LifecycleCallbacks {

			@Test
			@DisplayName("test1")
			void test1(TestReporter testReporter) {
				testReporter.publishEntry("test1");
			}

			@Test
			@DisplayName("test2")
			void test2(TestReporter testReporter) {
				testReporter.publishEntry("test2");
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class LifecycleCallbacks {
		@BeforeAll
		static void beforeAll(TestReporter testReporter, TestInfo testInfo) {
			testReporter.publishEntry("beforeAll: " + testInfo.getTestClass().orElseThrow().getSimpleName());
		}

		@BeforeEach
		void beforeEach(TestReporter testReporter, TestInfo testInfo) {
			testReporter.publishEntry(
				"beforeEach: " + testInfo.getDisplayName() + " [" + getClass().getSimpleName() + "]");
		}

		@AfterEach
		void afterEach(TestReporter testReporter, TestInfo testInfo) {
			testReporter.publishEntry(
				"afterEach: " + testInfo.getDisplayName() + " [" + getClass().getSimpleName() + "]");
		}

		@AfterAll
		static void afterAll(TestReporter testReporter, TestInfo testInfo) {
			testReporter.publishEntry("afterAll: " + testInfo.getTestClass().orElseThrow().getSimpleName());
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith({ PreparingClassTemplateInvocationContextProvider.class, CompanionExtension.class })
	static class ClassTemplateWithPreparationsTestCase {

		@Test
		void test(CustomCloseableResource resource) {
			assertNotNull(resource);
			assertFalse(CustomCloseableResource.closed, "should not be closed yet");
		}

	}

	private static class PreparingClassTemplateInvocationContextProvider
			implements ClassTemplateInvocationContextProvider {

		static final Namespace NAMESPACE = Namespace.create(PreparingClassTemplateInvocationContextProvider.class);

		@Override
		public boolean supportsClassTemplate(ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<? extends ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
				ExtensionContext context) {
			var invocationContext = new PreparingClassTemplateInvocationContext();
			return Stream.of(invocationContext, invocationContext);
		}

	}

	private static class PreparingClassTemplateInvocationContext implements ClassTemplateInvocationContext {

		@Override
		public void prepareInvocation(ExtensionContext context) {
			CustomCloseableResource.closed = false;
			context.getStore(PreparingClassTemplateInvocationContextProvider.NAMESPACE) //
					.put("resource", new CustomCloseableResource());
		}

	}

	private static class CompanionExtension implements ParameterResolver {

		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return ExtensionContextScope.TEST_METHOD;
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			return CustomCloseableResource.class.equals(parameterContext.getParameter().getType());
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			return extensionContext.getStore(PreparingClassTemplateInvocationContextProvider.NAMESPACE).get("resource");
		}

	}

	@SuppressWarnings("deprecation")
	private static class CustomCloseableResource implements ExtensionContext.Store.CloseableResource {

		static boolean closed;

		@Override
		public void close() {
			closed = true;
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	@ClassTemplate
	static class CallbackWrappingBehaviorTestCase {

		@RegisterExtension
		@Order(1)
		static Extension first = new ClassTemplateInvocationCallbacks("1st -> ");

		@RegisterExtension
		@Order(2)
		static Extension second = new ClassTemplateInvocationCallbacks("2nd -> ");

		@Test
		void test(TestReporter testReporter) {
			testReporter.publishEntry("test");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	@ClassTemplate
	static class CallbackExceptionBehaviorTestCase {

		@RegisterExtension
		@Order(1)
		static Extension first = new ClassTemplateInvocationCallbacks("1st -> ", TestAbortedException::new);

		@RegisterExtension
		@Order(2)
		static Extension second = new ClassTemplateInvocationCallbacks("2nd -> ", AssertionFailedError::new);

		@Test
		void test() {
			fail("should not be called");
		}
	}

	static class ClassTemplateInvocationCallbacks
			implements BeforeClassTemplateInvocationCallback, AfterClassTemplateInvocationCallback {

		private final String prefix;
		private final Function<String, Throwable> exceptionFactory;

		@SuppressWarnings("unused")
		ClassTemplateInvocationCallbacks() {
			this("");
		}

		ClassTemplateInvocationCallbacks(String prefix) {
			this(prefix, __ -> null);
		}

		ClassTemplateInvocationCallbacks(String prefix, Function<String, Throwable> exceptionFactory) {
			this.prefix = prefix;
			this.exceptionFactory = exceptionFactory;
		}

		@Override
		public void beforeClassTemplateInvocation(ExtensionContext context) {
			handle("beforeClassTemplateInvocation", context);
		}

		@Override
		public void afterClassTemplateInvocation(ExtensionContext context) {
			handle("afterClassTemplateInvocation", context);
		}

		private void handle(String methodName, ExtensionContext context) {
			var message = format(methodName, context);
			context.publishReportEntry(message);
			var throwable = exceptionFactory.apply(message);
			if (throwable != null) {
				throw throwAsUncheckedException(throwable);
			}
		}

		private String format(String methodName, ExtensionContext context) {
			return "%s%s: %s".formatted(prefix, methodName, context.getRequiredTestClass().getSimpleName());
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InheritedTwoInvocationsTestCase extends TwoInvocationsTestCase {
		@Test
		void c() {
		}
	}

	@Tag("top-level")
	static class NestedClassTemplateWithTagOnEnclosingClassTestCase {
		@Nested
		@ClassTemplate
		@Tag("nested")
		@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
		class NestedTestCase {
			@Test
			void test() {
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ClassTemplate
	@ExtendWith(TwoInvocationsClassTemplateInvocationContextProvider.class)
	@ResourceLock("test-resource")
	static class ClassTemplateWithResourceLockTestCase {
		@Test
		void test() {
			// This test verifies that @ResourceLock works with @ClassTemplate (issue #5155)
		}
	}

}
