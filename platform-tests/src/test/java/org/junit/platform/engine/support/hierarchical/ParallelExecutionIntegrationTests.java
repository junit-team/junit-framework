/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.engine.Constants.DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_PARALLEL_EXECUTION_MODE;
import static org.junit.jupiter.engine.Constants.PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.PARALLEL_CONFIG_STRATEGY_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_KEY;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.EventConditions.type;
import static org.junit.platform.testkit.engine.EventType.REPORTING_ENTRY_PUBLISHED;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.Events;

/**
 * @since 1.3
 */
@SuppressWarnings({ "JUnitMalformedDeclaration", "NewClassNamingConvention" })
class ParallelExecutionIntegrationTests {

	@Test
	void successfulParallelTest(TestReporter reporter) {
		var events = executeConcurrentlySuccessfully(3, SuccessfulParallelTestCase.class).list();

		var startedTimestamps = getTimestampsFor(events, event(test(), started()));
		var finishedTimestamps = getTimestampsFor(events, event(test(), finishedSuccessfully()));
		reporter.publishEntry("startedTimestamps", startedTimestamps.toString());
		reporter.publishEntry("finishedTimestamps", finishedTimestamps.toString());

		assertThat(startedTimestamps).hasSize(3);
		assertThat(finishedTimestamps).hasSize(3);
		assertThat(startedTimestamps).allMatch(startTimestamp -> finishedTimestamps.stream().noneMatch(
			finishedTimestamp -> finishedTimestamp.isBefore(startTimestamp)));
		assertThat(ThreadReporter.getThreadNames(events)).hasSize(3);
	}

	@Test
	void failingTestWithoutLock() {
		var events = executeConcurrently(3, FailingWithoutLockTestCase.class).list();
		assertThat(events.stream().filter(event(test(), finishedWithFailure())::matches)).hasSize(2);
	}

	@Test
	void successfulTestWithMethodLock() {
		var events = executeConcurrentlySuccessfully(3, SuccessfulWithMethodLockTestCase.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(3);
		assertThat(ThreadReporter.getThreadNames(events)).hasSize(3);
	}

	@Test
	void successfulTestWithClassLock() {
		var events = executeConcurrentlySuccessfully(3, SuccessfulWithClassLockTestCase.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(3);
		assertThat(ThreadReporter.getThreadNames(events)).hasSize(1);
	}

	@Test
	void testCaseWithFactory() {
		var events = executeConcurrentlySuccessfully(3, TestCaseWithTestFactory.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(3);
		assertThat(ThreadReporter.getThreadNames(events)).hasSize(1);
	}

	@Test
	void customContextClassLoader() {
		var currentThread = Thread.currentThread();
		var currentLoader = currentThread.getContextClassLoader();
		var smilingLoader = new URLClassLoader("(-:", new URL[0], ClassLoader.getSystemClassLoader());
		currentThread.setContextClassLoader(smilingLoader);
		try {
			var events = executeConcurrentlySuccessfully(3, SuccessfulWithMethodLockTestCase.class).list();

			assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(3);
			assertThat(ThreadReporter.getThreadNames(events)).hasSize(3);
			assertThat(ThreadReporter.getLoaderNames(events)).containsExactly("(-:");
		}
		finally {
			currentThread.setContextClassLoader(currentLoader);
		}
	}

	@RepeatedTest(10)
	void mixingClassAndMethodLevelLocks() {
		var events = executeConcurrentlySuccessfully(4, TestCaseWithSortedLocks.class,
			TestCaseWithUnsortedLocks.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(6);
		assertThat(ThreadReporter.getThreadNames(events).count()).isLessThanOrEqualTo(2);
	}

	@RepeatedTest(10)
	void locksOnNestedTests() {
		var events = executeConcurrentlySuccessfully(3, TestCaseWithNestedLocks.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(6);
		assertThat(ThreadReporter.getThreadNames(events)).hasSize(1);
	}

	@Test
	void afterHooksAreCalledAfterConcurrentDynamicTestsAreFinished() {
		var events = executeConcurrentlySuccessfully(3, ConcurrentDynamicTestCase.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(1);
		var timestampedEvents = ConcurrentDynamicTestCase.events;
		assertThat(timestampedEvents.get("afterEach")).isAfterOrEqualTo(timestampedEvents.get("dynamicTestFinished"));
	}

	/**
	 * @since 1.4
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/1688">gh-1688</a>
	 */
	@Test
	void threadInterruptedByUserCode() {
		var events = executeConcurrentlySuccessfully(3, InterruptedThreadTestCase.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(4);
	}

	@Test
	void executesTestTemplatesWithResourceLocksInSameThread() {
		var events = executeConcurrentlySuccessfully(2, ConcurrentTemplateTestCase.class).list();

		assertThat(events.stream().filter(event(test(), finishedSuccessfully())::matches)).hasSize(10);
		assertThat(ThreadReporter.getThreadNames(events)).hasSize(1);
	}

	@Test
	void executesClassesInParallelIfEnabledViaConfigurationParameter() {
		ParallelClassesTestCase.GLOBAL_BARRIER.reset();

		var configParams = Map.of(DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME, "concurrent");
		var results = executeWithFixedParallelism(3, configParams, ParallelClassesTestCaseA.class,
			ParallelClassesTestCaseB.class, ParallelClassesTestCaseC.class);

		results.testEvents().assertStatistics(stats -> stats.succeeded(9));
		assertThat(ThreadReporter.getThreadNames(results.allEvents().list())).hasSize(3);
		var testClassA = findFirstTestDescriptor(results, container(ParallelClassesTestCaseA.class));
		assertThat(ThreadReporter.getThreadNames(getEventsOfChildren(results, testClassA))).hasSize(1);
		var testClassB = findFirstTestDescriptor(results, container(ParallelClassesTestCaseB.class));
		assertThat(ThreadReporter.getThreadNames(getEventsOfChildren(results, testClassB))).hasSize(1);
		var testClassC = findFirstTestDescriptor(results, container(ParallelClassesTestCaseC.class));
		assertThat(ThreadReporter.getThreadNames(getEventsOfChildren(results, testClassC))).hasSize(1);
	}

	@Test
	void executesMethodsInParallelIfEnabledViaConfigurationParameter() {
		ParallelMethodsTestCase.barriersPerClass.clear();

		var configParams = Map.of( //
			DEFAULT_PARALLEL_EXECUTION_MODE, "concurrent", //
			DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME, "same_thread");
		var results = executeWithFixedParallelism(3, configParams, ParallelMethodsTestCaseA.class,
			ParallelMethodsTestCaseB.class, ParallelMethodsTestCaseC.class);

		results.testEvents().assertStatistics(stats -> stats.succeeded(9));
		assertThat(ThreadReporter.getThreadNames(results.allEvents().list())).hasSizeGreaterThanOrEqualTo(3);
		var testClassA = findFirstTestDescriptor(results, container(ParallelMethodsTestCaseA.class));
		assertThat(ThreadReporter.getThreadNames(getEventsOfChildren(results, testClassA))).hasSize(3);
		var testClassB = findFirstTestDescriptor(results, container(ParallelMethodsTestCaseB.class));
		assertThat(ThreadReporter.getThreadNames(getEventsOfChildren(results, testClassB))).hasSize(3);
		var testClassC = findFirstTestDescriptor(results, container(ParallelMethodsTestCaseC.class));
		assertThat(ThreadReporter.getThreadNames(getEventsOfChildren(results, testClassC))).hasSize(3);
	}

	@Test
	void canRunTestsIsolatedFromEachOther() {
		executeConcurrentlySuccessfully(2, IsolatedTestCase.class);
	}

	@Test
	void canRunTestsIsolatedFromEachOtherWithNestedCases() {
		executeConcurrentlySuccessfully(4, NestedIsolatedTestCase.class);
	}

	@Test
	void canRunTestsIsolatedFromEachOtherAcrossClasses() {
		executeConcurrentlySuccessfully(4, IndependentClasses.A.class, IndependentClasses.B.class);
	}

	@RepeatedTest(10)
	void canRunTestsIsolatedFromEachOtherAcrossClassesWithOtherResourceLocks() {
		executeConcurrentlySuccessfully(4, IndependentClasses.B.class, IndependentClasses.C.class);
	}

	@Test
	void runsIsolatedTestsLastToMaximizeParallelism() {
		var configParams = Map.of( //
			DEFAULT_PARALLEL_EXECUTION_MODE, "concurrent", //
			PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME, "3" //
		);
		Class<?>[] testClasses = { IsolatedTestCase.class, SuccessfulParallelTestCase.class };
		var events = executeWithFixedParallelism(3, configParams, testClasses) //
				.allEvents() //
				.assertStatistics(it -> it.failed(0));

		List<Event> parallelTestMethodEvents = events.reportingEntryPublished() //
				.filter(e -> e.getTestDescriptor().getSource() //
						.filter(it -> //
						it instanceof MethodSource methodSource
								&& SuccessfulParallelTestCase.class.equals(methodSource.getJavaClass()) //
						).isPresent() //
				) //
				.toList();
		assertThat(ThreadReporter.getThreadNames(parallelTestMethodEvents)).hasSize(3);

		var parallelClassFinish = getOnlyElement(getTimestampsFor(events.list(),
			event(container(SuccessfulParallelTestCase.class), finishedSuccessfully())));
		var isolatedClassStart = getOnlyElement(
			getTimestampsFor(events.list(), event(container(IsolatedTestCase.class), started())));
		assertThat(isolatedClassStart).isAfterOrEqualTo(parallelClassFinish);
	}

	@ParameterizedTest
	@ValueSource(classes = { IsolatedMethodFirstTestCase.class, IsolatedMethodLastTestCase.class,
			IsolatedNestedMethodFirstTestCase.class, IsolatedNestedMethodLastTestCase.class })
	void canRunTestsIsolatedFromEachOtherWhenDeclaredOnMethodLevel(Class<?> testClass) {
		List<Event> events = executeConcurrentlySuccessfully(1, testClass).list();

		assertThat(ThreadReporter.getThreadNames(events)).hasSize(1);
	}

	@ParameterizedTest
	@ValueSource(strings = { "testFactoryImplicitContainerAndExplicitChildExecutionMode",
			"testFactoryExplicitContainerAndExplicitChildExecutionModeOnContainer",
			"testFactoryExplicitContainerAndExplicitChildExecutionModeOnEachChild" })
	void allowsToControlExecutionModeOfDynamicTestsAndContainers(String methodName) {

		var results = executeWithFixedParallelism(3, Map.of(),
			List.of(selectMethod(ConcurrentDynamicContainerTestCase.class, methodName)));

		results.testEvents().assertStatistics(it -> it.succeeded(4));
		assertThat(ThreadReporter.getThreadNames(results.testEvents().list())).hasSize(2);
	}

	@Isolated("testing")
	static class IsolatedTestCase {
		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(2);
		}

		@Test
		void a() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		void b() throws Exception {
			storeAndBlockAndCheck(sharedResource, countDownLatch);
		}
	}

	static class NestedIsolatedTestCase {
		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(6);
		}

		@Test
		void a() throws Exception {
			storeAndBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		void b() throws Exception {
			storeAndBlockAndCheck(sharedResource, countDownLatch);
		}

		@Nested
		class Inner {

			@Test
			void a() throws Exception {
				storeAndBlockAndCheck(sharedResource, countDownLatch);
			}

			@Test
			void b() throws Exception {
				storeAndBlockAndCheck(sharedResource, countDownLatch);
			}

			@Nested
			@Isolated
			class InnerInner {

				@Test
				void a() throws Exception {
					incrementBlockAndCheck(sharedResource, countDownLatch);
				}

				@Test
				void b() throws Exception {
					storeAndBlockAndCheck(sharedResource, countDownLatch);
				}
			}
		}
	}

	@ExtendWith(ThreadReporter.class)
	static class IsolatedMethodFirstTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(2);
		}

		@Test
		@ResourceLock(value = GLOBAL_KEY, mode = READ_WRITE) // effectively @Isolated
		void test1() throws InterruptedException {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		@ResourceLock(value = "b", mode = READ_WRITE)
		void test2() throws InterruptedException {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}
	}

	@ExtendWith(ThreadReporter.class)
	static class IsolatedMethodLastTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(2);
		}

		@Test
		@ResourceLock(value = "b", mode = READ_WRITE)
		void test1() throws InterruptedException {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		@ResourceLock(value = GLOBAL_KEY, mode = READ_WRITE) // effectively @Isolated
		void test2() throws InterruptedException {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}
	}

	@ExtendWith(ThreadReporter.class)
	static class IsolatedNestedMethodFirstTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(2);
		}

		@Nested
		class Test1 {

			@Test
			@ResourceLock(value = GLOBAL_KEY, mode = READ_WRITE) // effectively @Isolated
			void test1() throws InterruptedException {
				incrementBlockAndCheck(sharedResource, countDownLatch);
			}
		}

		@Nested
		class Test2 {

			@Test
			@ResourceLock(value = "b", mode = READ_WRITE)
			void test2() throws InterruptedException {
				incrementBlockAndCheck(sharedResource, countDownLatch);
			}
		}
	}

	@ExtendWith(ThreadReporter.class)
	static class IsolatedNestedMethodLastTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(2);
		}

		@Nested
		class Test1 {

			@Test
			@ResourceLock(value = "b", mode = READ_WRITE)
			void test1() throws InterruptedException {
				incrementBlockAndCheck(sharedResource, countDownLatch);
			}
		}

		@Nested
		class Test2 {

			@Test
			@ResourceLock(value = GLOBAL_KEY, mode = READ_WRITE) // effectively @Isolated
			void test2() throws InterruptedException {
				incrementBlockAndCheck(sharedResource, countDownLatch);
			}
		}
	}

	static class IndependentClasses {
		static AtomicInteger sharedResource = new AtomicInteger();
		static CountDownLatch countDownLatch = new CountDownLatch(4);

		static class A {
			@Test
			void a() throws Exception {
				storeAndBlockAndCheck(sharedResource, countDownLatch);
			}

			@Test
			void b() throws Exception {
				storeAndBlockAndCheck(sharedResource, countDownLatch);
			}
		}

		@Isolated
		static class B {
			@Test
			void a() throws Exception {
				incrementBlockAndCheck(sharedResource, countDownLatch);
			}

			@Test
			void b() throws Exception {
				storeAndBlockAndCheck(sharedResource, countDownLatch);
			}
		}

		@ResourceLock("other")
		static class C {
			@Test
			void a() throws Exception {
				storeAndBlockAndCheck(sharedResource, countDownLatch);
			}

			@Test
			void b() throws Exception {
				storeAndBlockAndCheck(sharedResource, countDownLatch);
			}
		}
	}

	private List<Event> getEventsOfChildren(EngineExecutionResults results, TestDescriptor container) {
		return results.testEvents().filter(
			event -> event.getTestDescriptor().getParent().orElseThrow().equals(container)).toList();
	}

	private TestDescriptor findFirstTestDescriptor(EngineExecutionResults results, Condition<Event> condition) {
		return results.allEvents().filter(condition::matches).map(Event::getTestDescriptor).findFirst().orElseThrow();
	}

	private List<Instant> getTimestampsFor(List<Event> events, Condition<Event> condition) {
		// @formatter:off
		return events.stream()
				.filter(condition::matches)
				.map(Event::getTimestamp)
				.toList();
		// @formatter:on
	}

	private Events executeConcurrentlySuccessfully(int parallelism, Class<?>... testClasses) {
		var events = executeConcurrently(parallelism, testClasses);
		try {
			return events.assertStatistics(it -> it.failed(0));
		}
		catch (AssertionError error) {
			events.debug();
			throw error;
		}
	}

	private Events executeConcurrently(int parallelism, Class<?>... testClasses) {
		Map<String, String> configParams = Map.of(DEFAULT_PARALLEL_EXECUTION_MODE, "concurrent");
		return executeWithFixedParallelism(parallelism, configParams, testClasses) //
				.allEvents();
	}

	private EngineExecutionResults executeWithFixedParallelism(int parallelism, Map<String, String> configParams,
			Class<?>... testClasses) {
		return executeWithFixedParallelism(parallelism, configParams, selectClasses(testClasses));
	}

	private static EngineExecutionResults executeWithFixedParallelism(int parallelism, Map<String, String> configParams,
			List<? extends DiscoverySelector> selectors) {
		return EngineTestKit.engine("junit-jupiter") //
				.selectors(selectors) //
				.configurationParameter(PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, String.valueOf(true)) //
				.configurationParameter(PARALLEL_CONFIG_STRATEGY_PROPERTY_NAME, "fixed") //
				.configurationParameter(PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME, String.valueOf(parallelism)) //
				.configurationParameters(configParams) //
				.execute();
	}

	// -------------------------------------------------------------------------

	@ExtendWith(ThreadReporter.class)
	@Execution(SAME_THREAD)
	static class SuccessfulParallelTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(3);
		}

		@Test
		@Execution(CONCURRENT)
		void firstTest() throws Exception {
			incrementAndBlock(sharedResource, countDownLatch);
		}

		@Test
		@Execution(CONCURRENT)
		void secondTest() throws Exception {
			incrementAndBlock(sharedResource, countDownLatch);
		}

		@Test
		@Execution(CONCURRENT)
		void thirdTest() throws Exception {
			incrementAndBlock(sharedResource, countDownLatch);
		}
	}

	@ExtendWith(ThreadReporter.class)
	static class FailingWithoutLockTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(3);
		}

		@Test
		void firstTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		void secondTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		void thirdTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}
	}

	@ExtendWith(ThreadReporter.class)
	static class SuccessfulWithMethodLockTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(3);
		}

		@Test
		@ResourceLock("sharedResource")
		void firstTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		@ResourceLock("sharedResource")
		void secondTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		@ResourceLock("sharedResource")
		void thirdTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}
	}

	@ExtendWith(ThreadReporter.class)
	@ResourceLock("sharedResource")
	static class SuccessfulWithClassLockTestCase {

		static AtomicInteger sharedResource;
		static CountDownLatch countDownLatch;

		@BeforeAll
		static void initialize() {
			sharedResource = new AtomicInteger();
			countDownLatch = new CountDownLatch(3);
		}

		@Test
		void firstTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		void secondTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}

		@Test
		void thirdTest() throws Exception {
			incrementBlockAndCheck(sharedResource, countDownLatch);
		}
	}

	static class TestCaseWithTestFactory {
		@TestFactory
		@Execution(SAME_THREAD)
		Stream<DynamicTest> testFactory(TestReporter testReporter) {
			var sharedResource = new AtomicInteger(0);
			var countDownLatch = new CountDownLatch(3);
			return IntStream.range(0, 3).mapToObj(i -> dynamicTest("test " + i, () -> {
				incrementBlockAndCheck(sharedResource, countDownLatch);
				testReporter.publishEntry("thread", Thread.currentThread().getName());
			}));
		}
	}

	private static final ReentrantLock A = new ReentrantLock();
	private static final ReentrantLock B = new ReentrantLock();

	@ExtendWith(ThreadReporter.class)
	@ResourceLock("A")
	static class TestCaseWithSortedLocks {
		@ResourceLock("B")
		@Test
		void firstTest() {
			assertTrue(A.tryLock());
			assertTrue(B.tryLock());
		}

		@Execution(CONCURRENT)
		@ResourceLock("B")
		@Test
		void secondTest() {
			assertTrue(A.tryLock());
			assertTrue(B.tryLock());
		}

		@ResourceLock("B")
		@Test
		void thirdTest() {
			assertTrue(A.tryLock());
			assertTrue(B.tryLock());
		}

		@AfterEach
		void unlock() {
			B.unlock();
			A.unlock();
		}
	}

	@ExtendWith(ThreadReporter.class)
	@ResourceLock("B")
	static class TestCaseWithUnsortedLocks {
		@ResourceLock("A")
		@Test
		void firstTest() {
			assertTrue(B.tryLock());
			assertTrue(A.tryLock());
		}

		@Execution(CONCURRENT)
		@ResourceLock("A")
		@Test
		void secondTest() {
			assertTrue(B.tryLock());
			assertTrue(A.tryLock());
		}

		@ResourceLock("A")
		@Test
		void thirdTest() {
			assertTrue(B.tryLock());
			assertTrue(A.tryLock());
		}

		@AfterEach
		void unlock() {
			A.unlock();
			B.unlock();
		}
	}

	@ExtendWith(ThreadReporter.class)
	@ResourceLock("A")
	static class TestCaseWithNestedLocks {

		@ResourceLock("B")
		@Test
		void firstTest() {
			assertTrue(A.tryLock());
			assertTrue(B.tryLock());
		}

		@Execution(CONCURRENT)
		@ResourceLock("B")
		@Test
		void secondTest() {
			assertTrue(A.tryLock());
			assertTrue(B.tryLock());
		}

		@Test
		void thirdTest() {
			assertTrue(A.tryLock());
			assertTrue(B.tryLock());
		}

		@AfterEach
		void unlock() {
			A.unlock();
			B.unlock();
		}

		@Nested
		@ResourceLock("B")
		class B {

			@ResourceLock("A")
			@Test
			void firstTest() {
				assertTrue(B.tryLock());
				assertTrue(A.tryLock());
			}

			@ResourceLock("A")
			@Test
			void secondTest() {
				assertTrue(B.tryLock());
				assertTrue(A.tryLock());
			}

			@Test
			void thirdTest() {
				assertTrue(B.tryLock());
				assertTrue(A.tryLock());
			}
		}
	}

	@Execution(CONCURRENT)
	static class ConcurrentDynamicTestCase {
		static Map<String, Instant> events;

		@BeforeAll
		static void beforeAll() {
			events = new ConcurrentHashMap<>();
		}

		@AfterEach
		void afterEach() {
			events.put("afterEach", Instant.now());
		}

		@TestFactory
		DynamicTest testFactory() {
			return dynamicTest("slow", () -> {
				Thread.sleep(100);
				events.put("dynamicTestFinished", Instant.now());
			});
		}
	}

	@TestMethodOrder(MethodName.class)
	static class InterruptedThreadTestCase {

		@Test
		void test1() {
			Thread.currentThread().interrupt();
		}

		@Test
		void test2() throws InterruptedException {
			Thread.sleep(10);
		}

		@Test
		void test3() {
			Thread.currentThread().interrupt();
		}

		@Test
		void test4() throws InterruptedException {
			Thread.sleep(10);
		}

	}

	@Execution(CONCURRENT)
	@ExtendWith(ThreadReporter.class)
	static class ConcurrentTemplateTestCase {
		@RepeatedTest(10)
		@ResourceLock("a")
		void repeatedTest() throws Exception {
			Thread.sleep(100);
		}
	}

	@ExtendWith(ThreadReporter.class)
	static abstract class BarrierTestCase {

		@Test
		void test1() throws Exception {
			getBarrier().await();
		}

		@Test
		void test2() throws Exception {
			getBarrier().await();
		}

		@Test
		void test3() throws Exception {
			getBarrier().await();
		}

		abstract CyclicBarrier getBarrier();

	}

	static class ParallelMethodsTestCase extends BarrierTestCase {

		static final Map<Class<?>, CyclicBarrier> barriersPerClass = new ConcurrentHashMap<>();

		@Override
		CyclicBarrier getBarrier() {
			return barriersPerClass.computeIfAbsent(this.getClass(), key -> new CyclicBarrier(3));
		}
	}

	static class ParallelClassesTestCase extends BarrierTestCase {

		static final CyclicBarrier GLOBAL_BARRIER = new CyclicBarrier(3);

		@Override
		CyclicBarrier getBarrier() {
			return GLOBAL_BARRIER;
		}

	}

	static class ParallelClassesTestCaseA extends ParallelClassesTestCase {
	}

	static class ParallelClassesTestCaseB extends ParallelClassesTestCase {
	}

	static class ParallelClassesTestCaseC extends ParallelClassesTestCase {
	}

	static class ParallelMethodsTestCaseA extends ParallelMethodsTestCase {
	}

	static class ParallelMethodsTestCaseB extends ParallelMethodsTestCase {
	}

	static class ParallelMethodsTestCaseC extends ParallelMethodsTestCase {
	}

	@ExtendWith(ThreadReporter.class)
	static class ConcurrentDynamicContainerTestCase {

		@TestFactory
		@Execution(CONCURRENT)
		Stream<DynamicContainer> testFactoryImplicitContainerAndExplicitChildExecutionMode() {
			return testFactory(container -> container.childExecutionMode(SAME_THREAD), UnaryOperator.identity());
		}

		@TestFactory
		Stream<DynamicContainer> testFactoryExplicitContainerAndExplicitChildExecutionModeOnContainer() {
			return testFactory(container -> container.executionMode(CONCURRENT).childExecutionMode(SAME_THREAD),
				UnaryOperator.identity());
		}

		@TestFactory
		Stream<DynamicContainer> testFactoryExplicitContainerAndExplicitChildExecutionModeOnEachChild() {
			return testFactory(container -> container.executionMode(CONCURRENT),
				test -> test.executionMode(SAME_THREAD));
		}

		private Stream<DynamicContainer> testFactory(UnaryOperator<DynamicContainer.Configuration> containerConfigurer,
				UnaryOperator<DynamicTest.Configuration> testsConfigurer) {

			var sharedResource1 = new AtomicInteger();
			var latch1 = new CountDownLatch(2);

			var sharedResource2 = new AtomicInteger();
			var latch2 = new CountDownLatch(2);

			var tests1 = Stream.of( //
				dynamicTest(config -> testsConfigurer.apply(config) //
						.displayName("test1") //
						.executable(() -> incrementAndBlock(sharedResource1, latch1))), //
				dynamicTest(config -> testsConfigurer.apply(config) //
						.displayName("test2") //
						.executable(() -> incrementAndBlock(sharedResource2, latch2))));

			var tests2 = Stream.of( //
				dynamicTest(config -> testsConfigurer.apply(config) //
						.displayName("test3") //
						.executable(() -> incrementAndBlock(sharedResource1, latch1))), //
				dynamicTest(config -> testsConfigurer.apply(config) //
						.displayName("test4") //
						.executable(() -> incrementAndBlock(sharedResource2, latch2))));

			return Stream.of( //
				dynamicContainer(config -> containerConfigurer.apply(config) //
						.displayName("suite1") //
						.children(tests1)), //
				dynamicContainer(config -> containerConfigurer.apply(config) //
						.displayName("suite2") //
						.children(tests2)));
		}

	}

	private static void incrementBlockAndCheck(AtomicInteger sharedResource, CountDownLatch countDownLatch)
			throws InterruptedException {
		var value = incrementAndBlock(sharedResource, countDownLatch);
		assertEquals(value, sharedResource.get());
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static int incrementAndBlock(AtomicInteger sharedResource, CountDownLatch countDownLatch)
			throws InterruptedException {
		var value = sharedResource.incrementAndGet();
		countDownLatch.countDown();
		countDownLatch.await(estimateSimulatedTestDurationInMilliseconds(), MILLISECONDS);
		return value;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static void storeAndBlockAndCheck(AtomicInteger sharedResource, CountDownLatch countDownLatch)
			throws InterruptedException {
		var value = sharedResource.get();
		countDownLatch.countDown();
		countDownLatch.await(estimateSimulatedTestDurationInMilliseconds(), MILLISECONDS);
		assertEquals(value, sharedResource.get());
	}

	/*
	 * To simulate tests running in parallel tests will modify a shared
	 * resource, simulate work by waiting, then check if the shared resource was
	 * not modified by any other thread.
	 *
	 * Depending on system performance the simulation of work needs to be longer
	 * on slower systems to ensure tests can run in parallel.
	 *
	 * Currently, CI is known to be slow.
	 */
	private static long estimateSimulatedTestDurationInMilliseconds() {
		var runningInCi = Boolean.parseBoolean(System.getenv("CI"));
		return runningInCi ? 1000 : 100;
	}

	@NullMarked
	static class ThreadReporter implements AfterTestExecutionCallback, InvocationInterceptor {

		private static Stream<String> getLoaderNames(List<Event> events) {
			return getValues(events, "loader");
		}

		private static Stream<String> getThreadNames(List<Event> events) {
			return getValues(events, "thread");
		}

		private static Stream<String> getValues(List<Event> events, String key) {
			// @formatter:off
			return events.stream()
					.filter(type(REPORTING_ENTRY_PUBLISHED)::matches)
					.map(event -> event.getPayload(ReportEntry.class).orElseThrow())
					.map(ReportEntry::getKeyValuePairs)
					.filter(keyValuePairs -> keyValuePairs.containsKey(key))
					.map(keyValuePairs -> keyValuePairs.get(key))
					.distinct();
			// @formatter:on
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			publishReportEntries(context);
		}

		@Override
		public void interceptDynamicTest(Invocation<@Nullable Void> invocation,
				DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext) throws Throwable {

			try {
				invocation.proceed();
			}
			finally {
				publishReportEntries(extensionContext);
			}
		}

		private static void publishReportEntries(ExtensionContext context) {
			context.publishReportEntry("thread", Thread.currentThread().getName());
			context.publishReportEntry("loader", Thread.currentThread().getContextClassLoader().getName());
		}
	}

}
