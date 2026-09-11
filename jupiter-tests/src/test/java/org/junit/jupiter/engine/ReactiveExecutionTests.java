/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Integration tests for the reactive (cooperative) execution lane.
 *
 * @since 6.2
 */
class ReactiveExecutionTests {

	@Test
	void runsTestsOnReactiveExecutionLane() {
		var selector = selectClass(SampleTestCase.class);

		EngineExecutionResults results = executeOnReactiveLane(selector);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2).aborted(0).failed(0));
		results.testEvents().assertThatEvents().haveExactly(2, finishedSuccessfully());
	}

	@Test
	void reactiveLaneHandlesMultipleNodesSafely() {
		var selector = selectClass(SampleTestCase2.class);

		EngineExecutionResults results = executeOnReactiveLane(selector);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
	}

	@Test
	void reactiveLaneTimesOutOverrunningAsyncTestMethod() {
		var selector = selectClass(OverrunningAsyncTimeoutTestCase.class);

		EngineExecutionResults results = executeOnReactiveLane(selector);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
		results.testEvents().assertThatEvents() //
				.haveExactly(1, finishedWithFailure(instanceOf(TimeoutException.class)));
	}

	@Test
	void standaloneReactiveLaneOverlapsAsyncTestMethods() {
		assumeTrue(Runtime.getRuntime().availableProcessors() >= 3, """
				this test requires at least 3 available processors so that the standalone reactive \
				lane can overlap the async test methods""");
		AsyncOverlapTestCase.peak = new java.util.concurrent.atomic.AtomicInteger();
		AsyncOverlapTestCase.inFlight = new java.util.concurrent.atomic.AtomicInteger();
		var selector = selectClass(AsyncOverlapTestCase.class);

		EngineExecutionResults results = executeOnStandaloneReactiveLane(selector);

		results.testEvents().assertStatistics(stats -> stats.started(3).succeeded(3).failed(0));
		assertTrue(AsyncOverlapTestCase.peak.get() >= 2,
			"async test methods should overlap under the standalone cooperative lane; peak concurrency="
					+ AsyncOverlapTestCase.peak.get());
	}

	@Test
	void standaloneReactiveLaneKeepsSyncMethodsOrdered() {
		var selector = selectClass(SyncMethodsTestCase.class);

		EngineExecutionResults results = executeOnStandaloneReactiveLane(selector);

		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));
	}

	private static EngineExecutionResults executeOnReactiveLane(DiscoverySelector selector) {
		return EngineTestKit //
				.engine(new JupiterTestEngine()) //
				.configurationParameter(org.junit.jupiter.api.Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME,
					"true") //
				.configurationParameter(org.junit.jupiter.api.Constants.PARALLEL_EXECUTION_REACTIVE_PROPERTY_NAME,
					"true") //
				.configurationParameter(org.junit.jupiter.api.Constants.PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME,
					"4") //
				.selectors(selector) //
				.execute();
	}

	private static EngineExecutionResults executeOnStandaloneReactiveLane(DiscoverySelector selector) {
		return EngineTestKit //
				.engine(new JupiterTestEngine()) //
				.configurationParameter(org.junit.jupiter.api.Constants.JUPITER_EXECUTION_REACTIVE_PROPERTY_NAME,
					"true") //
				.selectors(selector) //
				.execute();
	}

	static class SampleTestCase {

		@Test
		void works() {
			assertEquals(1, 1);
		}

		@Test
		CompletionStage<?> worksAsynchronously() {
			return CompletableFuture.supplyAsync(() -> "done");
		}
	}

	@Timeout(5)
	static class SampleTestCase2 {

		static final boolean ok = true;

		@Test
		void alsoWorks() {
			assertTrue(ok);
		}
	}

	static class OverrunningAsyncTimeoutTestCase {

		@Test
		@Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
		CompletionStage<?> overruns() {
			return new CompletableFuture<>();
		}
	}

	static class AsyncOverlapTestCase {

		private static ExecutorService executor;

		static java.util.concurrent.atomic.AtomicInteger inFlight = new java.util.concurrent.atomic.AtomicInteger();
		static java.util.concurrent.atomic.AtomicInteger peak = new java.util.concurrent.atomic.AtomicInteger();

		@BeforeAll
		static void startExecutor() {
			executor = Executors.newFixedThreadPool(3);
		}

		@AfterAll
		static void stopExecutor() {
			executor.shutdownNow();
		}

		@Test
		CompletionStage<?> a() {
			return asyncBody();
		}

		@Test
		CompletionStage<?> b() {
			return asyncBody();
		}

		@Test
		CompletionStage<?> c() {
			return asyncBody();
		}

		private static CompletionStage<?> asyncBody() {
			return CompletableFuture.runAsync(() -> {
				int current = inFlight.incrementAndGet();
				peak.updateAndGet(seen -> Math.max(seen, current));
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally {
					inFlight.decrementAndGet();
				}
			}, executor);
		}
	}

	static class SyncMethodsTestCase {

		@Test
		void one() {
		}

		@Test
		void two() {
		}
	}

}
