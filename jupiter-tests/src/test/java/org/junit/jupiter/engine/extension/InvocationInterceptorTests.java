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

import static java.util.function.Function.identity;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;

class InvocationInterceptorTests extends AbstractJupiterTestEngineTests {

	@Test
	void failsTestWhenInterceptorChainDoesNotCallInvocation() {
		var results = executeTestsForClass(InvocationIgnoringInterceptorTestCase.class);

		var tests = results.testEvents().assertStatistics(stats -> stats.failed(1).succeeded(0));
		tests.failed().assertEventsMatchExactly(
			event(test("test"), finishedWithFailure(instanceOf(JUnitException.class),
				message(it -> it.startsWith("Chain of InvocationInterceptors never called invocation")))));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InvocationIgnoringInterceptorTestCase {
		@RegisterExtension
		Extension interceptor = new InvocationInterceptor() {
			@Override
			public void interceptTestMethod(Invocation<@Nullable Void> invocation,
					ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
				// do nothing
			}
		};

		@Test
		void test() {
			// never called
		}
	}

	@Test
	void successTestWhenInterceptorChainSkippedInvocation() {
		var results = executeTestsForClass(InvocationSkippedTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.failed(0).succeeded(1));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InvocationSkippedTestCase {
		@RegisterExtension
		Extension interceptor = new InvocationInterceptor() {
			@Override
			public void interceptTestMethod(Invocation<@Nullable Void> invocation,
					ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
				invocation.skip();
			}
		};

		@Test
		void test() {
			fail("should not be called");
		}
	}

	@Test
	void failsTestWhenInterceptorChainCallsInvocationMoreThanOnce() {
		var results = executeTestsForClass(DoubleInvocationInterceptorTestCase.class);

		var tests = results.testEvents().assertStatistics(stats -> stats.failed(1).succeeded(0));
		tests.failed().assertEventsMatchExactly(
			event(test("test"), finishedWithFailure(instanceOf(JUnitException.class), message(it -> it.startsWith(
				"Chain of InvocationInterceptors called invocation multiple times instead of just once")))));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class DoubleInvocationInterceptorTestCase {
		@RegisterExtension
		Extension interceptor = new InvocationInterceptor() {
			@Override
			public void interceptTestMethod(Invocation<@Nullable Void> invocation,
					ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
					throws Throwable {
				invocation.proceed();
				invocation.proceed();
			}
		};

		@Test
		void test() {
			// called twice
		}
	}

	@TestFactory
	Stream<DynamicTest> callsInterceptors() {
		var results = executeTestsForClass(TestCaseWithThreeInterceptors.class);

		results.testEvents().assertStatistics(stats -> stats.failed(0).succeeded(3));

		return Arrays.stream(InvocationType.values()) //
				.map(it -> dynamicTest(it.name(), () -> verifyEvents(results, it)));
	}

	private void verifyEvents(EngineExecutionResults results, InvocationType invocationType) {
		var beforeEvents = List.of("before:foo", "before:bar", "before:baz");
		var testEvent = List.of("test");
		var afterEvents = List.of("after:baz", "after:bar", "after:foo");
		var allEvents = Stream.of(beforeEvents, testEvent, afterEvents).flatMap(Collection::stream).toList();
		String testClassName = TestCaseWithThreeInterceptors.class.getName();

		var expectedElements = switch (invocationType) {
			case BEFORE_ALL, AFTER_ALL -> prefixed(allEvents, testClassName);
			case CONSTRUCTOR -> concatStreams(
				prefixed(allEvents, it -> it.endsWith(":bar") ? testClassName : "test(TestReporter)"),
				prefixed(allEvents, it -> it.endsWith(":bar") ? testClassName : "testTemplate(TestReporter)[1]"),
				prefixed(allEvents, it -> it.endsWith(":bar") ? testClassName : "testFactory(TestReporter)"));
			case BEFORE_EACH, AFTER_EACH -> concatStreams(prefixed(allEvents, "test(TestReporter)"),
				prefixed(allEvents, "testTemplate(TestReporter)[1]"), prefixed(allEvents, "testFactory(TestReporter)"));
			case TEST_METHOD -> prefixed(allEvents, "test(TestReporter)");
			case TEST_TEMPLATE_METHOD -> prefixed(allEvents, "testTemplate(TestReporter)[1]");
			case TEST_FACTORY_METHOD -> prefixed(allEvents, "testFactory(TestReporter)");
			case DYNAMIC_TEST -> concatStreams(prefixed(beforeEvents, "testFactory(TestReporter)[1]"),
				prefixed(testEvent, "testFactory(TestReporter)"),
				prefixed(afterEvents, "testFactory(TestReporter)[1]"));
		};

		assertThat(getEvents(results, invocationType)) //
				.containsExactlyElementsOf(expectedElements.toList());
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private static <T> Stream<T> concatStreams(Stream<T>... items) {
		return Stream.of(items).flatMap(identity());
	}

	private static Stream<String> prefixed(List<String> values, String prefix) {
		return prefixed(values, __ -> prefix);
	}

	private static Stream<String> prefixed(List<String> values, UnaryOperator<String> prefixGenerator) {
		return values.stream() //
				.map(it -> "[%s] %s".formatted(prefixGenerator.apply(it), it));
	}

	private Stream<String> getEvents(EngineExecutionResults results, InvocationType invocationType) {
		return results.allEvents().reportingEntryPublished().stream() //
				.flatMap(event -> {
					var reportEntry = event.getPayload(ReportEntry.class).orElseThrow();
					var keyValuePairs = reportEntry.getKeyValuePairs();
					if (keyValuePairs.keySet().stream() //
							.map(InvocationType::valueOf) //
							.anyMatch(isEqual(invocationType))) {
						return keyValuePairs.values().stream() //
								.map(it -> "[%s] %s".formatted(event.getTestDescriptor().getLegacyReportingName(), it));
					}
					return Stream.empty();
				});
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ FooInvocationInterceptor.class, BarInvocationInterceptor.class, BazInvocationInterceptor.class })
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	static class TestCaseWithThreeInterceptors {

		TestCaseWithThreeInterceptors(TestReporter reporter) {
			publish(reporter, InvocationType.CONSTRUCTOR);
		}

		@SuppressWarnings("JUnitMalformedDeclaration")
		@BeforeAll
		static void beforeAll(TestReporter reporter) {
			publish(reporter, InvocationType.BEFORE_ALL);
		}

		@BeforeEach
		void beforeEach(TestReporter reporter) {
			publish(reporter, InvocationType.BEFORE_EACH);
		}

		@Order(1)
		@Test
		void test(TestReporter reporter) {
			publish(reporter, InvocationType.TEST_METHOD);
		}

		@Order(2)
		@RepeatedTest(1)
		void testTemplate(TestReporter reporter) {
			publish(reporter, InvocationType.TEST_TEMPLATE_METHOD);
		}

		@Order(3)
		@TestFactory
		DynamicTest testFactory(TestReporter reporter) {
			publish(reporter, InvocationType.TEST_FACTORY_METHOD);
			return dynamicTest("dynamicTest", () -> {
				publish(reporter, InvocationType.DYNAMIC_TEST);
			});
		}

		@AfterEach
		void afterEach(TestReporter reporter) {
			publish(reporter, InvocationType.AFTER_EACH);
		}

		@SuppressWarnings("JUnitMalformedDeclaration")
		@AfterAll
		static void afterAll(TestReporter reporter) {
			publish(reporter, InvocationType.AFTER_ALL);
		}

		static void publish(TestReporter reporter, InvocationType type) {
			reporter.publishEntry(type.name(), "test");
		}

	}

	enum InvocationType {
		BEFORE_ALL,
		CONSTRUCTOR,
		BEFORE_EACH,
		TEST_METHOD,
		TEST_TEMPLATE_METHOD,
		TEST_FACTORY_METHOD,
		DYNAMIC_TEST,
		AFTER_EACH,
		AFTER_ALL
	}

	abstract static class ReportingInvocationInterceptor implements InvocationInterceptor {
		private final Class<TestCaseWithThreeInterceptors> testClass = TestCaseWithThreeInterceptors.class;
		private final String name;

		ReportingInvocationInterceptor(String name) {
			this.name = name;
		}

		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return ExtensionContextScope.TEST_METHOD;
		}

		@Override
		public void interceptBeforeAllMethod(Invocation<@Nullable Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertThat(invocationContext.getTarget()).isEmpty();
			assertEquals(testClass.getDeclaredMethod("beforeAll", TestReporter.class),
				invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			this.<@Nullable Void> reportAndProceed(invocation, extensionContext, InvocationType.BEFORE_ALL);
		}

		@Override
		public <T> T interceptTestClassConstructor(Invocation<T> invocation,
				ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertEquals(testClass.getDeclaredConstructor(TestReporter.class), invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			return reportAndProceed(invocation, extensionContext, InvocationType.CONSTRUCTOR);
		}

		@Override
		public void interceptBeforeEachMethod(Invocation<@Nullable Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertThat(invocationContext.getTarget()).containsInstanceOf(testClass);
			assertEquals(testClass.getDeclaredMethod("beforeEach", TestReporter.class),
				invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			this.<@Nullable Void> reportAndProceed(invocation, extensionContext, InvocationType.BEFORE_EACH);
		}

		@Override
		public void interceptTestMethod(Invocation<@Nullable Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertThat(invocationContext.getTarget()).containsInstanceOf(testClass);
			assertEquals(testClass.getDeclaredMethod("test", TestReporter.class), invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			this.<@Nullable Void> reportAndProceed(invocation, extensionContext, InvocationType.TEST_METHOD);
		}

		@Override
		public void interceptTestTemplateMethod(Invocation<@Nullable Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertThat(invocationContext.getTarget()).containsInstanceOf(testClass);
			assertEquals(testClass.getDeclaredMethod("testTemplate", TestReporter.class),
				invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			this.<@Nullable Void> reportAndProceed(invocation, extensionContext, InvocationType.TEST_TEMPLATE_METHOD);
		}

		@Override
		public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertThat(invocationContext.getTarget()).containsInstanceOf(testClass);
			assertEquals(testClass.getDeclaredMethod("testFactory", TestReporter.class),
				invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			return reportAndProceed(invocation, extensionContext, InvocationType.TEST_FACTORY_METHOD);
		}

		@Override
		public void interceptDynamicTest(Invocation<@Nullable Void> invocation,
				DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext) throws Throwable {
			assertThat(invocationContext.getExecutable()).isNotNull();
			assertThat(extensionContext.getUniqueId()).isNotBlank();
			assertThat(extensionContext.getElement()).isEmpty();
			assertThat(extensionContext.getParent().flatMap(ExtensionContext::getTestMethod)) //
					.contains(testClass.getDeclaredMethod("testFactory", TestReporter.class));
			this.<@Nullable Void> reportAndProceed(invocation, extensionContext, InvocationType.DYNAMIC_TEST);
		}

		@Override
		public void interceptAfterEachMethod(Invocation<@Nullable Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertThat(invocationContext.getTarget()).containsInstanceOf(testClass);
			assertEquals(testClass.getDeclaredMethod("afterEach", TestReporter.class),
				invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			this.<@Nullable Void> reportAndProceed(invocation, extensionContext, InvocationType.AFTER_EACH);
		}

		@Override
		public void interceptAfterAllMethod(Invocation<@Nullable Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			assertEquals(testClass, invocationContext.getTargetClass());
			assertThat(invocationContext.getTarget()).isEmpty();
			assertEquals(testClass.getDeclaredMethod("afterAll", TestReporter.class),
				invocationContext.getExecutable());
			assertThat(invocationContext.getArguments()).hasSize(1).hasOnlyElementsOfType(TestReporter.class);
			this.<@Nullable Void> reportAndProceed(invocation, extensionContext, InvocationType.AFTER_ALL);
		}

		private <T extends @Nullable Object> T reportAndProceed(Invocation<T> invocation,
				ExtensionContext extensionContext, InvocationType type) throws Throwable {
			extensionContext.publishReportEntry(type.name(), "before:" + name);
			try {
				return invocation.proceed();
			}
			finally {
				extensionContext.publishReportEntry(type.name(), "after:" + name);
			}
		}
	}

	static class FooInvocationInterceptor extends ReportingInvocationInterceptor {
		FooInvocationInterceptor() {
			super("foo");
		}
	}

	static class BarInvocationInterceptor extends ReportingInvocationInterceptor {
		BarInvocationInterceptor() {
			super("bar");
		}

		@SuppressWarnings("deprecation")
		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return ExtensionContextScope.DEFAULT;
		}
	}

	static class BazInvocationInterceptor extends ReportingInvocationInterceptor {
		BazInvocationInterceptor() {
			super("baz");
		}
	}

}
