/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Constants.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Constants.TIMEOUT_MODE_PROPERTY_NAME;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.Timeout.ThreadMode.SAME_THREAD;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.RuntimeUtils;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Events;
import org.junit.platform.testkit.engine.Execution;
import org.opentest4j.AssertionFailedError;

/**
 * @since 5.5
 */
@DisplayName("@Timeout")
class TimeoutExtensionTests extends AbstractJupiterTestEngineTests {

	@Test
	@DisplayName("is applied on annotated @Test methods")
	void appliesTimeoutOnAnnotatedTestMethods() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectMethod(TimeoutAnnotatedTestMethodTestCase.class, "testMethod")) //
				.configurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		Execution execution = findExecution(results.testEvents(), "testMethod()");
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessage("testMethod() timed out after 10 milliseconds");
	}

	@Test
	@DisplayName("is not applied on annotated @Test methods using timeout mode: disabled")
	void doesNotApplyTimeoutOnAnnotatedTestMethodsUsingDisabledTimeoutMode() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectMethod(TimeoutAnnotatedTestMethodTestCase.class, "testMethod")) //
				.configurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.configurationParameter(TIMEOUT_MODE_PROPERTY_NAME, "disabled").build());

		Execution execution = findExecution(results.testEvents(), "testMethod()");
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable()) //
				.isEmpty();
	}

	@Test
	@DisplayName("is not applied on annotated @Test methods using timeout mode: disabled")
	void applyTimeoutOnAnnotatedTestMethodsUsingDisabledOnDebugTimeoutMode() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectMethod(TimeoutAnnotatedTestMethodTestCase.class, "testMethod")) //
				.configurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.configurationParameter(TIMEOUT_MODE_PROPERTY_NAME, "disabled_on_debug").build());

		Execution execution = findExecution(results.testEvents(), "testMethod()");

		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				// The check to see if debugging is pushing the timer just above 1 second
				.isLessThan(Duration.ofSeconds(2));

		// Should we test if we're debugging? This test will fail if we are debugging.
		if (RuntimeUtils.isDebugMode()) {
			assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable()) //
					.isEmpty();
		}
		else {
			assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("testMethod() timed out after 10 milliseconds");
		}
	}

	@Test
	@DisplayName("is applied on annotated @TestTemplate methods")
	void appliesTimeoutOnAnnotatedTestTemplateMethods() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectMethod(TimeoutAnnotatedTestMethodTestCase.class, "testTemplateMethod")) //
				.configurationParameter(DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		Stream.of("repetition 1", "repetition 2").forEach(displayName -> {
			Execution execution = findExecution(results.testEvents(), displayName);
			assertThat(execution.getDuration()) //
					.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
					.isLessThan(Duration.ofSeconds(1));
			assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("testTemplateMethod() timed out after 10 milliseconds");
		});
	}

	@Test
	@DisplayName("is applied on annotated @TestFactory methods")
	void appliesTimeoutOnAnnotatedTestFactoryMethods() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectMethod(TimeoutAnnotatedTestMethodTestCase.class, "testFactoryMethod")) //
				.configurationParameter(DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		Execution execution = findExecution(results.containerEvents(), "testFactoryMethod()");
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessage("testFactoryMethod() timed out after 10 milliseconds");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(classes = { TimeoutAnnotatedClassTestCase.class, InheritedTimeoutAnnotatedClassTestCase.class })
	@DisplayName("is applied on testable methods in annotated classes")
	void appliesTimeoutOnTestableMethodsInAnnotatedClasses(Class<?> testClass) {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectClass(testClass)) //
				.configurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.configurationParameter(DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.configurationParameter(DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		assertAll(Stream.of("testMethod()", "repetition 1", "repetition 2", "testFactoryMethod()") //
				.map(displayName -> () -> {
					Execution execution = findExecution(results.allEvents(), displayName);
					assertThat(execution.getDuration()) //
							.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
							.isLessThan(Duration.ofSeconds(1));
					assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
							.isInstanceOf(TimeoutException.class) //
							.hasMessageEndingWith("timed out after 10000000 nanoseconds");
				}));
	}

	@Test
	@DisplayName("fails methods that do not throw InterruptedException")
	void failsMethodsWithoutInterruptedException() {
		EngineExecutionResults results = executeTestsForClass(MethodWithoutInterruptedExceptionTestCase.class);

		Execution execution = findExecution(results.testEvents(), "methodThatDoesNotThrowInterruptedException()");
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(1)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getStatus()).isEqualTo(FAILED);
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessage("methodThatDoesNotThrowInterruptedException() timed out after 1 millisecond");
	}

	@Test
	@DisplayName("is applied on annotated @BeforeAll methods")
	void appliesTimeoutOnAnnotatedBeforeAllMethods() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectClass(TimeoutAnnotatedBeforeAllMethodTestCase.class)) //
				.configurationParameter(DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		Execution execution = findExecution(results.containerEvents(),
			TimeoutAnnotatedBeforeAllMethodTestCase.class.getSimpleName());
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessage("setUp() timed out after 10 milliseconds");
	}

	@Test
	@DisplayName("is applied on annotated @BeforeEach methods")
	void appliesTimeoutOnAnnotatedBeforeEachMethods() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectClass(TimeoutAnnotatedBeforeEachMethodTestCase.class)) //
				.configurationParameter(DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		Execution execution = findExecution(results.testEvents(), "testMethod()");
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessage("setUp() timed out after 10 milliseconds");
	}

	@Test
	@DisplayName("is applied on annotated @AfterEach methods")
	void appliesTimeoutOnAnnotatedAfterEachMethods() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectClass(TimeoutAnnotatedAfterEachMethodTestCase.class)) //
				.configurationParameter(DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		Execution execution = findExecution(results.testEvents(), "testMethod()");
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessage("tearDown() timed out after 10 milliseconds");
	}

	@Test
	@DisplayName("is applied on annotated @AfterAll methods")
	void appliesTimeoutOnAnnotatedAfterAllMethods() {
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectClass(TimeoutAnnotatedAfterAllMethodTestCase.class)) //
				.configurationParameter(DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME, "42ns") //
				.build());

		Execution execution = findExecution(results.containerEvents(),
			TimeoutAnnotatedAfterAllMethodTestCase.class.getSimpleName());
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessage("tearDown() timed out after 10 milliseconds");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	@DisplayName("is applied from configuration parameters by default")
	void appliesDefaultTimeoutsFromConfigurationParameters(String propertyName, String slowMethod) {
		PlainTestCase.slowMethod = slowMethod;
		EngineExecutionResults results = executeTests(request() //
				.selectors(selectClass(PlainTestCase.class)) //
				.configurationParameter(propertyName, "1ns") //
				.build());
		var failure = results.allEvents().executions().failed() //
				.map(execution -> execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.findFirst();
		assertThat(failure).containsInstanceOf(TimeoutException.class);
		assertThat(failure.orElseThrow()).hasMessage(slowMethod + " timed out after 1 nanosecond");
	}

	static Stream<Arguments> appliesDefaultTimeoutsFromConfigurationParameters() {
		return Stream.of( //
			Arguments.of(DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME, "beforeAll()"), //
			Arguments.of(DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME, "beforeEach()"), //
			Arguments.of(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, "test()"), //
			Arguments.of(DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME, "testTemplate()"), //
			Arguments.of(DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME, "testFactory()"), //
			Arguments.of(DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME, "afterEach()"), //
			Arguments.of(DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME, "afterAll()") //
		);
	}

	@Test
	@DisplayName("does not swallow unrecoverable exceptions")
	void doesNotSwallowUnrecoverableExceptions() {
		assertThrows(OutOfMemoryError.class, () -> executeTestsForClass(UnrecoverableExceptionTestCase.class));
	}

	@Test
	@DisplayName("does not affect tests that don't exceed the timeout")
	void doesNotAffectTestsThatDoNotExceedTimeoutDuration() {
		executeTestsForClass(NonTimeoutExceedingTestCase.class).allEvents().assertStatistics(stats -> stats.failed(0));
	}

	@Test
	@DisplayName("includes fully qualified class name if method is not in the test class")
	void includesClassNameIfMethodIsNotInTestClass() {
		EngineExecutionResults results = executeTestsForClass(NestedClassWithOuterSetupMethodTestCase.class);

		Execution execution = findExecution(results.testEvents(), "testMethod()");
		assertThat(execution.getDuration()) //
				.isGreaterThanOrEqualTo(Duration.ofMillis(10)) //
				.isLessThan(Duration.ofSeconds(1));
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(TimeoutException.class) //
				.hasMessageEndingWith(
					"$NestedClassWithOuterSetupMethodTestCase#setUp() timed out after 10 milliseconds");
	}

	@Test
	@DisplayName("reports illegal timeout durations")
	void reportsIllegalTimeoutDurations() {
		EngineExecutionResults results = executeTestsForClass(IllegalTimeoutDurationTestCase.class);

		Execution execution = findExecution(results.testEvents(), "testMethod()");
		assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(PreconditionViolationException.class) //
				.hasMessage("timeout duration must be a positive number: 0");
	}

	private static Execution findExecution(Events events, String displayName) {
		return events.executions()//
				.filter(execution -> execution.getTestDescriptor().getDisplayName().contains(displayName))//
				.findFirst() //
				.orElseThrow();
	}

	@Nested
	@DisplayName("separate thread")
	class SeparateThread {
		@Test
		@DisplayName("timeout exceeded")
		void timeoutExceededInSeparateThread() {
			EngineExecutionResults results = executeTestsForClass(TimeoutExceedingSeparateThreadTestCase.class);

			Execution execution = findExecution(results.testEvents(), "testMethod()");
			Throwable failure = execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow();
			assertThat(failure) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("testMethod() timed out after 100 milliseconds");
			assertThat(failure.getCause()) //
					.hasMessageStartingWith("Execution timed out in ") //
					.hasStackTraceContaining(TimeoutExceedingSeparateThreadTestCase.class.getName() + ".testMethod");
		}

		@Test
		@DisplayName("non timeout exceeded")
		void nonTimeoutExceededInSeparateThread() {
			executeTestsForClass(NonTimeoutExceedingSeparateThreadTestCase.class).allEvents() //
					.assertStatistics(stats -> stats.failed(0));
		}

		@Test
		@DisplayName("does not swallow unrecoverable exceptions")
		void separateThreadDoesNotSwallowUnrecoverableExceptions() {
			assertThrows(OutOfMemoryError.class,
				() -> executeTestsForClass(UnrecoverableExceptionInSeparateThreadTestCase.class));
		}

		@Test
		@DisplayName("handles invocation exceptions")
		void separateThreadHandlesInvocationExceptions() {
			EngineExecutionResults results = executeTests(request() //
					.selectors(selectMethod(ExceptionInSeparateThreadTestCase.class, "test")) //
					.build());

			Execution execution = findExecution(results.testEvents(), "test()");
			assertThat(execution.getDuration()) //
					.isLessThan(Duration.ofSeconds(5));
			assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(RuntimeException.class) //
					.hasMessage("Oppps!");
		}

		@Test
		@DisplayName("propagates assertion exceptions")
		void separateThreadHandlesOpenTestFailedAssertion() {
			EngineExecutionResults results = executeTestsForClass(FailedAssertionInSeparateThreadTestCase.class);

			Execution openTestFailure = findExecution(results.testEvents(), "testOpenTestAssertion()");
			assertThat(openTestFailure.getDuration()) //
					.isLessThan(Duration.ofSeconds(5));
			assertThat(openTestFailure.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(AssertionFailedError.class);

			Execution javaLangFailure = findExecution(results.testEvents(), "testJavaLangAssertion()");
			assertThat(javaLangFailure.getDuration()) //
					.isLessThan(Duration.ofSeconds(5));
			assertThat(javaLangFailure.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(AssertionError.class);
		}

		@Test
		@DisplayName("when one test is stuck \"forever\" the next tests should not get stuck")
		void oneThreadStuckForever() {
			EngineExecutionResults results = executeTestsForClass(OneTestStuckForeverAndTheOthersNotTestCase.class);

			Execution stuckExecution = findExecution(results.testEvents(), "stuck()");
			assertThat(stuckExecution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("stuck() timed out after 10 milliseconds");

			Execution testZeroExecution = findExecution(results.testEvents(), "testZero()");
			assertThat(testZeroExecution.getTerminationInfo().getExecutionResult().getStatus()) //
					.isEqualTo(Status.SUCCESSFUL);

			Execution testOneExecution = findExecution(results.testEvents(), "testOne()");
			assertThat(testOneExecution.getTerminationInfo().getExecutionResult().getStatus()) //
					.isEqualTo(Status.SUCCESSFUL);
		}

		@Test
		@DisplayName("mixed same thread and separate thread tests")
		void mixedSameThreadAndSeparateThreadTests() {
			EngineExecutionResults results = executeTestsForClass(MixedSameThreadAndSeparateThreadTestCase.class);

			Execution stuck = findExecution(results.testEvents(), "testZero()");
			assertThat(stuck.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("testZero() timed out after 10 milliseconds");

			Execution testZeroExecution = findExecution(results.testEvents(), "testOne()");
			assertThat(testZeroExecution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("testOne() timed out after 10 milliseconds");

			Execution testOneExecution = findExecution(results.testEvents(), "testTwo()");
			assertThat(testOneExecution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("testTwo() timed out after 10 milliseconds");
		}

		@Test
		@DisplayName("one test is stuck \"forever\" in separate thread and other tests in same thread not")
		void oneThreadStuckForeverAndOtherTestsInSameThread() {
			EngineExecutionResults results = executeTestsForClass(
				OneTestStuckForeverAndTheOthersInSameThreadNotTestCase.class);

			Execution stuckExecution = findExecution(results.testEvents(), "stuck()");
			assertThat(stuckExecution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
					.isInstanceOf(TimeoutException.class) //
					.hasMessage("stuck() timed out after 10 milliseconds");

			Execution testZeroExecution = findExecution(results.testEvents(), "testZero()");
			assertThat(testZeroExecution.getTerminationInfo().getExecutionResult().getStatus()) //
					.isEqualTo(Status.SUCCESSFUL);

			Execution testOneExecution = findExecution(results.testEvents(), "testOne()");
			assertThat(testOneExecution.getTerminationInfo().getExecutionResult().getStatus()) //
					.isEqualTo(Status.SUCCESSFUL);
		}

		@Test
		@DisplayName("is not applied on annotated @Test methods using timeout mode: disabled")
		void doesNotApplyTimeoutOnAnnotatedTestMethodsUsingDisabledTimeoutMode() {
			EngineExecutionResults results = executeTests(request() //
					.selectors(selectMethod(TimeoutExceedingSeparateThreadTestCase.class, "testMethod")) //
					.configurationParameter(TIMEOUT_MODE_PROPERTY_NAME, "disabled").build());

			Execution execution = findExecution(results.testEvents(), "testMethod()");
			assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable()) //
					.isEmpty();
		}

		@Nested
		@DisplayName("on class level")
		class OnClassLevel {
			@Test
			@DisplayName("timeout exceeded")
			void timeoutExceededInSeparateThreadOnClassLevel() {
				EngineExecutionResults results = executeTestsForClass(TimeoutExceededOnClassLevelTestCase.class);

				Execution execution = findExecution(results.testEvents(), "exceptionThrown()");
				assertThat(execution.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow()) //
						.isInstanceOf(TimeoutException.class) //
						.hasMessage("exceptionThrown() timed out after 100 milliseconds");
			}

			@Test
			@DisplayName("non timeout exceeded")
			void nonTimeoutExceededInSeparateThreadOnClassLevel() {
				executeTestsForClass(NonTimeoutExceededOnClassLevelTestCase.class).allEvents() //
						.assertStatistics(stats -> stats.failed(0));
			}
		}
	}

	static class TimeoutAnnotatedTestMethodTestCase {
		@Test
		@Timeout(value = 10, unit = MILLISECONDS)
		void testMethod() throws Exception {
			Thread.sleep(1000);
		}

		@RepeatedTest(2)
		@Timeout(value = 10, unit = MILLISECONDS)
		void testTemplateMethod() throws Exception {
			Thread.sleep(1000);
		}

		@TestFactory
		@Timeout(value = 10, unit = MILLISECONDS)
		Stream<DynamicTest> testFactoryMethod() throws Exception {
			Thread.sleep(1000);
			return Stream.empty();
		}
	}

	static class TimeoutAnnotatedBeforeAllMethodTestCase {
		@BeforeAll
		@Timeout(value = 10, unit = MILLISECONDS)
		static void setUp() throws Exception {
			Thread.sleep(1000);
		}

		@Test
		void testMethod() {
			// never called
		}
	}

	static class TimeoutAnnotatedBeforeEachMethodTestCase {
		@BeforeEach
		@Timeout(value = 10, unit = MILLISECONDS)
		void setUp() throws Exception {
			Thread.sleep(1000);
		}

		@Test
		void testMethod() {
			// never called
		}
	}

	static class TimeoutAnnotatedAfterEachMethodTestCase {
		@Test
		void testMethod() {
			// do nothing
		}

		@AfterEach
		@Timeout(value = 10, unit = MILLISECONDS)
		void tearDown() throws Exception {
			Thread.sleep(1000);
		}
	}

	static class TimeoutAnnotatedAfterAllMethodTestCase {
		@Test
		void testMethod() {
			// do nothing
		}

		@AfterAll
		@Timeout(value = 10, unit = MILLISECONDS)
		static void tearDown() throws Exception {
			Thread.sleep(1000);
		}
	}

	@Timeout(value = 10_000_000, unit = NANOSECONDS)
	static class TimeoutAnnotatedClassTestCase {
		@Nested
		class NestedClass {
			@Test
			void testMethod() throws Exception {
				Thread.sleep(1000);
			}

			@RepeatedTest(2)
			void testTemplateMethod() throws Exception {
				Thread.sleep(1000);
			}

			@TestFactory
			Stream<DynamicTest> testFactoryMethod() throws Exception {
				Thread.sleep(1000);
				return Stream.empty();
			}
		}
	}

	static class InheritedTimeoutAnnotatedClassTestCase extends TimeoutAnnotatedClassTestCase {
	}

	static class MethodWithoutInterruptedExceptionTestCase {
		@Test
		@Timeout(value = 1, unit = MILLISECONDS)
		void methodThatDoesNotThrowInterruptedException() {
			new EventuallyInterruptibleInvocation().proceed();
		}
	}

	static class PlainTestCase {

		@Nullable
		public static String slowMethod;

		@BeforeAll
		static void beforeAll() throws Exception {
			waitForInterrupt("beforeAll()");
		}

		@BeforeEach
		void beforeEach() throws Exception {
			waitForInterrupt("beforeEach()");
		}

		@Test
		void test() throws Exception {
			waitForInterrupt("test()");
		}

		@RepeatedTest(2)
		void testTemplate() throws Exception {
			waitForInterrupt("testTemplate()");
		}

		@TestFactory
		Stream<DynamicTest> testFactory() throws Exception {
			waitForInterrupt("testFactory()");
			return Stream.empty();
		}

		@AfterEach
		void afterEach() throws Exception {
			waitForInterrupt("afterEach()");
		}

		@AfterAll
		static void afterAll() throws Exception {
			waitForInterrupt("afterAll()");
		}

		private static void waitForInterrupt(String methodName) throws InterruptedException {
			if (methodName.equals(slowMethod)) {
				blockUntilInterrupted();
			}
		}
	}

	static class UnrecoverableExceptionTestCase {
		@Test
		@Timeout(value = 1, unit = NANOSECONDS)
		void test() {
			new EventuallyInterruptibleInvocation().proceed();
			throw new OutOfMemoryError();
		}
	}

	@Timeout(10)
	static class NonTimeoutExceedingTestCase {
		@Test
		void testMethod() {
		}

		@RepeatedTest(1)
		void testTemplateMethod() {
		}

		@TestFactory
		Stream<DynamicTest> testFactoryMethod() {
			return Stream.of(dynamicTest("dynamicTest", () -> {
			}));
		}
	}

	static class NestedClassWithOuterSetupMethodTestCase {

		@Timeout(value = 10, unit = MILLISECONDS)
		@BeforeEach
		void setUp() throws Exception {
			Thread.sleep(1000);
		}

		@Nested
		class NestedClass {

			@BeforeEach
			void setUp() {
			}

			@Test
			void testMethod() {
			}

		}

	}

	static class IllegalTimeoutDurationTestCase {

		@Test
		@Timeout(0)
		void testMethod() {
		}

	}

	static class TimeoutExceedingWithInferredThreadModeTestCase {
		@Test
		@Timeout(value = 10, unit = MILLISECONDS)
		void testMethod() throws InterruptedException {
			Thread.sleep(1000);
		}
	}

	static class TimeoutExceedingSeparateThreadTestCase {
		@Test
		@Timeout(value = 100, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void testMethod() throws InterruptedException {
			Thread.sleep(1000);
		}
	}

	static class NonTimeoutExceedingSeparateThreadTestCase {
		@Test
		@Timeout(value = 100, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void testMethod() {
		}
	}

	static class UnrecoverableExceptionInSeparateThreadTestCase {
		@Test
		@Timeout(value = 100, unit = SECONDS, threadMode = SEPARATE_THREAD)
		void test() {
			throw new OutOfMemoryError();
		}
	}

	static class ExceptionInSeparateThreadTestCase {
		@Test
		@Timeout(value = 5, unit = SECONDS, threadMode = SEPARATE_THREAD)
		void test() {
			throw new RuntimeException("Oppps!");
		}
	}

	static class FailedAssertionInSeparateThreadTestCase {
		@Test
		@Timeout(value = 5, unit = SECONDS, threadMode = SEPARATE_THREAD)
		void testOpenTestAssertion() {
			throw new AssertionFailedError();
		}

		@Test
		@Timeout(value = 5, unit = SECONDS, threadMode = SEPARATE_THREAD)
		void testJavaLangAssertion() {
			throw new AssertionError();
		}
	}

	@Timeout(value = 100, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
	static class TimeoutExceededOnClassLevelTestCase {
		@Test
		void exceptionThrown() throws InterruptedException {
			Thread.sleep(1000);
		}
	}

	@Timeout(value = 100, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
	static class NonTimeoutExceededOnClassLevelTestCase {
		@Test
		void test() {
		}
	}

	@TestMethodOrder(OrderAnnotation.class)
	static class OneTestStuckForeverAndTheOthersNotTestCase {

		@Test
		@Order(0)
		@Timeout(value = 10, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void stuck() throws InterruptedException {
			blockUntilInterrupted();
		}

		@Test
		@Order(1)
		@Timeout(value = 100, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void testZero() {
		}

		@Test
		@Order(2)
		@Timeout(value = 100, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void testOne() {
		}
	}

	static class MixedSameThreadAndSeparateThreadTestCase {
		@Test
		@Timeout(value = 10, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void testZero() throws InterruptedException {
			Thread.sleep(1000);
		}

		@Test
		@Timeout(value = 10, unit = MILLISECONDS, threadMode = SAME_THREAD)
		void testOne() throws InterruptedException {
			Thread.sleep(1000);
		}

		@Test
		@Timeout(value = 10, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void testTwo() throws InterruptedException {
			Thread.sleep(1000);
		}
	}

	@TestMethodOrder(OrderAnnotation.class)
	static class OneTestStuckForeverAndTheOthersInSameThreadNotTestCase {

		@Test
		@Order(0)
		@Timeout(value = 10, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		void stuck() throws InterruptedException {
			blockUntilInterrupted();
		}

		@Test
		@Order(1)
		@Timeout(value = 10, unit = MILLISECONDS, threadMode = SAME_THREAD)
		void testZero() {
		}

		@Test
		@Order(2)
		@Timeout(value = 10, unit = MILLISECONDS, threadMode = SAME_THREAD)
		void testOne() {
		}
	}

	private static void blockUntilInterrupted() throws InterruptedException {
		new CountDownLatch(1).await();
	}
}
