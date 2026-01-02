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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

/**
 * Integration tests that verify support for {@link TestInstancePreDestroyCallback}.
 *
 * @since 5.6
 */
class TestInstancePreDestroyCallbackTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	@BeforeEach
	void resetCallSequence() {
		callSequence.clear();
	}

	@Test
	void instancePreDestroyCallbacksInNestedClasses() {
		executeTestsForClass(OuterTestCase.class).testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));

		// @formatter:off
		assertThat(callSequence).containsExactly(
			// OuterTestCase
			"beforeOuterMethod",
				"testOuter",
			"fooPreDestroyCallbackTestInstance:OuterTestCase",

			// InnerTestCase
			"beforeOuterMethod",
				"beforeInnerMethod",
					"testInner",
				"bazPreDestroyCallbackTestInstance:InnerTestCase",
				"barPreDestroyCallbackTestInstance:InnerTestCase",
			"fooPreDestroyCallbackTestInstance:InnerTestCase"
		);
		// @formatter:on
	}

	@Test
	void testSpecificTestInstancePreDestroyCallbackIsCalled() {
		executeTestsForClass(TestCaseWithTestSpecificTestInstancePreDestroyCallback.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"beforeEachMethod",
				"test",
			"fooPreDestroyCallbackTestInstance:TestCaseWithTestSpecificTestInstancePreDestroyCallback"
		);
		// @formatter:on
	}

	@Test
	void classLifecyclePreDestroyCallbacks() {
		executeTestsForClass(PerClassLifecyclePreDestroyCallbacksWithTwoTestMethods.class).testEvents()//
				.assertStatistics(stats -> stats.started(2).succeeded(2));

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"beforeEachMethod",
				"test1",
			"beforeEachMethod",
				"test2",
			"barPreDestroyCallbackTestInstance:PerClassLifecyclePreDestroyCallbacksWithTwoTestMethods",
			"fooPreDestroyCallbackTestInstance:PerClassLifecyclePreDestroyCallbacksWithTwoTestMethods"
		);
		// @formatter:on
	}

	// -------------------------------------------------------------------

	private abstract static class Destroyable {

		boolean destroyed;

		void setDestroyed() {
			this.destroyed = true;
		}
	}

	@ExtendWith(FooInstancePreDestroyCallback.class)
	static class OuterTestCase extends Destroyable {

		@BeforeEach
		void beforeOuterMethod() {
			callSequence.add("beforeOuterMethod");
		}

		@Test
		void testOuter() {
			assertFalse(destroyed);
			callSequence.add("testOuter");
		}

		@Nested
		@ExtendWith(BarInstancePreDestroyCallback.class)
		@ExtendWith(BazInstancePreDestroyCallback.class)
		class InnerTestCase extends Destroyable {

			@BeforeEach
			void beforeInnerMethod() {
				assertFalse(destroyed);
				callSequence.add("beforeInnerMethod");
			}

			@Test
			void testInner() {
				callSequence.add("testInner");
			}
		}
	}

	static class TestCaseWithTestSpecificTestInstancePreDestroyCallback extends Destroyable {

		@BeforeEach
		void beforeEachMethod() {
			assertFalse(destroyed);
			callSequence.add("beforeEachMethod");
		}

		@ExtendWith(FooInstancePreDestroyCallback.class)
		@Test
		void test() {
			callSequence.add("test");
		}
	}

	@SuppressWarnings("NewClassNamingConvention")
	@TestInstance(PER_CLASS)
	@ExtendWith(FooInstancePreDestroyCallback.class)
	@ExtendWith(BarInstancePreDestroyCallback.class)
	static class PerClassLifecyclePreDestroyCallbacksWithTwoTestMethods extends Destroyable {

		@BeforeEach
		void beforeEachMethod() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void test1() {
			callSequence.add("test1");
		}

		@Test
		void test2() {
			callSequence.add("test2");
		}
	}

	static abstract class AbstractTestInstancePreDestroyCallback implements TestInstancePreDestroyCallback {

		private final String name;

		AbstractTestInstancePreDestroyCallback(String name) {
			this.name = name;
		}

		@Override
		public void preDestroyTestInstance(ExtensionContext context) {
			assertThat(context.getTestInstance()).isPresent();
			Object testInstance = context.getTestInstance().get();
			if (testInstance instanceof Destroyable destroyable) {
				destroyable.setDestroyed();
			}
			callSequence.add(name + "PreDestroyCallbackTestInstance:" + testInstance.getClass().getSimpleName());
		}
	}

	static class FooInstancePreDestroyCallback extends AbstractTestInstancePreDestroyCallback {

		FooInstancePreDestroyCallback() {
			super("foo");
		}
	}

	static class BarInstancePreDestroyCallback extends AbstractTestInstancePreDestroyCallback {

		BarInstancePreDestroyCallback() {
			super("bar");
		}
	}

	static class BazInstancePreDestroyCallback extends AbstractTestInstancePreDestroyCallback {

		BazInstancePreDestroyCallback() {
			super("baz");
		}
	}

}
