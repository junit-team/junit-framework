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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Timeout.ThreadMode.SAME_THREAD;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

/**
 * Integration tests for {@link TimeoutExtension} applied to test methods that
 * return an asynchronous completion signal.
 *
 * @since 6.2
 */
class AsyncTimeoutTests extends AbstractJupiterTestEngineTests {

	@Test
	void asyncTestMethodThatOverrunsTimesOutOnSameThread() {
		var results = executeTestsForClass(OverrunningAsyncTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1, finishedWithFailure(instanceOf(TimeoutException.class)));
	}

	@Test
	void asyncTestMethodThatOverrunsTimesOutOnSeparateThread() {
		var results = executeTestsForClass(OverrunningSeparateThreadAsyncTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1, finishedWithFailure(instanceOf(TimeoutException.class)));
	}

	@Test
	void asyncTestMethodCompletingWithinTimeoutSucceeds() {
		var results = executeTestsForClass(FastAsyncTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
		results.testEvents().assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void timedOutAsyncTestIsNeverReportedSuccessful() {
		// A never-completing async body with a timeout: the test must fail with
		// TimeoutException and must NOT be reported successful.
		var results = executeTestsForClass(NeverCompletingAsyncTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents().haveExactly(1, finishedWithFailure(instanceOf(TimeoutException.class)));
	}

	static class OverrunningAsyncTestCase {

		@Test
		@Timeout(value = 200, unit = MILLISECONDS, threadMode = SAME_THREAD)
		CompletionStage<?> overruns() {
			return new CompletableFuture<>();
		}
	}

	static class OverrunningSeparateThreadAsyncTestCase {

		@Test
		@Timeout(value = 200, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
		CompletionStage<?> overruns() {
			return new CompletableFuture<>();
		}
	}

	static class FastAsyncTestCase {

		@Test
		@Timeout(value = 5, unit = MILLISECONDS, threadMode = SAME_THREAD)
		CompletionStage<?> fast() {
			return CompletableFuture.supplyAsync(() -> {
				try {
					Thread.sleep(1);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return "done";
			});
		}
	}

	static class NeverCompletingAsyncTestCase {

		@Test
		@Timeout(value = 200, unit = MILLISECONDS, threadMode = SAME_THREAD)
		CompletionStage<?> never() {
			return new CompletableFuture<>();
		}
	}

}
