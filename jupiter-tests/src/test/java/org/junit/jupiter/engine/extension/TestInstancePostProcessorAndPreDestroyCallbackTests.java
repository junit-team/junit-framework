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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;

/**
 * Integration tests that verify support for {@link TestInstancePostProcessor}
 * and {@link TestInstancePreDestroyCallback} in the {@link JupiterTestEngine}.
 *
 * @since 5.6
 */
class TestInstancePostProcessorAndPreDestroyCallbackTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	@Test
	void postProcessorAndPreDestroyCallbacks() {
		// @formatter:off
		assertPostProcessorAndPreDestroyCallbacks(TopLevelTestCase.class,
			"fooPostProcessTestInstance",
			"barPostProcessTestInstance",
				"test-1",
			"barPreDestroyTestInstance",
			"fooPreDestroyTestInstance"
		);
		// @formatter:on
	}

	@Test
	void postProcessorAndPreDestroyCallbacksInSubclass() {
		// @formatter:off
		assertPostProcessorAndPreDestroyCallbacks(SecondLevelTestCase.class,
			"fooPostProcessTestInstance",
			"barPostProcessTestInstance",
				"bazPostProcessTestInstance",
					"test-2",
				"bazPreDestroyTestInstance",
			"barPreDestroyTestInstance",
			"fooPreDestroyTestInstance"
		);
		// @formatter:on
	}

	@Test
	void postProcessorAndPreDestroyCallbacksInSubSubclass() {
		// @formatter:off
		assertPostProcessorAndPreDestroyCallbacks(ThirdLevelTestCase.class,
			"fooPostProcessTestInstance",
			"barPostProcessTestInstance",
				"bazPostProcessTestInstance",
					"quuxPostProcessTestInstance",
						"test-3",
					"quuxPreDestroyTestInstance",
				"bazPreDestroyTestInstance",
			"barPreDestroyTestInstance",
			"fooPreDestroyTestInstance"
		);
		// @formatter:on
	}

	@Test
	void preDestroyTestInstanceMethodThrowsAnException() {
		// @formatter:off
		assertPostProcessorAndPreDestroyCallbacks(ExceptionInTestInstancePreDestroyCallbackTestCase.class, 0,
			"fooPostProcessTestInstance",
				"test",
			"exceptionThrowingTestInstancePreDestroyCallback"
		);
		// @formatter:on
	}

	@Test
	void postProcessTestInstanceMethodThrowsAnException() {
		assertPostProcessorAndPreDestroyCallbacks(ExceptionInTestInstancePostProcessorTestCase.class, 0,
			"exceptionThrowingTestInstancePostProcessor", "exceptionPreDestroyTestInstance");
	}

	@Test
	void testClassConstructorThrowsAnException() {
		assertPostProcessorAndPreDestroyCallbacks(ExceptionInTestClassConstructorTestCase.class, 0,
			"exceptionThrowingConstructor");
	}

	private void assertPostProcessorAndPreDestroyCallbacks(Class<?> testClass, String... expectedCalls) {
		assertPostProcessorAndPreDestroyCallbacks(testClass, 1, expectedCalls);
	}

	private void assertPostProcessorAndPreDestroyCallbacks(Class<?> testClass, int testsSuccessful,
			String... expectedCalls) {

		callSequence.clear();

		executeTestsForClass(testClass).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(testsSuccessful));

		assertEquals(asList(expectedCalls), callSequence, () -> "wrong call sequence for " + testClass.getName());
	}

	// -------------------------------------------------------------------------

	// Must NOT be private; otherwise, the @Test method gets discovered but never executed.
	@ExtendWith({ FooTestInstanceCallbacks.class, BarTestInstanceCallbacks.class })
	static class TopLevelTestCase {

		@Test
		void test() {
			callSequence.add("test-1");
		}
	}

	// Must NOT be private; otherwise, the @Test method gets discovered but never executed.
	@ExtendWith(BazTestInstanceCallbacks.class)
	static class SecondLevelTestCase extends TopLevelTestCase {

		@Test
		@Override
		void test() {
			callSequence.add("test-2");
		}
	}

	@ExtendWith(QuuxTestInstanceCallbacks.class)
	static class ThirdLevelTestCase extends SecondLevelTestCase {

		@Test
		@Override
		void test() {
			callSequence.add("test-3");
		}
	}

	@ExtendWith(ExceptionThrowingTestInstancePreDestroyCallback.class)
	static class ExceptionInTestInstancePreDestroyCallbackTestCase {

		@Test
		void test() {
			callSequence.add("test");
		}
	}

	@ExtendWith(ExceptionThrowingTestInstancePostProcessor.class)
	static class ExceptionInTestInstancePostProcessorTestCase {

		@Test
		void test() {
			callSequence.add("test");
		}
	}

	@ExtendWith(FooTestInstanceCallbacks.class)
	static class ExceptionInTestClassConstructorTestCase {

		ExceptionInTestClassConstructorTestCase() {
			callSequence.add("exceptionThrowingConstructor");
			throw new RuntimeException("in constructor");
		}

		@Test
		void test() {
			callSequence.add("test");
		}
	}

	// -------------------------------------------------------------------------

	static class FooTestInstanceCallbacks extends AbstractTestInstanceCallbacks {

		protected FooTestInstanceCallbacks() {
			super("foo");
		}
	}

	static class BarTestInstanceCallbacks extends AbstractTestInstanceCallbacks {

		protected BarTestInstanceCallbacks() {
			super("bar");
		}
	}

	static class BazTestInstanceCallbacks extends AbstractTestInstanceCallbacks {

		protected BazTestInstanceCallbacks() {
			super("baz");
		}
	}

	static class QuuxTestInstanceCallbacks extends AbstractTestInstanceCallbacks {

		protected QuuxTestInstanceCallbacks() {
			super("quux");
		}
	}

	static class ExceptionThrowingTestInstancePreDestroyCallback extends AbstractTestInstanceCallbacks {

		protected ExceptionThrowingTestInstancePreDestroyCallback() {
			super("foo");
		}

		@Override
		public void preDestroyTestInstance(ExtensionContext context) {
			callSequence.add("exceptionThrowingTestInstancePreDestroyCallback");
			throw new EnigmaException("preDestroyTestInstance");
		}
	}

	static class ExceptionThrowingTestInstancePostProcessor extends AbstractTestInstanceCallbacks {

		protected ExceptionThrowingTestInstancePostProcessor() {
			super("exception");
		}

		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
			callSequence.add("exceptionThrowingTestInstancePostProcessor");
			throw new EnigmaException("postProcessTestInstance");
		}
	}

	private static abstract class AbstractTestInstanceCallbacks
			implements TestInstancePostProcessor, TestInstancePreDestroyCallback {

		private final String name;

		AbstractTestInstanceCallbacks(String name) {
			this.name = name;
		}

		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
			callSequence.add(name + "PostProcessTestInstance");
		}

		@Override
		public void preDestroyTestInstance(ExtensionContext context) {
			callSequence.add(name + "PreDestroyTestInstance");
		}
	}
}
