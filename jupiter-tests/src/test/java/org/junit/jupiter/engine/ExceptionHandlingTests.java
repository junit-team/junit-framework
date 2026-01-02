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

import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.suppressed;

import java.io.IOException;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Events;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/**
 * Integration tests that verify correct exception handling in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class ExceptionHandlingTests extends AbstractJupiterTestEngineTests {

	@Test
	void failureInTestMethodIsRegistered() {
		EngineExecutionResults executionResults = executeTests(selectMethod(FailureTestCase.class, "failingTest"));
		Events tests = executionResults.testEvents();

		tests.assertStatistics(stats -> stats.started(1).failed(1));

		tests.failed().assertEventsMatchExactly( //
			event(test("failingTest"),
				finishedWithFailure(instanceOf(AssertionFailedError.class), message("always fails"))));
	}

	@Test
	void uncheckedExceptionInTestMethodIsRegistered() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(FailureTestCase.class, "testWithUncheckedException"));
		Events tests = executionResults.testEvents();

		tests.assertStatistics(stats -> stats.started(1).failed(1));

		tests.failed().assertEventsMatchExactly( //
			event(test("testWithUncheckedException"),
				finishedWithFailure(instanceOf(RuntimeException.class), message("unchecked"))));
	}

	@Test
	void checkedExceptionInTestMethodIsRegistered() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(FailureTestCase.class, "testWithCheckedException"));
		Events tests = executionResults.testEvents();

		tests.assertStatistics(stats -> stats.started(1).failed(1));

		tests.failed().assertEventsMatchExactly( //
			event(test("testWithCheckedException"),
				finishedWithFailure(instanceOf(IOException.class), message("checked"))));
	}

	@Test
	void checkedExceptionInBeforeEachIsRegistered() {
		FailureTestCase.exceptionToThrowInBeforeEach = Optional.of(new IOException("checked"));

		EngineExecutionResults executionResults = executeTests(selectMethod(FailureTestCase.class, "succeedingTest"));
		Events tests = executionResults.testEvents();

		tests.assertStatistics(stats -> stats.started(1).failed(1));

		tests.failed().assertEventsMatchExactly(
			event(test("succeedingTest"), finishedWithFailure(instanceOf(IOException.class), message("checked"))));
	}

	@Test
	void checkedExceptionInAfterEachIsRegistered() {
		FailureTestCase.exceptionToThrowInAfterEach = Optional.of(new IOException("checked"));

		EngineExecutionResults executionResults = executeTests(selectMethod(FailureTestCase.class, "succeedingTest"));
		Events tests = executionResults.testEvents();

		tests.assertStatistics(stats -> stats.started(1).failed(1));

		tests.failed().assertEventsMatchExactly(
			event(test("succeedingTest"), finishedWithFailure(instanceOf(IOException.class), message("checked"))));
	}

	@Test
	void checkedExceptionInAfterEachIsSuppressedByExceptionInTest() {
		Class<?> testClass = FailureTestCase.class;

		FailureTestCase.exceptionToThrowInAfterEach = Optional.of(new IOException("checked"));

		EngineExecutionResults executionResults = executeTests(selectMethod(testClass, "testWithUncheckedException"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testWithUncheckedException"), started()), //
			event(test("testWithUncheckedException"), //
				finishedWithFailure( //
					instanceOf(RuntimeException.class), //
					message("unchecked"), //
					suppressed(0, instanceOf(IOException.class), message("checked")))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void exceptionInAfterEachTakesPrecedenceOverFailedAssumptionInTest() {
		FailureTestCase.exceptionToThrowInAfterEach = Optional.of(new IOException("checked"));

		EngineExecutionResults executionResults = executeTests(selectMethod(FailureTestCase.class, "abortedTest"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(FailureTestCase.class), started()), //
			event(test("abortedTest"), started()), //
			event(test("abortedTest"), //
				finishedWithFailure(instanceOf(IOException.class), message("checked"), //
					suppressed(0, instanceOf(TestAbortedException.class)))), //
			event(container(FailureTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void checkedExceptionInBeforeAllIsRegistered() {
		Class<?> testClass = FailureTestCase.class;

		FailureTestCase.exceptionToThrowInBeforeAll = Optional.of(new IOException("checked"));

		EngineExecutionResults executionResults = executeTests(selectMethod(testClass, "succeedingTest"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), finishedWithFailure(instanceOf(IOException.class), message("checked"))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void checkedExceptionInAfterAllIsRegistered() {
		Class<?> testClass = FailureTestCase.class;

		FailureTestCase.exceptionToThrowInAfterAll = Optional.of(new IOException("checked"));

		EngineExecutionResults executionResults = executeTests(selectMethod(testClass, "succeedingTest"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("succeedingTest"), started()), //
			event(test("succeedingTest"), finishedSuccessfully()), //
			event(container(testClass), finishedWithFailure(instanceOf(IOException.class), message("checked"))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void exceptionInAfterAllCallbackDoesNotHideExceptionInBeforeAllCallback() {
		Class<?> testClass = TestCaseWithThrowingBeforeAllAndAfterAllCallbacks.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);
		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), finishedWithFailure( //
				message("beforeAll callback"), //
				suppressed(0, message("afterAll callback")))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void exceptionsInConstructorAndAfterAllCallbackAreReportedWhenTestInstancePerMethodIsUsed() {
		Class<?> testClass = TestCaseWithInvalidConstructorAndThrowingAfterAllCallbackAndPerMethodLifecycle.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);
		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("test"), started()), //
			event(test("test"), finishedWithFailure(message("constructor"))), //
			event(container(testClass), finishedWithFailure(message("afterAll callback"))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void exceptionInConstructorPreventsExecutionOfAfterAllCallbacksWhenTestInstancePerClassIsUsed() {
		Class<?> testClass = TestCaseWithInvalidConstructorAndThrowingAfterAllCallbackAndPerClassLifecycle.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);
		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), finishedWithFailure(message("constructor"))),
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void failureInAfterAllTakesPrecedenceOverTestAbortedExceptionInBeforeAll() {
		FailureTestCase.exceptionToThrowInBeforeAll = Optional.of(new TestAbortedException("aborted"));
		FailureTestCase.exceptionToThrowInAfterAll = Optional.of(new IOException("checked"));

		EngineExecutionResults executionResults = executeTests(selectMethod(FailureTestCase.class, "succeedingTest"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(FailureTestCase.class), started()), //
			event(container(FailureTestCase.class),
				finishedWithFailure(instanceOf(IOException.class), message("checked"),
					suppressed(0, instanceOf(TestAbortedException.class), message("aborted")))), //
			event(engine(), finishedSuccessfully()));
	}

	@AfterEach
	void cleanUpExceptions() {
		FailureTestCase.exceptionToThrowInBeforeAll = Optional.empty();
		FailureTestCase.exceptionToThrowInAfterAll = Optional.empty();
		FailureTestCase.exceptionToThrowInBeforeEach = Optional.empty();
		FailureTestCase.exceptionToThrowInAfterEach = Optional.empty();
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	static class FailureTestCase {

		static Optional<Throwable> exceptionToThrowInBeforeAll = Optional.empty();
		static Optional<Throwable> exceptionToThrowInAfterAll = Optional.empty();
		static Optional<Throwable> exceptionToThrowInBeforeEach = Optional.empty();
		static Optional<Throwable> exceptionToThrowInAfterEach = Optional.empty();

		@BeforeAll
		static void beforeAll() throws Throwable {
			if (exceptionToThrowInBeforeAll.isPresent()) {
				throw exceptionToThrowInBeforeAll.get();
			}
		}

		@AfterAll
		static void afterAll() throws Throwable {
			if (exceptionToThrowInAfterAll.isPresent()) {
				throw exceptionToThrowInAfterAll.get();
			}
		}

		@BeforeEach
		void beforeEach() throws Throwable {
			if (exceptionToThrowInBeforeEach.isPresent()) {
				throw exceptionToThrowInBeforeEach.get();
			}
		}

		@AfterEach
		void afterEach() throws Throwable {
			if (exceptionToThrowInAfterEach.isPresent()) {
				throw exceptionToThrowInAfterEach.get();
			}
		}

		@Test
		void succeedingTest() {
		}

		@Test
		void failingTest() {
			Assertions.fail("always fails");
		}

		@Test
		void testWithUncheckedException() {
			throw new RuntimeException("unchecked");
		}

		@Test
		void testWithCheckedException() throws IOException {
			throw new IOException("checked");
		}

		@Test
		void abortedTest() {
			abort("abortedTest");
		}

	}

	@TestInstance(PER_METHOD)
	@ExtendWith(ThrowingAfterAllCallback.class)
	static class TestCaseWithInvalidConstructorAndThrowingAfterAllCallbackAndPerMethodLifecycle {
		TestCaseWithInvalidConstructorAndThrowingAfterAllCallbackAndPerMethodLifecycle() {
			throw new IllegalStateException("constructor");
		}

		@Test
		void test() {
		}

	}

	@TestInstance(PER_CLASS)
	@ExtendWith(ThrowingAfterAllCallback.class)
	static class TestCaseWithInvalidConstructorAndThrowingAfterAllCallbackAndPerClassLifecycle {
		TestCaseWithInvalidConstructorAndThrowingAfterAllCallbackAndPerClassLifecycle() {
			throw new IllegalStateException("constructor");
		}

		@Test
		void test() {
		}
	}

	@ExtendWith(ThrowingBeforeAllCallback.class)
	@ExtendWith(ThrowingAfterAllCallback.class)
	static class TestCaseWithThrowingBeforeAllAndAfterAllCallbacks {
		@Test
		void test() {
		}
	}

	@NullMarked
	static class ThrowingBeforeAllCallback implements BeforeAllCallback {
		@Override
		public void beforeAll(ExtensionContext context) {
			throw new IllegalStateException("beforeAll callback");
		}
	}

	@NullMarked
	static class ThrowingAfterAllCallback implements AfterAllCallback {
		@Override
		public void afterAll(ExtensionContext context) {
			throw new IllegalStateException("afterAll callback");
		}
	}

}
