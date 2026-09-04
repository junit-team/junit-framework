/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

/**
 * Integration tests for lifecycle methods ({@code @BeforeAll}, {@code @AfterAll},
 * {@code @BeforeEach}, and {@code @AfterEach}) that return an asynchronous completion
 * signal such as a {@link CompletionStage}.
 *
 * @since 6.2
 */
class CompletionStageLifecycleMethodsTests extends AbstractJupiterTestEngineTests {

	@Test
	void asyncBeforeEachAndAfterEachCompleteSuccessfully() {
		AsyncLifecycleTestCase.beforeEachBodyFlag.set(false);
		AsyncLifecycleTestCase.afterEachBodyFlag.set(false);

		var results = executeTestsForClass(AsyncLifecycleTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
		assertTrue(AsyncLifecycleTestCase.beforeEachBodyFlag.get(),
			"test body should only run after the @BeforeEach stage completes");
		assertTrue(AsyncLifecycleTestCase.afterEachBodyFlag.get(),
			"test result should only be reported after the @AfterEach stage completes");
	}

	@Test
	void asyncBeforeAllAndAfterAllCompleteSuccessfully() {
		AsyncClassLifecycleTestCase.beforeAllBodyFlag.set(false);
		AsyncClassLifecycleTestCase.afterAllBodyFlag.set(false);

		var results = executeTestsForClass(AsyncClassLifecycleTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
		assertTrue(AsyncClassLifecycleTestCase.beforeAllBodyFlag.get(),
			"test body should only run after the @BeforeAll stage completes");
		assertTrue(AsyncClassLifecycleTestCase.afterAllBodyFlag.get(),
			"@AfterAll stage should complete before the result is reported");
	}

	@Test
	void asyncBeforeEachThatFailsAsynchronouslyIsReportedAsFailed() {
		var results = executeTestsForClass(FailingAsyncBeforeEachTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1,
					finishedWithFailure(instanceOf(IllegalStateException.class), message("async before boom")));
	}

	@Test
	void asyncAfterEachThatFailsAsynchronouslyIsReportedAsFailed() {
		var results = executeTestsForClass(FailingAsyncAfterEachTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1,
					finishedWithFailure(instanceOf(IllegalStateException.class), message("async after boom")));
	}

	@Test
	void timeoutAppliesToAwaitedAsyncBeforeEachStage() {
		var results = executeTestsForClass(TimedOutAsyncBeforeEachTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1, finishedWithFailure(instanceOf(TimeoutException.class)));
	}

	@Test
	void voidLifecycleMethodsStillSupported() {
		var results = executeTestsForClass(VoidLifecycleTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
	}

	// ----------------------------------------------------------------------

	static class AsyncLifecycleTestCase {

		static final AtomicBoolean beforeEachBodyFlag = new AtomicBoolean();
		static final AtomicBoolean afterEachBodyFlag = new AtomicBoolean();

		@BeforeEach
		CompletionStage<?> setUpOnAnotherThread() {
			return CompletableFuture.runAsync(() -> beforeEachBodyFlag.set(true));
		}

		@AfterEach
		CompletionStage<?> tearDownOnAnotherThread() {
			return CompletableFuture.runAsync(() -> afterEachBodyFlag.set(true));
		}

		@Test
		CompletionStage<?> stageReturningTest() {
			return completedFuture("");
		}
	}

	static class AsyncClassLifecycleTestCase {

		static final AtomicBoolean beforeAllBodyFlag = new AtomicBoolean();
		static final AtomicBoolean afterAllBodyFlag = new AtomicBoolean();

		@BeforeAll
		static CompletionStage<?> setUpClassOnAnotherThread() {
			return CompletableFuture.runAsync(() -> beforeAllBodyFlag.set(true));
		}

		@AfterAll
		static CompletionStage<?> tearDownClassOnAnotherThread() {
			return CompletableFuture.runAsync(() -> afterAllBodyFlag.set(true));
		}

		@Test
		CompletionStage<?> stageReturningTest() {
			return completedFuture("");
		}
	}

	static class FailingAsyncBeforeEachTestCase {

		@BeforeEach
		CompletionStage<?> failingSetUp() {
			return CompletableFuture.failedFuture(new IllegalStateException("async before boom"));
		}

		@Test
		CompletionStage<?> neverRuns() {
			return completedFuture("");
		}
	}

	static class FailingAsyncAfterEachTestCase {

		@AfterEach
		CompletionStage<?> failingTearDown() {
			return CompletableFuture.failedFuture(new IllegalStateException("async after boom"));
		}

		@Test
		CompletionStage<?> runsFine() {
			return CompletableFuture.runAsync(() -> {
			});
		}
	}

	static class TimedOutAsyncBeforeEachTestCase {

		@BeforeEach
		@Timeout(value = 200, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
		CompletableFuture<?> neverCompletes() {
			return new CompletableFuture<>();
		}

		@Test
		CompletionStage<?> neverRuns() {
			return completedFuture("");
		}
	}

	static class VoidLifecycleTestCase {

		@BeforeEach
		void setUpVoid() {
		}

		@AfterEach
		void tearDownVoid() {
		}

		@Test
		void plainTest() {
		}
	}
}
