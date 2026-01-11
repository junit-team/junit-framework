/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.testkit.engine.Event;

/**
 * Integration tests for {@link ResourceLocksProvider}.
 *
 * @since 5.12
 */
class ResourceLocksProviderTests extends AbstractJupiterTestEngineTests {

	@Test
	void classLevelProvider() {
		var events = execute(ClassLevelProviderTestCase.class);
		assertThat(events.filter(event(test(), finishedSuccessfully())::matches)).hasSize(2);
	}

	@Test
	void nestedClassLevelProvider() {
		var events = execute(NestedClassLevelProviderTestCase.class);
		assertThat(events.filter(event(test(), finishedSuccessfully())::matches)).hasSize(2);
	}

	@Test
	void methodLevelProvider() {
		var events = execute(MethodLevelProviderTestCase.class);
		assertThat(events.filter(event(test(), finishedSuccessfully())::matches)).hasSize(2);
	}

	@Test
	void methodLevelProviderInNestedClass() {
		var events = execute(MethodLevelProviderInNestedClassTestCase.class);
		assertThat(events.filter(event(test(), finishedSuccessfully())::matches)).hasSize(2);
	}

	@Test
	void providesAccessToRuntimeEnclosingInstances() {
		var events = execute(SubClassLevelProviderTestCase.class);
		assertThat(events.filter(event(test(), finishedSuccessfully())::matches)).hasSize(2);
	}

	private Stream<Event> execute(Class<?> testCase) {
		return executeTestsForClass(testCase).allEvents().stream();
	}

	// -------------------------------------------------------------------------

	@ResourceLock(providers = ClassLevelProviderTestCase.Provider.class)
	static class ClassLevelProviderTestCase {

		@Test
		void test() {
			assertTrue(Provider.isProvideForClassCalled, "'provideForClass' was not called");
			assertTrue(Provider.isProvideForTestMethodCalled, "'provideForMethod(test)' was not called");
		}

		@Nested
		class NestedClass {

			@Test
			void nestedTest() {
				assertTrue(Provider.isProvideForNestedClassCalled, "'provideForNestedClass' was not called");
				// @formatter:off
				assertTrue(
						Provider.isProvideForNestedTestMethodCalled,
						"'provideForMethod(nestedTest)' was not called"
				);
				// @formatter:on
			}
		}

		@AfterAll
		static void afterAll() {
			Provider.isProvideForClassCalled = false;
			Provider.isProvideForTestMethodCalled = false;

			Provider.isProvideForNestedClassCalled = false;
			Provider.isProvideForNestedTestMethodCalled = false;
		}

		static class Provider implements ResourceLocksProvider {

			private static boolean isProvideForClassCalled = false;
			private static boolean isProvideForTestMethodCalled = false;

			private static boolean isProvideForNestedClassCalled = false;
			private static boolean isProvideForNestedTestMethodCalled = false;

			@Nullable
			private Class<?> testClass;

			@Override
			public Set<Lock> provideForClass(Class<?> testClass) {
				this.testClass = testClass;
				isProvideForClassCalled = true;
				assertThat(testClass).isAssignableTo(ClassLevelProviderTestCase.class);
				return emptySet();
			}

			@Override
			public Set<Lock> provideForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> testClass) {
				isProvideForNestedClassCalled = true;
				assertEquals(List.of(requireNonNull(this.testClass)), enclosingInstanceTypes);
				assertEquals(ClassLevelProviderTestCase.NestedClass.class, testClass);
				return emptySet();
			}

			@Override
			public Set<Lock> provideForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
					Method testMethod) {
				if (ClassLevelProviderTestCase.class.isAssignableFrom(testClass)) {
					assertEquals(List.of(), enclosingInstanceTypes);
					assertEquals("test", testMethod.getName());
					isProvideForTestMethodCalled = true;
					return emptySet();
				}
				if (testClass == ClassLevelProviderTestCase.NestedClass.class) {
					assertEquals(List.of(this.testClass), enclosingInstanceTypes);
					assertEquals("nestedTest", testMethod.getName());
					isProvideForNestedTestMethodCalled = true;
					return emptySet();
				}
				fail("Unexpected test class: " + testClass);
				return emptySet();
			}
		}
	}

	static class SubClassLevelProviderTestCase extends ClassLevelProviderTestCase {
	}

	static class NestedClassLevelProviderTestCase {

		@Test
		void test() {
		}

		@Nested
		@ResourceLock(providers = NestedClassLevelProviderTestCase.Provider.class)
		class NestedClass {

			@Test
			void nestedTest() {
				assertTrue(Provider.isProvideForNestedClassCalled, "'provideForNestedClass' was not called");
				assertTrue(Provider.isProvideForMethodCalled, "'provideForMethod' was not called");
			}
		}

		@AfterAll
		static void afterAll() {
			Provider.isProvideForNestedClassCalled = false;
			Provider.isProvideForMethodCalled = false;
		}

		static class Provider implements ResourceLocksProvider {

			private static boolean isProvideForNestedClassCalled = false;

			private static boolean isProvideForMethodCalled = false;

			@Override
			public Set<Lock> provideForClass(Class<?> testClass) {
				fail("'provideForClass' should not be called");
				return emptySet();
			}

			@Override
			public Set<Lock> provideForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> testClass) {
				isProvideForNestedClassCalled = true;
				assertEquals(List.of(NestedClassLevelProviderTestCase.class), enclosingInstanceTypes);
				assertEquals(NestedClass.class, testClass);
				return emptySet();
			}

			@Override
			public Set<Lock> provideForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
					Method testMethod) {
				isProvideForMethodCalled = true;
				assertEquals(List.of(NestedClassLevelProviderTestCase.class), enclosingInstanceTypes);
				assertEquals(NestedClassLevelProviderTestCase.NestedClass.class, testClass);
				assertEquals("nestedTest", testMethod.getName());
				return emptySet();
			}
		}
	}

	static class MethodLevelProviderTestCase {

		@Test
		@ResourceLock(providers = MethodLevelProviderTestCase.Provider.class)
		void test() {
			assertTrue(Provider.isProvideForMethodCalled, "'provideForMethod' was not called");
		}

		@Nested
		class NestedClass {

			@Test
			void nestedTest() {
			}
		}

		@AfterAll
		static void afterAll() {
			Provider.isProvideForMethodCalled = false;
		}

		static class Provider implements ResourceLocksProvider {

			private static boolean isProvideForMethodCalled = false;

			@Override
			public Set<Lock> provideForClass(Class<?> testClass) {
				fail("'provideForClass' should not be called");
				return emptySet();
			}

			@Override
			public Set<Lock> provideForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> testClass) {
				fail("'provideForNestedClass' should not be called");
				return emptySet();
			}

			@Override
			public Set<Lock> provideForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
					Method testMethod) {
				isProvideForMethodCalled = true;
				assertEquals(List.of(), enclosingInstanceTypes);
				assertEquals(MethodLevelProviderTestCase.class, testClass);
				assertEquals("test", testMethod.getName());
				return emptySet();
			}
		}
	}

	static class MethodLevelProviderInNestedClassTestCase {

		@Test
		void test() {
		}

		@Nested
		class NestedClass {

			@Test
			@ResourceLock(providers = MethodLevelProviderInNestedClassTestCase.Provider.class)
			void nestedTest() {
				assertTrue(Provider.isProvideForMethodCalled, "'provideForMethod' was not called");
			}
		}

		@AfterAll
		static void afterAll() {
			Provider.isProvideForMethodCalled = false;
		}

		static class Provider implements ResourceLocksProvider {

			private static boolean isProvideForMethodCalled = false;

			@Override
			public Set<Lock> provideForClass(Class<?> testClass) {
				fail("'provideForClass' should not be called");
				return emptySet();
			}

			@Override
			public Set<Lock> provideForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> testClass) {
				fail("'provideForNestedClass' should not be called");
				return emptySet();
			}

			@Override
			public Set<Lock> provideForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
					Method testMethod) {
				isProvideForMethodCalled = true;
				assertEquals(List.of(MethodLevelProviderInNestedClassTestCase.class), enclosingInstanceTypes);
				assertEquals(MethodLevelProviderInNestedClassTestCase.NestedClass.class, testClass);
				assertEquals("nestedTest", testMethod.getName());
				return emptySet();
			}
		}
	}
}
