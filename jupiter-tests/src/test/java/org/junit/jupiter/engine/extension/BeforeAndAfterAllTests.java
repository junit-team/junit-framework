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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;

/**
 * Integration tests that verify support for {@link BeforeAll}, {@link AfterAll},
 * {@link BeforeAllCallback}, and {@link AfterAllCallback} in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class BeforeAndAfterAllTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	private static @Nullable Optional<Throwable> actualExceptionInAfterAllCallback;

	@Test
	void beforeAllAndAfterAllCallbacks() {
		// @formatter:off
		assertBeforeAllAndAfterAllCallbacks(TopLevelTestCase.class,
			"fooBeforeAllCallback",
			"barBeforeAllCallback",
				"beforeAllMethod-1",
					"test-1",
				"afterAllMethod-1",
			"barAfterAllCallback",
			"fooAfterAllCallback"
		);
		// @formatter:on

		assertThat(actualExceptionInAfterAllCallback).isEmpty();
	}

	@Test
	void beforeAllAndAfterAllCallbacksInSubclass() {
		// @formatter:off
		assertBeforeAllAndAfterAllCallbacks(SecondLevelTestCase.class,
			"fooBeforeAllCallback",
			"barBeforeAllCallback",
				"bazBeforeAllCallback",
					"beforeAllMethod-1",
						"beforeAllMethod-2",
							"test-2",
						"afterAllMethod-2",
					"afterAllMethod-1",
				"bazAfterAllCallback",
			"barAfterAllCallback",
			"fooAfterAllCallback"
		);
		// @formatter:on

		assertThat(actualExceptionInAfterAllCallback).isEmpty();
	}

	@Test
	void beforeAllAndAfterAllCallbacksInSubSubclass() {
		// @formatter:off
		assertBeforeAllAndAfterAllCallbacks(ThirdLevelTestCase.class,
			"fooBeforeAllCallback",
			"barBeforeAllCallback",
				"bazBeforeAllCallback",
					"quuxBeforeAllCallback",
						"beforeAllMethod-1",
							"beforeAllMethod-2",
								"beforeAllMethod-3",
									"test-3",
								"afterAllMethod-3",
							"afterAllMethod-2",
						"afterAllMethod-1",
					"quuxAfterAllCallback",
				"bazAfterAllCallback",
			"barAfterAllCallback",
			"fooAfterAllCallback"
		);
		// @formatter:on

		assertThat(actualExceptionInAfterAllCallback).isEmpty();
	}

	/**
	 * Since static methods cannot be overridden, "static hiding" no longer occurs since 5.11.
	 */
	@Test
	void beforeAllAndAfterAllCallbacksInSubSubclassWithoutStaticMethodHiding() {
		// NOTE: both the @BeforeAll AND the @AfterAll methods in level 3 are
		// executed as 1/2/3 due to the "stable" method sort order on the Platform.

		// @formatter:off
		assertBeforeAllAndAfterAllCallbacks(ThirdLevelStaticHidingTestCase.class,
			"fooBeforeAllCallback",
			"barBeforeAllCallback",
				"bazBeforeAllCallback",
					"quuxBeforeAllCallback",
						"beforeAllMethod-1",
							"beforeAllMethod-2",
								"beforeAllMethod-1-level3",
								"beforeAllMethod-2-level3",
								"beforeAllMethod-3-level3",
									"test-3",
								"afterAllMethod-1-level3",
								"afterAllMethod-2-level3",
								"afterAllMethod-3-level3",
							"afterAllMethod-2",
						"afterAllMethod-1",
					"quuxAfterAllCallback",
				"bazAfterAllCallback",
			"barAfterAllCallback",
			"fooAfterAllCallback"
		);
		// @formatter:on

		assertThat(actualExceptionInAfterAllCallback).isEmpty();
	}

	@Test
	void beforeAllMethodThrowsAnException() {
		// @formatter:off
		assertBeforeAllAndAfterAllCallbacks(ExceptionInBeforeAllMethodTestCase.class, 0, 0,
			"fooBeforeAllCallback",
				"beforeAllMethod", // throws an exception.
					// test should not get invoked.
				"afterAllMethod",
			"fooAfterAllCallback"
		);
		// @formatter:on

		assertThat(actualExceptionInAfterAllCallback).containsInstanceOf(EnigmaException.class);
	}

	@Test
	void beforeAllCallbackThrowsAnException() {
		// @formatter:off
		assertBeforeAllAndAfterAllCallbacks(ExceptionInBeforeAllCallbackTestCase.class, 0, 0,
			"fooBeforeAllCallback",
			"exceptionThrowingBeforeAllCallback", // throws an exception.
				// beforeAllMethod should not get invoked.
					// test should not get invoked.
				// afterAllMethod should not get invoked.
			"fooAfterAllCallback"
		);
		// @formatter:on

		assertThat(actualExceptionInAfterAllCallback).containsInstanceOf(EnigmaException.class);
	}

	private void assertBeforeAllAndAfterAllCallbacks(Class<?> testClass, String... expectedCalls) {
		assertBeforeAllAndAfterAllCallbacks(testClass, 1, 1, expectedCalls);
	}

	private void assertBeforeAllAndAfterAllCallbacks(Class<?> testClass, int testsStarted, int testsSuccessful,
			String... expectedCalls) {

		callSequence.clear();

		executeTestsForClass(testClass).testEvents()//
				.assertStatistics(stats -> stats.started(testsStarted).succeeded(testsSuccessful));

		assertEquals(asList(expectedCalls), callSequence, () -> "wrong call sequence for " + testClass.getName());
	}

	// -------------------------------------------------------------------------

	// Must NOT be private; otherwise, the @Test method gets discovered but never executed.
	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooClassLevelCallbacks.class, BarClassLevelCallbacks.class })
	static class TopLevelTestCase {

		@BeforeAll
		static void beforeAll1() {
			callSequence.add("beforeAllMethod-1");
		}

		@AfterAll
		static void afterAll1() {
			callSequence.add("afterAllMethod-1");
		}

		@Test
		void test() {
			callSequence.add("test-1");
		}
	}

	// Must NOT be private; otherwise, the @Test method gets discovered but never executed.
	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(BazClassLevelCallbacks.class)
	static class SecondLevelTestCase extends TopLevelTestCase {

		@BeforeAll
		static void beforeAll2() {
			callSequence.add("beforeAllMethod-2");
		}

		@AfterAll
		static void afterAll2() {
			callSequence.add("afterAllMethod-2");
		}

		@Test
		@Override
		void test() {
			callSequence.add("test-2");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(QuuxClassLevelCallbacks.class)
	static class ThirdLevelTestCase extends SecondLevelTestCase {

		@BeforeAll
		static void beforeAll3() {
			callSequence.add("beforeAllMethod-3");
		}

		@AfterAll
		static void afterAll3() {
			callSequence.add("afterAllMethod-3");
		}

		@Test
		@Override
		void test() {
			callSequence.add("test-3");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(QuuxClassLevelCallbacks.class)
	static class ThirdLevelStaticHidingTestCase extends SecondLevelTestCase {

		@BeforeAll
		static void beforeAll1() {
			callSequence.add("beforeAllMethod-1-level3");
		}

		@BeforeAll
		static void beforeAll2() {
			callSequence.add("beforeAllMethod-2-level3");
		}

		@BeforeAll
		static void beforeAll3() {
			callSequence.add("beforeAllMethod-3-level3");
		}

		@AfterAll
		static void afterAll1() {
			callSequence.add("afterAllMethod-1-level3");
		}

		@AfterAll
		static void afterAll2() {
			callSequence.add("afterAllMethod-2-level3");
		}

		@AfterAll
		static void afterAll3() {
			callSequence.add("afterAllMethod-3-level3");
		}

		@Test
		@Override
		void test() {
			callSequence.add("test-3");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooClassLevelCallbacks.class)
	static class ExceptionInBeforeAllMethodTestCase {

		@BeforeAll
		static void beforeAll() {
			callSequence.add("beforeAllMethod");
			throw new EnigmaException("@BeforeAll");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterAll
		static void afterAll() {
			callSequence.add("afterAllMethod");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooClassLevelCallbacks.class, ExceptionThrowingBeforeAllCallback.class })
	static class ExceptionInBeforeAllCallbackTestCase {

		@BeforeAll
		static void beforeAll() {
			callSequence.add("beforeAllMethod");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterAll
		static void afterAll() {
			callSequence.add("afterAllMethod");
		}
	}

	// -------------------------------------------------------------------------

	static class FooClassLevelCallbacks implements BeforeAllCallback, AfterAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			callSequence.add("fooBeforeAllCallback");
		}

		@Override
		public void afterAll(ExtensionContext context) {
			callSequence.add("fooAfterAllCallback");
			actualExceptionInAfterAllCallback = context.getExecutionException();
		}
	}

	static class BarClassLevelCallbacks implements BeforeAllCallback, AfterAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			callSequence.add("barBeforeAllCallback");
		}

		@Override
		public void afterAll(ExtensionContext context) {
			callSequence.add("barAfterAllCallback");
		}
	}

	static class BazClassLevelCallbacks implements BeforeAllCallback, AfterAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			callSequence.add("bazBeforeAllCallback");
		}

		@Override
		public void afterAll(ExtensionContext context) {
			callSequence.add("bazAfterAllCallback");
		}
	}

	static class QuuxClassLevelCallbacks implements BeforeAllCallback, AfterAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			callSequence.add("quuxBeforeAllCallback");
		}

		@Override
		public void afterAll(ExtensionContext context) {
			callSequence.add("quuxAfterAllCallback");
		}
	}

	static class ExceptionThrowingBeforeAllCallback implements BeforeAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			callSequence.add("exceptionThrowingBeforeAllCallback");
			throw new EnigmaException("BeforeAllCallback");
		}
	}

}
