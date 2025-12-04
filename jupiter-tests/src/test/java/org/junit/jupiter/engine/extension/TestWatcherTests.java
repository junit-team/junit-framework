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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests for the {@link TestWatcher} extension API.
 *
 * @since 5.4
 */
class TestWatcherTests extends AbstractJupiterTestEngineTests {

	private static final List<String> testWatcherMethodNames = Arrays.stream(TestWatcher.class.getDeclaredMethods()) //
			.filter(not(Method::isSynthetic)) //
			.map(Method::getName) //
			.toList();

	@BeforeEach
	void clearResults() {
		TrackingTestWatcher.results.clear();
		DataRetrievingTestWatcher.results.clear();
	}

	@Test
	void testWatcherIsInvokedForTestMethodsInTopLevelAndNestedTestClasses() {
		assertCommonStatistics(executeTestsForClass(TrackingTestWatcherTestMethodsTestCase.class));
		assertThat(TrackingTestWatcher.results.keySet()).containsAll(testWatcherMethodNames);
		TrackingTestWatcher.results.values().forEach(testMethodNames -> assertEquals(2, testMethodNames.size()));
	}

	@Test
	void testWatcherIsInvokedForRepeatedTestMethods() {
		EngineExecutionResults results = executeTestsForClass(TrackingTestWatcherRepeatedTestMethodsTestCase.class);

		results.containerEvents().assertStatistics(
			stats -> stats.skipped(1).started(5).succeeded(5).aborted(0).failed(0));
		results.testEvents().assertStatistics(
			stats -> stats.dynamicallyRegistered(6).skipped(0).started(6).succeeded(2).aborted(2).failed(2));

		// Since the @RepeatedTest container is disabled, the individual invocations never occur.
		assertThat(TrackingTestWatcher.results.keySet()).containsAll(testWatcherMethodNames);
		// 2 => number of iterations declared in @RepeatedTest(2).
		TrackingTestWatcher.results.forEach((testWatcherMethod, testMethodNames) -> assertEquals(
			"testDisabled".equals(testWatcherMethod) ? 1 : 2, testMethodNames.size()));
	}

	@Test
	void testWatcherIsNotInvokedForTestFactoryMethods() {
		EngineExecutionResults results = executeTestsForClass(TrackingTestWatcherTestFactoryMethodsTestCase.class);

		results.containerEvents().assertStatistics(
			stats -> stats.skipped(1).started(5).succeeded(5).aborted(0).failed(0));
		results.testEvents().assertStatistics(
			stats -> stats.dynamicallyRegistered(6).skipped(0).started(6).succeeded(2).aborted(2).failed(2));

		// There should be zero results, since the TestWatcher API is not supported for @TestFactory containers.
		assertThat(TrackingTestWatcher.results).isEmpty();
	}

	@Test
	void testWatcherExceptionsAreLoggedAndSwallowed(@TrackLogRecords LogRecordListener logRecordListener) {
		assertCommonStatistics(executeTestsForClass(ExceptionThrowingTestWatcherTestCase.class));

		// @formatter:off
		long exceptionCount = logRecordListener.stream(MethodBasedTestDescriptor.class, Level.WARNING)
				.map(LogRecord::getThrown)
				.filter(JUnitException.class::isInstance)
				.map(throwable -> throwable.getStackTrace()[0].getMethodName())
				.filter(testWatcherMethodNames::contains)
				.count();
		// @formatter:on

		assertEquals(8, exceptionCount, "Thrown exceptions were not logged properly.");
	}

	@Test
	void testWatcherIsInvokedForTestMethodsInTestCaseWithProblematicConstructor() {
		EngineExecutionResults results = executeTestsForClass(ProblematicConstructorTestCase.class);
		results.testEvents().assertStatistics(stats -> stats.skipped(0).started(8).succeeded(0).aborted(0).failed(8));
		assertThat(TrackingTestWatcher.results.keySet()).containsExactly("testFailed");
		assertThat(TrackingTestWatcher.results.get("testFailed")).hasSize(8);
	}

	@Test
	void testWatcherSemanticsWhenRegisteredAtClassLevel() {
		Class<?> testClass = ClassLevelTestWatcherTestCase.class;
		assertStatsForAbstractDisabledMethodsTestCase(testClass);

		// We get "testDisabled" events for the @Test method and the @RepeatedTest container.
		assertThat(TrackingTestWatcher.results.get("testDisabled")).containsExactly("test", "repeatedTest");
	}

	@Test
	void testWatcherSemanticsWhenRegisteredAtInstanceLevelWithTestInstanceLifecyclePerClass() {
		Class<?> testClass = TestInstancePerClassInstanceLevelTestWatcherTestCase.class;
		assertStatsForAbstractDisabledMethodsTestCase(testClass);

		// We get "testDisabled" events for the @Test method and the @RepeatedTest container.
		assertThat(TrackingTestWatcher.results.get("testDisabled")).containsExactly("test", "repeatedTest");
	}

	@Test
	void testWatcherSemanticsWhenRegisteredAtInstanceLevelWithTestInstanceLifecyclePerMethod() {
		Class<?> testClass = TestInstancePerMethodInstanceLevelTestWatcherTestCase.class;
		assertStatsForAbstractDisabledMethodsTestCase(testClass);

		// Since the TestWatcher is registered at the instance level with test instance
		// lifecycle per-method semantics, we get a "testDisabled" event only for the @Test
		// method and NOT for the @RepeatedTest container.
		assertThat(TrackingTestWatcher.results.get("testDisabled")).containsExactly("test");
	}

	@Test
	void testWatcherSemanticsWhenRegisteredAtMethodLevel() {
		Class<?> testClass = MethodLevelTestWatcherTestCase.class;
		assertStatsForAbstractDisabledMethodsTestCase(testClass);

		// We get "testDisabled" events for the @Test method and the @RepeatedTest container.
		assertThat(TrackingTestWatcher.results.get("testDisabled")).containsExactly("test", "repeatedTest");
	}

	@Test
	void testWatcherCanRetrieveDataFromTheExtensionContextStore() {
		Class<?> testClass = DataRetrievingTestWatcherTestCase.class;
		EngineExecutionResults results = executeTestsForClass(testClass);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		assertThat(DataRetrievingTestWatcher.results).containsExactly(entry("key", "enigma"));
	}

	private void assertCommonStatistics(EngineExecutionResults results) {
		results.containerEvents().assertStatistics(stats -> stats.started(3).succeeded(3).failed(0));
		results.testEvents().assertStatistics(stats -> stats.skipped(2).started(6).succeeded(2).aborted(2).failed(2));
	}

	private void assertStatsForAbstractDisabledMethodsTestCase(Class<?> testClass) {
		EngineExecutionResults results = executeTestsForClass(testClass);

		results.containerEvents().assertStatistics(//
			stats -> stats.skipped(1).started(2).succeeded(2).aborted(0).failed(0));
		results.testEvents().assertStatistics(//
			stats -> stats.skipped(1).started(0).succeeded(0).aborted(0).failed(0));

		assertThat(TrackingTestWatcher.results.keySet()).containsExactly("testDisabled");
	}

	// -------------------------------------------------------------------------

	private static abstract class AbstractTestCase {

		@Test
		public void successfulTest() {
			//no-op
		}

		@Test
		public void failedTest() {
			fail("Must fail");
		}

		@Test
		public void abortedTest() {
			assumeTrue(false);
		}

		@Test
		@Disabled
		public void skippedTest() {
			//no-op
		}

		@Nested
		class SecondLevel {

			@Test
			public void successfulTest() {
				//no-op
			}

			@Test
			public void failedTest() {
				fail("Must fail");
			}

			@Test
			public void abortedTest() {
				assumeTrue(false);
			}

			@Test
			@Disabled
			public void skippedTest() {
				//no-op
			}

		}

	}

	@ExtendWith(TrackingTestWatcher.class)
	static class TrackingTestWatcherTestMethodsTestCase extends AbstractTestCase {
	}

	@ExtendWith(TrackingTestWatcher.class)
	static class TrackingTestWatcherRepeatedTestMethodsTestCase {

		@RepeatedTest(2)
		void successfulTest() {
			//no-op
		}

		@RepeatedTest(2)
		void failedTest() {
			fail("Must fail");
		}

		@RepeatedTest(2)
		void abortedTest() {
			assumeTrue(false);
		}

		@RepeatedTest(2)
		@Disabled
		void skippedTest() {
			//no-op
		}

	}

	@ExtendWith(TrackingTestWatcher.class)
	static class TrackingTestWatcherTestFactoryMethodsTestCase {

		@TestFactory
		Stream<DynamicTest> successfulTest() {
			return Stream.of("A", "B").map(text -> dynamicTest(text, () -> assertTrue(true)));
		}

		@TestFactory
		Stream<DynamicTest> failedTest() {
			return Stream.of("A", "B").map(text -> dynamicTest(text, () -> fail("Must fail")));

		}

		@TestFactory
		Stream<DynamicTest> abortedTest() {
			return Stream.of("A", "B").map(text -> dynamicTest(text, () -> assumeTrue(false)));

		}

		@TestFactory
		@Disabled
		Stream<DynamicTest> skippedTest() {
			return Stream.of("A", "B").map(text -> dynamicTest(text, Assertions::fail));
		}

	}

	@ExtendWith(ExceptionThrowingTestWatcher.class)
	static class ExceptionThrowingTestWatcherTestCase extends AbstractTestCase {
	}

	@ExtendWith(TrackingTestWatcher.class)
	static class ProblematicConstructorTestCase extends AbstractTestCase {
		ProblematicConstructorTestCase(Object ignore) {
		}
	}

	@TestMethodOrder(OrderAnnotation.class)
	private static abstract class AbstractDisabledMethodsTestCase {

		@Disabled
		@Test
		@Order(1)
		void test() {
		}

		@Disabled
		@RepeatedTest(2)
		@Order(2)
		void repeatedTest() {
		}
	}

	static class ClassLevelTestWatcherTestCase extends AbstractDisabledMethodsTestCase {

		@RegisterExtension
		static TestWatcher watcher = new TrackingTestWatcher();
	}

	@TestInstance(Lifecycle.PER_CLASS)
	static class TestInstancePerClassInstanceLevelTestWatcherTestCase extends AbstractDisabledMethodsTestCase {

		@RegisterExtension
		TestWatcher watcher = new TrackingTestWatcher();
	}

	@TestInstance(Lifecycle.PER_METHOD)
	static class TestInstancePerMethodInstanceLevelTestWatcherTestCase extends AbstractDisabledMethodsTestCase {

		@RegisterExtension
		TestWatcher watcher = new TrackingTestWatcher();
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class MethodLevelTestWatcherTestCase extends AbstractDisabledMethodsTestCase {

		@Override
		@Disabled
		@Test
		@Order(1)
		@ExtendWith(TrackingTestWatcher.class)
		void test() {
		}

		@Override
		@Disabled
		@RepeatedTest(1)
		@Order(2)
		@ExtendWith(TrackingTestWatcher.class)
		void repeatedTest() {
		}
	}

	private static class TrackingTestWatcher implements TestWatcher {

		private static final Map<String, List<String>> results = new HashMap<>();

		@Override
		public void testSuccessful(ExtensionContext context) {
			trackResult("testSuccessful", context);
		}

		@Override
		public void testAborted(ExtensionContext context, @Nullable Throwable cause) {
			trackResult("testAborted", context);
		}

		@Override
		public void testFailed(ExtensionContext context, @Nullable Throwable cause) {
			trackResult("testFailed", context);
		}

		@Override
		public void testDisabled(ExtensionContext context, Optional<String> reason) {
			trackResult("testDisabled", context);
		}

		protected void trackResult(String testWatcherMethod, ExtensionContext context) {
			String testMethod = context.getRequiredTestMethod().getName();
			results.computeIfAbsent(testWatcherMethod, k -> new ArrayList<>()).add(testMethod);
		}

	}

	private static class ExceptionThrowingTestWatcher implements TestWatcher {

		@Override
		public void testSuccessful(ExtensionContext context) {
			throw new JUnitException("Exception in testSuccessful()");
		}

		@Override
		public void testDisabled(ExtensionContext context, Optional<String> reason) {
			throw new JUnitException("Exception in testDisabled()");
		}

		@Override
		public void testAborted(ExtensionContext context, @Nullable Throwable cause) {
			throw new JUnitException("Exception in testAborted()");
		}

		@Override
		public void testFailed(ExtensionContext context, @Nullable Throwable cause) {
			throw new JUnitException("Exception in testFailed()");
		}

	}

	/**
	 * {@link TestWatcher} that retrieves data from the {@link ExtensionContext.Store}.
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/3944">#3944</a>
	 */
	static class DataRetrievingTestWatcher implements BeforeTestExecutionCallback, TestWatcher {

		private static final Namespace NAMESPACE = Namespace.create(DataRetrievingTestWatcher.class);

		private static final String KEY = "key";

		private static final Map<String, @Nullable String> results = new HashMap<>();

		@Override
		public void beforeTestExecution(ExtensionContext context) throws Exception {
			getStore(context).put(KEY, "enigma");
		}

		@Override
		public void testSuccessful(ExtensionContext context) {
			results.put(KEY, getStore(context).get(KEY, String.class));
		}

		private static Store getStore(ExtensionContext context) {
			return context.getStore(NAMESPACE);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(DataRetrievingTestWatcher.class)
	static class DataRetrievingTestWatcherTestCase {

		@Test
		public void test() {
			//no-op
		}
	}

}
