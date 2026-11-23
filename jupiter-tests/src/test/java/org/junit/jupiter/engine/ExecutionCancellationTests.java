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

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.engine.ExecutionCancellationTests.TestCase.requiredCancellationToken;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.reportEntry;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.CancellationToken;
import org.opentest4j.TestAbortedException;

class ExecutionCancellationTests extends AbstractJupiterTestEngineTests {

	@BeforeEach
	void initializeCancellationToken() {
		TestCase.cancellationToken = CancellationToken.create();
	}

	@AfterEach
	void resetCancellationToken() {
		TestCase.cancellationToken = null;
	}

	@Test
	void canCancelExecutionWhileTestClassIsRunning() {
		var testClass = RegularTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(1).finished(1).skipped(1));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("first"), started()), //
			event(test("first"), reportEntry(Map.of("cancelled", "true"))), //
			event(test("first"), finishedSuccessfully()), //
			event(test("second"), skippedWithReason("Execution cancelled")), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void canCancelExecutionWhileDynamicTestsAreRunning() {
		var testClass = DynamicTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.containerEvents().assertStatistics(stats -> stats.skipped(1));
		results.testEvents().assertStatistics(stats -> stats.started(1).finished(1).skipped(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container("testFactory"), started()), //
			event(dynamicTestRegistered("#1"), displayName("first")), //
			event(test("#1"), started()), //
			event(test("#1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("#2"), displayName("container")), //
			event(container("#2"), skippedWithReason("Execution cancelled")), //
			event(container("testFactory"), finishedSuccessfully()), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void abortsTestWhenCancelledDuringBeforeEachMethod() {
		var testClass = CancelledInBeforeEachTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(1).aborted(1).failed(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testMethod"), started()), //
			event(test("testMethod"), reportEntry(Map.of("afterEach", "called"))), //
			event(test("testMethod"), abortedWithReason(instanceOf(TestAbortedException.class))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void reportsFailureWhenCancelledAndThenFailingDuringBeforeEachMethod() {
		var testClass = CancelledAndThenFailingInBeforeEachTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(1).failed(1).aborted(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testMethod"), started()), //
			event(test("testMethod"), reportEntry(Map.of("afterEach", "called"))), //
			event(test("testMethod"), finishedWithFailure(instanceOf(IllegalStateException.class), message("failure"))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void abortsClassWhenCancelledDuringBeforeAllMethod() {
		var testClass = CancelledInBeforeAllMethodTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), reportEntry(Map.of("afterAll", "called"))), //
			event(container(testClass), abortedWithReason(instanceOf(TestAbortedException.class))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void doesNotInvokeRemainingBeforeEachCallbacksWhenCancelled() {
		var testClass = CancelledInFirstOfTwoBeforeEachCallbacksTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(1).aborted(1).failed(0));
	}

	@Test
	void abortsClassWhenCancelledDuringPerClassInstanceCreation() {
		var testClass = CancelledDuringPerClassInstanceCreationTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), abortedWithReason(instanceOf(TestAbortedException.class))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void doesNotInvokeTestMethodWhenCancelledDuringBeforeTestExecutionCallback() {
		var testClass = CancelledInBeforeTestExecutionCallbackTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(1).aborted(1).failed(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testMethod"), started()), //
			event(test("testMethod"), reportEntry(Map.of("afterTestExecution", "called"))), //
			event(test("testMethod"), reportEntry(Map.of("afterEach", "called"))), //
			event(test("testMethod"), abortedWithReason(instanceOf(TestAbortedException.class))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void abortsClassTemplateInvocationWhenCancelledDuringBeforeClassTemplateInvocationCallback() {
		var testClass = CancelledInBeforeClassTemplateInvocationCallbackTestCase.class;

		var results = jupiterTestEngine() //
				.selectors(selectClass(testClass)) //
				.cancellationToken(requiredCancellationToken()) //
				.execute();

		results.testEvents().assertStatistics(stats -> stats.started(0));

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(dynamicTestRegistered("#1")), //
			event(container("#1"), started()), //
			event(container("#1"), reportEntry(Map.of("afterClassTemplateInvocation", "called"))), //
			event(container("#1"), abortedWithReason(instanceOf(TestAbortedException.class))), //
			event(dynamicTestRegistered("#2")), //
			event(container("#2"), skippedWithReason("Execution cancelled")), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	static class TestCase {

		static @Nullable CancellationToken cancellationToken;

		static CancellationToken requiredCancellationToken() {
			return requireNonNull(cancellationToken);
		}

	}

	@TestMethodOrder(OrderAnnotation.class)
	static class RegularTestCase extends TestCase {

		@Test
		@Order(1)
		void first() {
			requiredCancellationToken().cancel();
		}

		@AfterEach
		void afterEach(TestReporter reporter) {
			reporter.publishEntry("cancelled", String.valueOf(requiredCancellationToken().isCancellationRequested()));
		}

		@Test
		@Order(2)
		void second() {
			fail("should not be called");
		}
	}

	static class DynamicTestCase extends TestCase {

		@TestFactory
		Stream<DynamicNode> testFactory() {
			return Stream.of( //
				dynamicTest("first", () -> requiredCancellationToken().cancel()), //
				dynamicContainer("container", Stream.of( //
					dynamicTest("second", () -> fail("should not be called")) //
				)) //
			);
		}
	}

	static class CancelledInBeforeEachTestCase extends TestCase {

		@BeforeEach
		void beforeEach() {
			requiredCancellationToken().cancel();
		}

		@Test
		void testMethod() {
			fail("should not be called");
		}

		@AfterEach
		void afterEach(TestReporter reporter) {
			reporter.publishEntry("afterEach", "called");
		}
	}

	static class CancelledAndThenFailingInBeforeEachTestCase extends TestCase {

		@BeforeEach
		void beforeEach() {
			requiredCancellationToken().cancel();
			throw new IllegalStateException("failure");
		}

		@Test
		void testMethod() {
			fail("should not be called");
		}

		@AfterEach
		void afterEach(TestReporter reporter) {
			reporter.publishEntry("afterEach", "called");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class CancelledInBeforeAllMethodTestCase extends TestCase {

		@BeforeAll
		static void beforeAll() {
			requiredCancellationToken().cancel();
		}

		@Test
		void testMethod() {
			fail("should not be called");
		}

		@AfterAll
		static void afterAll(TestReporter reporter) {
			reporter.publishEntry("afterAll", "called");
		}
	}

	@ExtendWith({ CancellingBeforeEachCallback.class, FailingBeforeEachCallback.class })
	static class CancelledInFirstOfTwoBeforeEachCallbacksTestCase extends TestCase {

		@Test
		void testMethod() {
			fail("should not be called");
		}
	}

	static class CancellingBeforeEachCallback implements BeforeEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			requiredCancellationToken().cancel();
		}
	}

	static class FailingBeforeEachCallback implements BeforeEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			fail("should not be called");
		}
	}

	@ExtendWith({ CancellingBeforeTestExecutionCallback.class, ReportingAfterTestExecutionCallback.class })
	static class CancelledInBeforeTestExecutionCallbackTestCase extends TestCase {

		@Test
		void testMethod() {
			fail("should not be called");
		}

		@AfterEach
		void afterEach(TestReporter reporter) {
			reporter.publishEntry("afterEach", "called");
		}
	}

	static class CancellingBeforeTestExecutionCallback implements BeforeTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			requiredCancellationToken().cancel();
		}
	}

	static class ReportingAfterTestExecutionCallback implements AfterTestExecutionCallback {

		@Override
		public void afterTestExecution(ExtensionContext context) {
			context.publishReportEntry("afterTestExecution", "called");
		}
	}

	@TestInstance(PER_CLASS)
	@ExtendWith(FailingBeforeAllCallback.class)
	static class CancelledDuringPerClassInstanceCreationTestCase extends TestCase {

		CancelledDuringPerClassInstanceCreationTestCase() {
			requiredCancellationToken().cancel();
		}

		@BeforeAll
		void beforeAll() {
			fail("should not be called");
		}

		@Test
		void testMethod() {
			fail("should not be called");
		}
	}

	static class FailingBeforeAllCallback implements BeforeAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			fail("should not be called");
		}
	}

	@ClassTemplate
	@ExtendWith({ TwoInvocationsProvider.class, CancellingBeforeClassTemplateInvocationCallback.class,
			FailingBeforeClassTemplateInvocationCallback.class, ReportingAfterClassTemplateInvocationCallback.class })
	static class CancelledInBeforeClassTemplateInvocationCallbackTestCase extends TestCase {

		@Test
		void testMethod() {
			fail("should not be called");
		}
	}

	static class TwoInvocationsProvider implements ClassTemplateInvocationContextProvider {

		@Override
		public boolean supportsClassTemplate(ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(ExtensionContext context) {
			return Stream.of(new ClassTemplateInvocationContext() {
			}, new ClassTemplateInvocationContext() {
			});
		}
	}

	static class CancellingBeforeClassTemplateInvocationCallback implements BeforeClassTemplateInvocationCallback {

		@Override
		public void beforeClassTemplateInvocation(ExtensionContext context) {
			requiredCancellationToken().cancel();
		}
	}

	static class FailingBeforeClassTemplateInvocationCallback implements BeforeClassTemplateInvocationCallback {

		@Override
		public void beforeClassTemplateInvocation(ExtensionContext context) {
			fail("should not be called");
		}
	}

	static class ReportingAfterClassTemplateInvocationCallback implements AfterClassTemplateInvocationCallback {

		@Override
		public void afterClassTemplateInvocation(ExtensionContext context) {
			context.publishReportEntry("afterClassTemplateInvocation", "called");
		}
	}
}
