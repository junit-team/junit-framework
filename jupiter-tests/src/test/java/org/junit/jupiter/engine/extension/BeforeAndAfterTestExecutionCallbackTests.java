/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests that verify support for {@link BeforeTestExecutionCallback},
 * {@link AfterTestExecutionCallback}, {@link BeforeEach}, and {@link AfterEach}
 * in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 * @see BeforeAndAfterEachTests
 */
class BeforeAndAfterTestExecutionCallbackTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	private static @Nullable Optional<Throwable> actualExceptionInAfterTestExecution;

	@SuppressWarnings("OptionalAssignedToNull")
	@BeforeEach
	void resetCallSequence() {
		callSequence.clear();
		actualExceptionInAfterTestExecution = null;
	}

	@Test
	void beforeAndAfterTestExecutionCallbacks() {
		EngineExecutionResults executionResults = executeTestsForClass(OuterTestCase.class);

		assertEquals(2, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(2, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(

			// OuterTestCase
			"beforeEachMethodOuter",
				"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"testOuter",
				"barAfterTestExecutionCallback",
				"fooAfterTestExecutionCallback",
			"afterEachMethodOuter",

			// InnerTestCase
			"beforeEachMethodOuter",
				"beforeEachMethodInner",
					"fooBeforeTestExecutionCallback",
					"barBeforeTestExecutionCallback",
						"fizzBeforeTestExecutionCallback",
							"testInner",
						"fizzAfterTestExecutionCallback",
					"barAfterTestExecutionCallback",
					"fooAfterTestExecutionCallback",
				"afterEachMethodInner",
			"afterEachMethodOuter"

		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeAndAfterTestExecutionCallbacksDeclaredOnSuperclassAndSubclass() {
		EngineExecutionResults executionResults = executeTestsForClass(ChildTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"fooBeforeTestExecutionCallback",
			"barBeforeTestExecutionCallback",
				"testChild",
			"barAfterTestExecutionCallback",
			"fooAfterTestExecutionCallback"
		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeAndAfterTestExecutionCallbacksDeclaredOnInterfaceAndClass() {
		EngineExecutionResults executionResults = executeTestsForClass(TestInterfaceTestCase.class);

		assertEquals(2, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(2, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(

			// Test Interface
			"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"defaultTestMethod",
				"barAfterTestExecutionCallback",
			"fooAfterTestExecutionCallback",

			// Test Class
			"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"localTestMethod",
				"barAfterTestExecutionCallback",
			"fooAfterTestExecutionCallback"

		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeEachMethodThrowsAnException() {
		EngineExecutionResults executionResults = executeTestsForClass(ExceptionInBeforeEachMethodTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod", // throws an exception.
				// fooBeforeTestExecutionCallback should not get invoked.
					// test should not get invoked.
				// fooAfterTestExecutionCallback should not get invoked.
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNull(actualExceptionInAfterTestExecution,
			"test exception (fooAfterTestExecutionCallback should not have been called)");
	}

	@Test
	void beforeTestExecutionCallbackThrowsAnException() {
		EngineExecutionResults executionResults = executeTestsForClass(
			ExceptionInBeforeTestExecutionCallbackTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod",
				"fooBeforeTestExecutionCallback",
				"exceptionThrowingBeforeTestExecutionCallback", // throws an exception.
				// barBeforeTestExecutionCallback should not get invoked.
					// test() should not get invoked.
				"barAfterTestExecutionCallback",
				"fooAfterTestExecutionCallback",
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNotNull(actualExceptionInAfterTestExecution, "test exception");
		assertTrue(actualExceptionInAfterTestExecution.isPresent(), "test exception should be present");
		assertEquals(EnigmaException.class, actualExceptionInAfterTestExecution.get().getClass());
	}

	@Test
	void afterTestExecutionCallbackThrowsAnException() {
		EngineExecutionResults executionResults = executeTestsForClass(
			ExceptionInAfterTestExecutionCallbackTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod",
				"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"test",
				"barAfterTestExecutionCallback",
				"exceptionThrowingAfterTestExecutionCallback", // throws an exception.
				"fooAfterTestExecutionCallback",
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNotNull(actualExceptionInAfterTestExecution, "test exception");
		assertTrue(actualExceptionInAfterTestExecution.isPresent(), "test exception should be present");
		assertEquals(EnigmaException.class, actualExceptionInAfterTestExecution.get().getClass());
	}

	@Test
	void testMethodThrowsAnException() {
		EngineExecutionResults executionResults = executeTestsForClass(ExceptionInTestMethodTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod",
				"fooBeforeTestExecutionCallback",
					"test", // throws an exception.
				"fooAfterTestExecutionCallback",
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNotNull(actualExceptionInAfterTestExecution, "test exception");
		assertTrue(actualExceptionInAfterTestExecution.isPresent(), "test exception should be present");
		assertEquals(EnigmaException.class, actualExceptionInAfterTestExecution.get().getClass());
	}

	// -------------------------------------------------------------------------

	@ExtendWith(FooTestExecutionCallbacks.class)
	static class ParentTestCase {
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(BarTestExecutionCallbacks.class)
	static class ChildTestCase extends ParentTestCase {

		@Test
		void test() {
			callSequence.add("testChild");
		}
	}

	@ExtendWith(FooTestExecutionCallbacks.class)
	private interface TestInterface {

		@Test
		default void defaultTest() {
			callSequence.add("defaultTestMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(BarTestExecutionCallbacks.class)
	static class TestInterfaceTestCase implements TestInterface {

		@Test
		void localTest() {
			callSequence.add("localTestMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooTestExecutionCallbacks.class, BarTestExecutionCallbacks.class })
	static class OuterTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethodOuter");
		}

		@Test
		void testOuter() {
			callSequence.add("testOuter");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethodOuter");
		}

		@Nested
		@ExtendWith(FizzTestExecutionCallbacks.class)
		class InnerTestCase {

			@BeforeEach
			void beforeInnerMethod() {
				callSequence.add("beforeEachMethodInner");
			}

			@Test
			void testInner() {
				callSequence.add("testInner");
			}

			@AfterEach
			void afterInnerMethod() {
				callSequence.add("afterEachMethodInner");
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooTestExecutionCallbacks.class, ExceptionThrowingBeforeTestExecutionCallback.class,
			BarTestExecutionCallbacks.class })
	static class ExceptionInBeforeTestExecutionCallbackTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooTestExecutionCallbacks.class, ExceptionThrowingAfterTestExecutionCallback.class,
			BarTestExecutionCallbacks.class })
	static class ExceptionInAfterTestExecutionCallbackTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooTestExecutionCallbacks.class)
	static class ExceptionInBeforeEachMethodTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
			throw new EnigmaException("@BeforeEach");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooTestExecutionCallbacks.class)
	static class ExceptionInTestMethodTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void test() {
			callSequence.add("test");
			throw new EnigmaException("@Test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	// -------------------------------------------------------------------------

	static class FooTestExecutionCallbacks implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("fooBeforeTestExecutionCallback");
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("fooAfterTestExecutionCallback");
			actualExceptionInAfterTestExecution = context.getExecutionException();
		}
	}

	static class BarTestExecutionCallbacks implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("barBeforeTestExecutionCallback");
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("barAfterTestExecutionCallback");
		}
	}

	static class FizzTestExecutionCallbacks implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("fizzBeforeTestExecutionCallback");
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("fizzAfterTestExecutionCallback");
		}
	}

	static class ExceptionThrowingBeforeTestExecutionCallback implements BeforeTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("exceptionThrowingBeforeTestExecutionCallback");
			throw new EnigmaException("BeforeTestExecutionCallback");
		}
	}

	static class ExceptionThrowingAfterTestExecutionCallback implements AfterTestExecutionCallback {

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("exceptionThrowingAfterTestExecutionCallback");
			throw new EnigmaException("AfterTestExecutionCallback");
		}
	}

}
