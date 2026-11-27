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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

/**
 * Integration tests for {@code @Test} methods that return an asynchronous
 * completion signal such as a {@link CompletionStage}.
 *
 * @since 6.0
 */
class CompletionStageTestMethodsTests extends AbstractJupiterTestEngineTests {

	@Test
	void stageReturningTestMethodCompletesSuccessfully() {
		var results = executeTestsForClass(StageReturningTestCase.class);
		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
	}

	@Test
	void futureReturningTestMethodCompletesSuccessfully() {
		var results = executeTestsForClass(FutureReturningTestCase.class);
		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
	}

	@Test
	void stageReturningTestMethodThatFailsAsynchronouslyIsReportedAsFailed() {
		var results = executeTestsForClass(FailingStageTestCase.class);
		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1, finishedWithFailure(instanceOf(IllegalStateException.class), message("async boom")));
	}

	@Test
	void stageReturningTestMethodAppliesLifecycleCallbacks() {
		LifecycleCallbackTestCase.beforeEachCount.set(0);
		LifecycleCallbackTestCase.afterEachCount.set(0);

		var results = executeTestsForClass(LifecycleCallbackTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));
		assertEquals(2, LifecycleCallbackTestCase.beforeEachCount.get(), "each test method should run @BeforeEach");
		assertEquals(2, LifecycleCallbackTestCase.afterEachCount.get(), "each test method should run @AfterEach");
	}

	@Test
	void voidReturningTestMethodStillSupported() {
		var results = executeTestsForClass(VoidTestCase.class);
		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
	}

	@Test
	void timeoutAppliesToAwaitedStageReturningBody() {
		var results = executeTestsForClass(TimedOutStageTestCase.class);
		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1, finishedWithFailure(instanceOf(TimeoutException.class)));
	}

	// ----------------------------------------------------------------------

	static class StageReturningTestCase {

		@Test
		CompletionStage<?> onStage() {
			return completedFuture("done");
		}
	}

	static class FutureReturningTestCase {

		@Test
		CompletableFuture<?> onFuture() {
			return completedFuture("");
		}
	}

	static class FailingStageTestCase {

		@Test
		CompletionStage<?> onStage() {
			return CompletableFuture.failedFuture(new IllegalStateException("async boom"));
		}
	}

	static class LifecycleCallbackTestCase {

		static final AtomicInteger beforeEachCount = new AtomicInteger();
		static final AtomicInteger afterEachCount = new AtomicInteger();

		@BeforeEach
		void setUp() {
			beforeEachCount.incrementAndGet();
		}

		@AfterEach
		void tearDown() {
			afterEachCount.incrementAndGet();
		}

		@Test
		CompletionStage<?> first() {
			return completedFuture("");
		}

		@Test
		CompletionStage<?> second() {
			return completedFuture("");
		}
	}

	static class VoidTestCase {

		@Test
		void doesNothing() {
			assertEquals(1, 1);
		}
	}

	static class TimedOutStageTestCase {

		@Test
		@Timeout(value = 200, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
		CompletableFuture<?> neverCompletes() {
			return new CompletableFuture<>();
		}
	}
}
