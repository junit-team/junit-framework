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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.suppressed;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.assertj.core.api.Condition;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.PreInterruptCallback;
import org.junit.jupiter.api.extension.PreInterruptContext;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.Constants;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Events;

/**
 * @since 5.12
 */
@Isolated
class PreInterruptCallbackTests extends AbstractJupiterTestEngineTests {

	private static final String TC = "test";
	private static final String TIMEOUT_ERROR_MSG = TC + "() timed out after 1 microsecond";
	private static final AtomicBoolean interruptedTest = new AtomicBoolean();
	private static final CompletableFuture<?> testThreadExecutionDone = new CompletableFuture<>();
	private static final AtomicReference<@Nullable Thread> interruptedTestThread = new AtomicReference<>();
	private static final AtomicBoolean interruptCallbackShallThrowException = new AtomicBoolean();
	private static final AtomicReference<@Nullable PreInterruptContext> calledPreInterruptContext = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		interruptedTest.set(false);
		interruptCallbackShallThrowException.set(false);
		calledPreInterruptContext.set(null);
	}

	@AfterEach
	void tearDown() {
		calledPreInterruptContext.set(null);
		interruptedTestThread.set(null);
	}

	@Test
	@ResourceLock(value = SYSTEM_OUT, mode = READ_WRITE)
	void testCaseWithDefaultInterruptCallbackEnabled() {
		PrintStream orgOutStream = System.out;
		EngineExecutionResults results;
		String output;
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream outStream = new PrintStream(buffer, false, StandardCharsets.UTF_8);
			System.setOut(outStream);
			// Use larger timeout to increase likelihood of the test being started when the timeout is reached
			var timeout = WINDOWS.isCurrentOs() ? "1 s" : "100 ms";
			results = executeDefaultPreInterruptCallbackTimeoutOnMethodTestCase(timeout, request -> request //
					.configurationParameter(Constants.EXTENSIONS_TIMEOUT_THREAD_DUMP_ENABLED_PROPERTY_NAME, "true"));
			output = buffer.toString(StandardCharsets.UTF_8);
		}
		finally {
			System.setOut(orgOutStream);
		}

		assertTestHasTimedOut(results.testEvents(), message(it -> it.startsWith(TC + "() timed out after")));
		assertTrue(interruptedTest.get());
		Thread thread = Thread.currentThread();

		assertThat(output) //
				.containsSubsequence(
					"Thread \"%s\" prio=%d Id=%d %s will be interrupted.".formatted(thread.getName(),
						thread.getPriority(), thread.threadId(), Thread.State.TIMED_WAITING), //
					"java.lang.Thread.sleep", //
					"%s.test(PreInterruptCallbackTests.java".formatted(
						DefaultPreInterruptCallbackTimeoutOnMethodTestCase.class.getName()));

		assertThat(output) //
				.containsSubsequence( //
					"junit-jupiter-timeout-watcher", //
					"%s.beforeThreadInterrupt".formatted(PreInterruptThreadDumpPrinter.class.getName()));
	}

	@Test
	void testCaseWithNoInterruptCallbackEnabled() {
		Events tests = executeDefaultPreInterruptCallbackTimeoutOnMethodTestCase("1 μs", UnaryOperator.identity()) //
				.testEvents();
		assertTestHasTimedOut(tests);
		assertTrue(interruptedTest.get());
	}

	private EngineExecutionResults executeDefaultPreInterruptCallbackTimeoutOnMethodTestCase(String timeout,
			UnaryOperator<LauncherDiscoveryRequestBuilder> configurer) {
		return executeTests(request -> configurer.apply(request //
				.selectors(selectClass(DefaultPreInterruptCallbackTimeoutOnMethodTestCase.class)) //
				.configurationParameter(Constants.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, timeout)));
	}

	@Test
	void testCaseWithDeclaredInterruptCallbackEnabled() {
		Events tests = executeTestsForClass(DefaultPreInterruptCallbackWithExplicitCallbackTestCase.class).testEvents();
		assertTestHasTimedOut(tests);
		assertTrue(interruptedTest.get());
		PreInterruptContext preInterruptContext = calledPreInterruptContext.get();
		assertNotNull(preInterruptContext);
		assertNotNull(preInterruptContext.getThreadToInterrupt());
		assertEquals(preInterruptContext.getThreadToInterrupt(), interruptedTestThread.get());
	}

	@Test
	void testCaseWithDeclaredInterruptCallbackEnabledWithSeparateThread() throws Exception {
		Events tests = executeTestsForClass(
			DefaultPreInterruptCallbackWithExplicitCallbackWithSeparateThreadTestCase.class).testEvents();
		assertOneFailedTest(tests);
		tests.failed().assertEventsMatchExactly(
			event(test(TC), finishedWithFailure(instanceOf(TimeoutException.class))));

		//Wait until the real test thread was interrupted due to executor.shutdown(), otherwise the asserts below will be flaky.
		testThreadExecutionDone.get(1, TimeUnit.SECONDS);

		assertTrue(interruptedTest.get());
		PreInterruptContext preInterruptContext = calledPreInterruptContext.get();
		assertNotNull(preInterruptContext);
		assertNotNull(preInterruptContext.getThreadToInterrupt());
		assertEquals(preInterruptContext.getThreadToInterrupt(), interruptedTestThread.get());
	}

	@Test
	void testCaseWithDeclaredInterruptCallbackThrowsException() {
		interruptCallbackShallThrowException.set(true);
		Events tests = executeTestsForClass(DefaultPreInterruptCallbackWithExplicitCallbackTestCase.class).testEvents();
		tests.failed().assertEventsMatchExactly(event(test(TC),
			finishedWithFailure(instanceOf(TimeoutException.class), message(TIMEOUT_ERROR_MSG),
				suppressed(0, instanceOf(InterruptedException.class)),
				suppressed(1, instanceOf(IllegalStateException.class)))));
		assertTrue(interruptedTest.get());
		PreInterruptContext preInterruptContext = calledPreInterruptContext.get();
		assertNotNull(preInterruptContext);
		assertNotNull(preInterruptContext.getThreadToInterrupt());
		assertEquals(preInterruptContext.getThreadToInterrupt(), interruptedTestThread.get());
	}

	private static void assertTestHasTimedOut(Events tests) {
		assertTestHasTimedOut(tests, message(TIMEOUT_ERROR_MSG));
	}

	private static void assertTestHasTimedOut(Events tests, Condition<Throwable> messageCondition) {
		assertOneFailedTest(tests);
		tests.failed().assertEventsMatchExactly(
			event(test(TC), finishedWithFailure(instanceOf(TimeoutException.class), messageCondition, //
				suppressed(0, instanceOf(InterruptedException.class))//
			)));
	}

	private static void assertOneFailedTest(Events tests) {
		tests.assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
	}

	static class TestPreInterruptCallback implements PreInterruptCallback {

		@Override
		public void beforeThreadInterrupt(PreInterruptContext preInterruptContext, ExtensionContext extensionContext) {
			assertNotNull(extensionContext);

			calledPreInterruptContext.set(preInterruptContext);
			if (interruptCallbackShallThrowException.get()) {
				throw new IllegalStateException("Test-Ex");
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class DefaultPreInterruptCallbackTimeoutOnMethodTestCase {
		@Test
		void test() throws InterruptedException {
			try {
				Thread.sleep(5_000);
			}
			catch (InterruptedException ex) {
				interruptedTest.set(true);
				interruptedTestThread.set(Thread.currentThread());
				throw ex;
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(TestPreInterruptCallback.class)
	static class DefaultPreInterruptCallbackWithExplicitCallbackTestCase {
		@Test
		@Timeout(value = 1, unit = TimeUnit.MICROSECONDS)
		void test() throws InterruptedException {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				interruptedTest.set(true);
				interruptedTestThread.set(Thread.currentThread());
				throw ex;
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(TestPreInterruptCallback.class)
	static class DefaultPreInterruptCallbackWithExplicitCallbackWithSeparateThreadTestCase {
		@Test
		@Timeout(value = 200, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
		void test() throws InterruptedException {
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ex) {
				interruptedTest.set(true);
				interruptedTestThread.set(Thread.currentThread());
				throw ex;
			}
			finally {
				testThreadExecutionDone.complete(null);
			}
		}
	}
}
