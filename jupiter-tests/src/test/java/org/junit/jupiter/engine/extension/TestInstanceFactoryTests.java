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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.TEST_METHOD;
import static org.junit.platform.commons.util.ClassUtils.nullSafeToString;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.nestedContainer;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.Constants;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.test.TestClassLoader;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests that verify support for {@link TestInstanceFactory}.
 *
 * @since 5.3
 */
class TestInstanceFactoryTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	@BeforeEach
	void resetCallSequence() {
		callSequence.clear();
	}

	@Test
	void multipleFactoriesRegisteredOnSingleTestClass() {
		Class<?> testClass = MultipleFactoriesRegisteredOnSingleTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(0, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message("The following TestInstanceFactory extensions were registered for test class ["
							+ testClass.getName() + "], but only one is permitted: "
							+ nullSafeToString(FooInstanceFactory.class, BarInstanceFactory.class)))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void multipleFactoriesRegisteredWithinTestClassHierarchy() {
		Class<?> testClass = MultipleFactoriesRegisteredWithinClassHierarchyTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(0, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message("The following TestInstanceFactory extensions were registered for test class ["
							+ testClass.getName() + "], but only one is permitted: "
							+ nullSafeToString(FooInstanceFactory.class, BarInstanceFactory.class)))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void multipleFactoriesRegisteredWithinNestedClassStructure() {
		Class<?> outerClass = MultipleFactoriesRegisteredWithinNestedClassStructureTestCase.class;
		Class<?> nestedClass = MultipleFactoriesRegisteredWithinNestedClassStructureTestCase.InnerTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(outerClass);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(outerClass), started()), //
			event(test("outerTest()"), started()), //
			event(test("outerTest()"), finishedSuccessfully()), //
			event(nestedContainer(nestedClass), started()), //
			event(nestedContainer(nestedClass),
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message("The following TestInstanceFactory extensions were registered for test class ["
							+ nestedClass.getName() + "], but only one is permitted: "
							+ nullSafeToString(FooInstanceFactory.class, BarInstanceFactory.class)))), //
			event(container(outerClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nullTestInstanceFactoryWithPerMethodLifecycle() {
		Class<?> testClass = NullTestInstanceFactoryTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testShouldNotBeCalled"), started()), //
			event(test("testShouldNotBeCalled"),
				finishedWithFailure(instanceOf(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + NullTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [null].")))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nullTestInstanceFactoryWithPerClassLifecycle() {
		Class<?> testClass = PerClassLifecycleNullTestInstanceFactoryTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(0, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(instanceOf(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + NullTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [null].")))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void bogusTestInstanceFactoryWithPerMethodLifecycle() {
		Class<?> testClass = BogusTestInstanceFactoryTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testShouldNotBeCalled"), started()), //
			event(test("testShouldNotBeCalled"),
				finishedWithFailure(instanceOf(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + BogusTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [java.lang.String].")))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void bogusTestInstanceFactoryWithPerClassLifecycle() {
		Class<?> testClass = PerClassLifecycleBogusTestInstanceFactoryTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(0, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(instanceOf(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + BogusTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [java.lang.String].")))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void explosiveTestInstanceFactoryWithPerMethodLifecycle() {
		Class<?> testClass = ExplosiveTestInstanceFactoryTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testShouldNotBeCalled"), started()), //
			event(test("testShouldNotBeCalled"),
				finishedWithFailure(instanceOf(TestInstantiationException.class),
					message("TestInstanceFactory [" + ExplosiveTestInstanceFactory.class.getName()
							+ "] failed to instantiate test class [" + testClass.getName() + "]: boom!"))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void explosiveTestInstanceFactoryWithPerClassLifecycle() {
		Class<?> testClass = PerClassLifecycleExplosiveTestInstanceFactoryTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(0, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), //
				finishedWithFailure(instanceOf(TestInstantiationException.class),
					message("TestInstanceFactory [" + ExplosiveTestInstanceFactory.class.getName()
							+ "] failed to instantiate test class [" + testClass.getName() + "]: boom!"))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void proxyTestInstanceFactoryFailsDueToUseOfDifferentClassLoader() {
		Class<?> testClass = ProxiedTestCase.class;
		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertEquals(0, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests aborted");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), //
				// NOTE: the test class names are the same even though the objects are
				// instantiated using different ClassLoaders. Thus, we check for the
				// appended "@" but ignore the actual hash code for the test class
				// loaded by the different ClassLoader.
				finishedWithFailure(instanceOf(TestInstantiationException.class),
					message(m -> m.startsWith("TestInstanceFactory [" + ProxyTestInstanceFactory.class.getName() + "]")
							&& m.contains("failed to return an instance of [" + testClass.getName() + "@"
									+ Integer.toHexString(System.identityHashCode(testClass)))
							&& m.contains("and instead returned an instance of [" + testClass.getName() + "@")//
					))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void instanceFactoryOnTopLevelTestClass() {
		EngineExecutionResults executionResults = executeTestsForClass(ParentTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"FooInstanceFactory instantiated: ParentTestCase",
				"parentTest",
			"close ParentTestCase"
		);
		// @formatter:on
	}

	@Test
	void instanceFactorySupportedWhenTestClassDeclaresMultipleConstructors() {
		executeTestsForClass(MultipleConstructorsTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"MultipleConstructorsTestInstanceFactory instantiated: MultipleConstructorsTestCase",
				"test: 42"
		);
		// @formatter:on
	}

	@Test
	void inheritedFactoryInTestClassHierarchy() {
		EngineExecutionResults executionResults = executeTestsForClass(InheritedFactoryTestCase.class);

		assertEquals(2, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(2, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"FooInstanceFactory instantiated: InheritedFactoryTestCase",
				"parentTest",
			"close InheritedFactoryTestCase",
			"FooInstanceFactory instantiated: InheritedFactoryTestCase",
				"childTest",
			"close InheritedFactoryTestCase"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoriesInNestedClassStructureAreInherited() {
		EngineExecutionResults executionResults = executeTestsForClass(OuterTestCase.class);

		assertEquals(3, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(3, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(

			// OuterTestCase
			"FooInstanceFactory instantiated: OuterTestCase",
				"outerTest",
			"close OuterTestCase",

			// InnerTestCase
			"FooInstanceFactory instantiated: OuterTestCase",
				"FooInstanceFactory instantiated: InnerTestCase",
					"innerTest1",
				"close InnerTestCase",
			"close OuterTestCase",

			// InnerInnerTestCase
			"FooInstanceFactory instantiated: OuterTestCase",
				"FooInstanceFactory instantiated: InnerTestCase",
					"FooInstanceFactory instantiated: InnerInnerTestCase",
						"innerTest2",
					"close InnerInnerTestCase",
				"close InnerTestCase",
			"close OuterTestCase"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryRegisteredViaTestInterface() {
		EngineExecutionResults executionResults = executeTestsForClass(FactoryFromInterfaceTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"FooInstanceFactory instantiated: FactoryFromInterfaceTestCase",
				"test",
			"close FactoryFromInterfaceTestCase"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryRegisteredAsLambdaExpression() {
		EngineExecutionResults executionResults = executeTestsForClass(LambdaFactoryTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"beforeEach: lambda",
				"test: lambda"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryWithPerClassLifecycle() {
		EngineExecutionResults executionResults = executeTestsForClass(PerClassLifecycleTestCase.class);

		assertEquals(1, PerClassLifecycleTestCase.counter.get());

		assertEquals(2, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(2, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"FooInstanceFactory instantiated: PerClassLifecycleTestCase",
				"@BeforeAll",
					"@BeforeEach",
						"test1",
					"@BeforeEach",
						"test2",
				"@AfterAll",
			"close PerClassLifecycleTestCase"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryWithLegacyContext() {
		EngineExecutionResults executionResults = executeTestsForClass(LegacyContextTestCase.class);

		assertEquals(3, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(3, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
				"LegacyInstanceFactory instantiated: LegacyContextTestCase",
				"outerTest",
				"LegacyInstanceFactory instantiated: LegacyContextTestCase",
				"LegacyInstanceFactory instantiated: InnerTestCase",
				"innerTest1",
				"LegacyInstanceFactory instantiated: LegacyContextTestCase",
				"LegacyInstanceFactory instantiated: InnerTestCase",
				"innerTest2",
				"close InnerTestCase",
				"close InnerTestCase",
				"close LegacyContextTestCase",
				"close LegacyContextTestCase",
				"close LegacyContextTestCase"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryWithLegacyContextAndChangedDefaultScope() {
		var executionResults = executeTests(request() //
				.selectors(selectClass(LegacyContextTestCase.class)) //
				.configurationParameter(
					Constants.DEFAULT_TEST_CLASS_INSTANCE_CONSTRUCTION_EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME,
					TEST_METHOD.name()));

		assertEquals(3, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(3, executionResults.testEvents().succeeded().count(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
				"LegacyInstanceFactory instantiated: LegacyContextTestCase",
				"outerTest",
				"close LegacyContextTestCase",
				"LegacyInstanceFactory instantiated: LegacyContextTestCase",
				"LegacyInstanceFactory instantiated: InnerTestCase",
				"innerTest1",
				"close InnerTestCase",
				"close LegacyContextTestCase",
				"LegacyInstanceFactory instantiated: LegacyContextTestCase",
				"LegacyInstanceFactory instantiated: InnerTestCase",
				"innerTest2",
				"close InnerTestCase",
				"close LegacyContextTestCase"
		);
		// @formatter:on
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooInstanceFactory.class, BarInstanceFactory.class })
	static class MultipleFactoriesRegisteredOnSingleTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(NullTestInstanceFactory.class)
	static class NullTestInstanceFactoryTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@TestInstance(PER_CLASS)
	static class PerClassLifecycleNullTestInstanceFactoryTestCase extends NullTestInstanceFactoryTestCase {
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(BogusTestInstanceFactory.class)
	static class BogusTestInstanceFactoryTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@TestInstance(PER_CLASS)
	static class PerClassLifecycleBogusTestInstanceFactoryTestCase extends BogusTestInstanceFactoryTestCase {
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(ExplosiveTestInstanceFactory.class)
	static class ExplosiveTestInstanceFactoryTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@TestInstance(PER_CLASS)
	static class PerClassLifecycleExplosiveTestInstanceFactoryTestCase extends ExplosiveTestInstanceFactoryTestCase {
	}

	private static class MultipleConstructorsTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			instantiated(getClass(), factoryContext.getTestClass());
			return new MultipleConstructorsTestCase(42);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(MultipleConstructorsTestInstanceFactory.class)
	static class MultipleConstructorsTestCase {

		private final int number;

		MultipleConstructorsTestCase(String text) {
			this.number = -1;
		}

		MultipleConstructorsTestCase(int number) {
			this.number = number;
		}

		@Test
		void test() {
			callSequence.add("test: " + this.number);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooInstanceFactory.class)
	static class ParentTestCase {

		@Test
		void parentTest() {
			callSequence.add("parentTest");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InheritedFactoryTestCase extends ParentTestCase {

		@Test
		void childTest() {
			callSequence.add("childTest");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(BarInstanceFactory.class)
	static class MultipleFactoriesRegisteredWithinClassHierarchyTestCase extends ParentTestCase {

		@Test
		void childTest() {
			callSequence.add("childTest");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooInstanceFactory.class)
	static class OuterTestCase {

		@Test
		void outerTest() {
			callSequence.add("outerTest");
		}

		@Nested
		class InnerTestCase {

			@Test
			void innerTest1() {
				callSequence.add("innerTest1");
			}

			@Nested
			class InnerInnerTestCase {

				@Test
				void innerTest2() {
					callSequence.add("innerTest2");
				}
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooInstanceFactory.class)
	static class MultipleFactoriesRegisteredWithinNestedClassStructureTestCase {

		@Test
		void outerTest() {
		}

		@Nested
		@ExtendWith(BarInstanceFactory.class)
		class InnerTestCase {

			@Test
			void innerTest() {
			}
		}
	}

	@ExtendWith(FooInstanceFactory.class)
	interface TestInterface {
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryFromInterfaceTestCase implements TestInterface {

		@Test
		void test() {
			callSequence.add("test");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class LambdaFactoryTestCase {

		private final String text;

		@RegisterExtension
		static final TestInstanceFactory factory = (__, ___) -> new LambdaFactoryTestCase("lambda");

		LambdaFactoryTestCase(String text) {
			this.text = text;
		}

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEach: " + this.text);
		}

		@Test
		void test() {
			callSequence.add("test: " + this.text);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(FooInstanceFactory.class)
	@TestInstance(PER_CLASS)
	static class PerClassLifecycleTestCase {

		static final AtomicInteger counter = new AtomicInteger();

		PerClassLifecycleTestCase() {
			counter.incrementAndGet();
		}

		@BeforeAll
		void beforeAll() {
			callSequence.add("@BeforeAll");
		}

		@BeforeEach
		void beforeEach() {
			callSequence.add("@BeforeEach");
		}

		@Test
		void test1() {
			callSequence.add("test1");
		}

		@Test
		void test2() {
			callSequence.add("test2");
		}

		@AfterAll
		void afterAll() {
			callSequence.add("@AfterAll");
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(LegacyInstanceFactory.class)
	static class LegacyContextTestCase {

		@Test
		void outerTest() {
			callSequence.add("outerTest");
		}

		@Nested
		class InnerTestCase {

			@Test
			void innerTest1() {
				callSequence.add("innerTest1");
			}

			@Test
			void innerTest2() {
				callSequence.add("innerTest2");
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(ProxyTestInstanceFactory.class)
	@TestInstance(PER_CLASS)
	static class ProxiedTestCase {

		@Test
		void test1() {
			callSequence.add("test1");
		}

		@Test
		void test2() {
			callSequence.add("test2");
		}
	}

	// -------------------------------------------------------------------------

	private static abstract class AbstractTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			Class<?> testClass = factoryContext.getTestClass();
			instantiated(getClass(), testClass);

			extensionContext.getStore(ExtensionContext.Namespace.create(this)).put(new Object(),
				(AutoCloseable) () -> callSequence.add("close " + testClass.getSimpleName()));

			if (factoryContext.getOuterInstance().isPresent()) {
				return ReflectionSupport.newInstance(testClass, factoryContext.getOuterInstance().get());
			}
			// else
			return ReflectionSupport.newInstance(testClass);
		}
	}

	private static class FooInstanceFactory extends AbstractTestInstanceFactory {
		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return TEST_METHOD;
		}
	}

	private static class BarInstanceFactory extends AbstractTestInstanceFactory {
		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return TEST_METHOD;
		}
	}

	private static class LegacyInstanceFactory extends AbstractTestInstanceFactory {
	}

	/**
	 * {@link TestInstanceFactory} that returns null.
	 */
	private static class NullTestInstanceFactory implements TestInstanceFactory {

		@SuppressWarnings("DataFlowIssue")
		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			return null;
		}
	}

	/**
	 * {@link TestInstanceFactory} that returns an object of a type that does
	 * not match the supplied test class.
	 */
	private static class BogusTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			return "bogus";
		}
	}

	/**
	 * {@link TestInstanceFactory} that always throws an exception.
	 */
	private static class ExplosiveTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			throw new RuntimeException("boom!");
		}
	}

	/**
	 * This does not actually create a proxy. Rather, it simulates what
	 * a proxy-based implementation might do, by loading the class from a
	 * different {@link ClassLoader}.
	 */
	private static class ProxyTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			Class<?> testClass = factoryContext.getTestClass();
			String className = testClass.getName();

			instantiated(getClass(), testClass);

			try (var testClassLoader = TestClassLoader.forClasses(testClass)) {
				// Load test class from different class loader
				Class<?> clazz = testClassLoader.loadClass(className);
				return ReflectionSupport.newInstance(clazz);
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to load class [" + className + "]", ex);
			}
		}
	}

	private static void instantiated(Class<? extends TestInstanceFactory> factoryClass, Class<?> testClass) {
		callSequence.add(factoryClass.getSimpleName() + " instantiated: " + testClass.getSimpleName());
	}

}
