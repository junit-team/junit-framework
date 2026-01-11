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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.TEST_METHOD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

/**
 * Integration tests that verify support for {@link TestInstancePostProcessor}.
 *
 * @since 5.0
 */
class TestInstancePostProcessorTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	@BeforeEach
	void resetCallSequence() {
		callSequence.clear();
	}

	@Test
	void instancePostProcessorsInNestedClasses() {
		executeTestsForClass(OuterTestCase.class).testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));

		// @formatter:off
		assertThat(callSequence).containsExactly(

			// OuterTestCase
			"foo:OuterTestCase",
			"legacy:OuterTestCase",
				"beforeOuterMethod",
					"testOuter",
			"close:foo:OuterTestCase",

			// InnerTestCase

			"foo:OuterTestCase",
			"legacy:OuterTestCase",
			"foo:InnerTestCase",
			"legacy:InnerTestCase",
				"bar:InnerTestCase",
					"beforeOuterMethod",
						"beforeInnerMethod",
							"testInner",
				"close:bar:InnerTestCase",
			"close:foo:InnerTestCase",
			"close:foo:OuterTestCase",
			"close:legacy:InnerTestCase",
			"close:legacy:OuterTestCase",
			"close:legacy:OuterTestCase"
		);
		// @formatter:on
	}

	@Test
	void testSpecificTestInstancePostProcessorIsCalled() {
		executeTestsForClass(TestCaseWithTestSpecificTestInstancePostProcessor.class).testEvents()//
				.assertStatistics(stats -> stats.started(2).succeeded(2));

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"foo:TestCaseWithTestSpecificTestInstancePostProcessor",
			"legacy:TestCaseWithTestSpecificTestInstancePostProcessor",
				"beforeEachMethod",
					"test1",
			"close:foo:TestCaseWithTestSpecificTestInstancePostProcessor",
				"beforeEachMethod",
					"test2",
			"close:legacy:TestCaseWithTestSpecificTestInstancePostProcessor"
		);
		// @formatter:on
	}

	// -------------------------------------------------------------------

	@ExtendWith(FooInstancePostProcessor.class)
	@ExtendWith(LegacyInstancePostProcessor.class)
	static class OuterTestCase implements Named {

		private final Map<String, String> outerNames = new HashMap<>();

		@Override
		public void setName(String source, String name) {
			outerNames.put(source, name);
		}

		@BeforeEach
		void beforeOuterMethod() {
			callSequence.add("beforeOuterMethod");
		}

		@Test
		void testOuter() {
			assertEquals(
				Map.of("foo", OuterTestCase.class.getSimpleName(), "legacy", OuterTestCase.class.getSimpleName()),
				outerNames);
			callSequence.add("testOuter");
		}

		@Nested
		@ExtendWith(BarInstancePostProcessor.class)
		class InnerTestCase implements Named {

			private final Map<String, String> innerNames = new HashMap<>();

			@Override
			public void setName(String source, String name) {
				innerNames.put(source, name);
			}

			@BeforeEach
			void beforeInnerMethod() {
				callSequence.add("beforeInnerMethod");
			}

			@Test
			void testInner() {
				assertEquals(
					Map.of("foo", InnerTestCase.class.getSimpleName(), "legacy", OuterTestCase.class.getSimpleName()),
					outerNames);
				assertEquals(Map.of("foo", InnerTestCase.class.getSimpleName(), "bar",
					InnerTestCase.class.getSimpleName(), "legacy", InnerTestCase.class.getSimpleName()), innerNames);
				callSequence.add("testInner");
			}
		}

	}

	static class TestCaseWithTestSpecificTestInstancePostProcessor implements Named {

		private final Map<String, String> names = new HashMap<>();

		@Override
		public void setName(String source, String name) {
			names.put(source, name);
		}

		@BeforeEach
		void beforeEachMethod() {
			callSequence.add("beforeEachMethod");
		}

		@ExtendWith(FooInstancePostProcessor.class)
		@ExtendWith(LegacyInstancePostProcessor.class)
		@Test
		void test1() {
			callSequence.add("test1");
			assertEquals(Map.of("foo", getClass().getSimpleName(), "legacy", getClass().getSimpleName()), names);
		}

		@Test
		void test2() {
			callSequence.add("test2");
			assertEquals(Map.of(), names);
		}
	}

	static abstract class AbstractInstancePostProcessor implements TestInstancePostProcessor {
		private final String name;

		AbstractInstancePostProcessor(String name) {
			this.name = name;
		}

		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
			if (testInstance instanceof Named named) {
				named.setName(name, context.getRequiredTestClass().getSimpleName());
			}
			String instanceType = testInstance.getClass().getSimpleName();
			callSequence.add(name + ":" + instanceType);
			context.getStore(ExtensionContext.Namespace.create(this)).put(new Object(),
				(AutoCloseable) () -> callSequence.add("close:" + name + ":" + instanceType));
		}
	}

	static class FooInstancePostProcessor extends AbstractInstancePostProcessor {
		FooInstancePostProcessor() {
			super("foo");
		}

		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return TEST_METHOD;
		}
	}

	static class BarInstancePostProcessor extends AbstractInstancePostProcessor {
		BarInstancePostProcessor() {
			super("bar");
		}

		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return TEST_METHOD;
		}
	}

	static class LegacyInstancePostProcessor extends AbstractInstancePostProcessor {
		LegacyInstancePostProcessor() {
			super("legacy");
		}
	}

	private interface Named {

		void setName(String source, String name);
	}

}
