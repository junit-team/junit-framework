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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests that verify support for {@link BeforeEach}, {@link AfterEach},
 * {@link BeforeEachCallback}, and {@link AfterEachCallback} in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 * @see BeforeAndAfterTestExecutionCallbackTests
 */
class BeforeAndAfterEachTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();
	private static final List<String> beforeEachMethodCallSequence = new ArrayList<>();

	private static @Nullable Optional<Throwable> actualExceptionInAfterEachCallback;

	@SuppressWarnings("OptionalAssignedToNull")
	@BeforeEach
	void resetCallSequence() {
		callSequence.clear();
		beforeEachMethodCallSequence.clear();
		actualExceptionInAfterEachCallback = null;
	}

	@Test
	void beforeEachAndAfterEachCallbacks() {
		Events testEvents = executeTestsForClass(OuterTestCase.class).testEvents();

		assertEquals(2, testEvents.started().count(), "# tests started");
		assertEquals(2, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(0, testEvents.failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(

			// OuterTestCase
			"fooBeforeEachCallback",
			"barBeforeEachCallback",
				"beforeEachMethod",
					"testOuter",
				"afterEachMethod",
			"barAfterEachCallback",
			"fooAfterEachCallback",

			// InnerTestCase
			"fooBeforeEachCallback",
			"barBeforeEachCallback",
			"fizzBeforeEachCallback",
				"beforeEachMethod",
					"beforeEachInnerMethod",
						"testInner",
					"afterEachInnerMethod",
				"afterEachMethod",
			"fizzAfterEachCallback",
			"barAfterEachCallback",
			"fooAfterEachCallback"

			), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeEachAndAfterEachCallbacksDeclaredOnSuperclassAndSubclass() {
		Events testEvents = executeTestsForClass(ChildTestCase.class).testEvents();

		assertEquals(1, testEvents.started().count(), "# tests started");
		assertEquals(1, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(0, testEvents.failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"fooBeforeEachCallback",
			"barBeforeEachCallback",
				"testChild",
			"barAfterEachCallback",
			"fooAfterEachCallback"
		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeEachAndAfterEachCallbacksDeclaredOnInterfaceAndClass() {
		Events testEvents = executeTestsForClass(TestInterfaceTestCase.class).testEvents();

		assertEquals(2, testEvents.started().count(), "# tests started");
		assertEquals(2, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(0, testEvents.failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(

			// Test Interface
			"fooBeforeEachCallback",
			"barBeforeEachCallback",
				"defaultTestMethod",
			"barAfterEachCallback",
			"fooAfterEachCallback",

			// Test Class
			"fooBeforeEachCallback",
			"barBeforeEachCallback",
				"localTestMethod",
			"barAfterEachCallback",
			"fooAfterEachCallback"

		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeEachCallbackThrowsAnException() {
		Events testEvents = executeTestsForClass(ExceptionInBeforeEachCallbackTestCase.class).testEvents();

		assertEquals(1, testEvents.started().count(), "# tests started");
		assertEquals(0, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(1, testEvents.failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"fooBeforeEachCallback",
			"exceptionThrowingBeforeEachCallback", // throws an exception.
			// barBeforeEachCallback should not get invoked.
				// beforeEachMethod should not get invoked.
					// test should not get invoked.
				// afterEachMethod should not get invoked.
			"barAfterEachCallback",
			"fooAfterEachCallback"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertThat(actualExceptionInAfterEachCallback).containsInstanceOf(EnigmaException.class);
	}

	@Test
	void afterEachCallbackThrowsAnException() {
		Events testEvents = executeTestsForClass(ExceptionInAfterEachCallbackTestCase.class).testEvents();

		assertEquals(1, testEvents.started().count(), "# tests started");
		assertEquals(0, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(1, testEvents.failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"fooBeforeEachCallback",
			"barBeforeEachCallback",
				"beforeEachMethod",
					"test",
				"afterEachMethod",
			"barAfterEachCallback",
			"exceptionThrowingAfterEachCallback", // throws an exception.
			"fooAfterEachCallback"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertThat(actualExceptionInAfterEachCallback).containsInstanceOf(EnigmaException.class);
	}

	@Test
	void beforeEachMethodThrowsAnException() {
		Events testEvents = executeTestsForClass(ExceptionInBeforeEachMethodTestCase.class).testEvents();

		assertEquals(1, testEvents.started().count(), "# tests started");
		assertEquals(0, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(1, testEvents.failed().count(), "# tests failed");

		// Since the JVM does not guarantee the order in which methods are
		// returned via reflection (and since JUnit Jupiter does not yet
		// support ordering of @BeforeEach methods), we have to figure out
		// which @BeforeEach method got executed first in order to determine
		// the expected call sequence.

		// @formatter:off
		List<String> list1 = asList(
			"fooBeforeEachCallback",
				"beforeEachMethod1", // throws an exception.
				// "beforeEachMethod2" should not get invoked
					// test should not get invoked.
				"afterEachMethod",
			"fooAfterEachCallback"
		);
		List<String> list2 = asList(
			"fooBeforeEachCallback",
				"beforeEachMethod2",
				"beforeEachMethod1", // throws an exception.
					// test should not get invoked.
				"afterEachMethod",
			"fooAfterEachCallback"
		);
		// @formatter:on

		List<String> expected = "beforeEachMethod1".equals(beforeEachMethodCallSequence.getFirst()) ? list1 : list2;

		assertEquals(expected, callSequence, "wrong call sequence");

		assertThat(actualExceptionInAfterEachCallback).containsInstanceOf(EnigmaException.class);
	}

	@Test
	void afterEachMethodThrowsAnException() {
		Events testEvents = executeTestsForClass(ExceptionInAfterEachMethodTestCase.class).testEvents();

		assertEquals(1, testEvents.started().count(), "# tests started");
		assertEquals(0, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(1, testEvents.failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"fooBeforeEachCallback",
				"beforeEachMethod",
					"test",
				"afterEachMethod", // throws an exception.
			"fooAfterEachCallback"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertThat(actualExceptionInAfterEachCallback).containsInstanceOf(EnigmaException.class);
	}

	@Test
	void testMethodThrowsAnException() {
		Events testEvents = executeTestsForClass(ExceptionInTestMethodTestCase.class).testEvents();

		assertEquals(1, testEvents.started().count(), "# tests started");
		assertEquals(0, testEvents.succeeded().count(), "# tests succeeded");
		assertEquals(0, testEvents.skipped().count(), "# tests skipped");
		assertEquals(0, testEvents.aborted().count(), "# tests aborted");
		assertEquals(1, testEvents.failed().count(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"fooBeforeEachCallback",
				"beforeEachMethod",
					"test", // throws an exception.
				"afterEachMethod",
			"fooAfterEachCallback"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertThat(actualExceptionInAfterEachCallback).containsInstanceOf(EnigmaException.class);
	}

	// -------------------------------------------------------------------------

	@ExtendWith(FooMethodLevelCallbacks.class)
	static class ParentTestCase {
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(BarMethodLevelCallbacks.class)
	static class ChildTestCase extends ParentTestCase {

		@Test
		void test() {
			callSequence.add("testChild");
		}
	}

	@ExtendWith(FooMethodLevelCallbacks.class)
	private interface TestInterface {

		@Test
		default void defaultTest() {
			callSequence.add("defaultTestMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(BarMethodLevelCallbacks.class)
	static class TestInterfaceTestCase implements TestInterface {

		@Test
		void localTest() {
			callSequence.add("localTestMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooMethodLevelCallbacks.class, BarMethodLevelCallbacks.class })
	static class OuterTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void testOuter() {
			callSequence.add("testOuter");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}

		@Nested
		@ExtendWith(FizzMethodLevelCallbacks.class)
		class InnerTestCase {

			@BeforeEach
			void beforeEachInnerMethod() {
				callSequence.add("beforeEachInnerMethod");
			}

			@Test
			void testInner() {
				callSequence.add("testInner");
			}

			@AfterEach
			void afterEachInnerMethod() {
				callSequence.add("afterEachInnerMethod");
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooMethodLevelCallbacks.class, ExceptionThrowingBeforeEachCallback.class,
			BarMethodLevelCallbacks.class })
	static class ExceptionInBeforeEachCallbackTestCase {

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
	@ExtendWith({ FooMethodLevelCallbacks.class, ExceptionThrowingAfterEachCallback.class,
			BarMethodLevelCallbacks.class })
	static class ExceptionInAfterEachCallbackTestCase {

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
	@ExtendWith(FooMethodLevelCallbacks.class)
	static class ExceptionInBeforeEachMethodTestCase {

		@BeforeEach
		void beforeEach1() {
			beforeEachMethodCallSequence.add("beforeEachMethod1");
			callSequence.add("beforeEachMethod1");
			throw new EnigmaException("@BeforeEach");
		}

		@BeforeEach
		void beforeEach2() {
			beforeEachMethodCallSequence.add("beforeEachMethod2");
			callSequence.add("beforeEachMethod2");
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
	@ExtendWith(FooMethodLevelCallbacks.class)
	static class ExceptionInAfterEachMethodTestCase {

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
			throw new EnigmaException("@AfterEach");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooMethodLevelCallbacks.class)
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

	static class FooMethodLevelCallbacks implements BeforeEachCallback, AfterEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) {
			callSequence.add("fooBeforeEachCallback");
		}

		@Override
		public void afterEach(ExtensionContext context) {
			callSequence.add("fooAfterEachCallback");
			actualExceptionInAfterEachCallback = context.getExecutionException();
		}
	}

	static class BarMethodLevelCallbacks implements BeforeEachCallback, AfterEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) {
			callSequence.add("barBeforeEachCallback");
		}

		@Override
		public void afterEach(ExtensionContext context) {
			callSequence.add("barAfterEachCallback");
		}
	}

	static class FizzMethodLevelCallbacks implements BeforeEachCallback, AfterEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) {
			callSequence.add("fizzBeforeEachCallback");
		}

		@Override
		public void afterEach(ExtensionContext context) {
			callSequence.add("fizzAfterEachCallback");
		}
	}

	static class ExceptionThrowingBeforeEachCallback implements BeforeEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) {
			callSequence.add("exceptionThrowingBeforeEachCallback");
			throw new EnigmaException("BeforeEachCallback");
		}
	}

	static class ExceptionThrowingAfterEachCallback implements AfterEachCallback {

		@Override
		public void afterEach(ExtensionContext context) {
			callSequence.add("exceptionThrowingAfterEachCallback");
			throw new EnigmaException("AfterEachCallback");
		}
	}

}
