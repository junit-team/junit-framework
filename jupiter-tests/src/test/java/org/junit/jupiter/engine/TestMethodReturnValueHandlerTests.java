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

import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link org.junit.jupiter.api.extension.TestMethodReturnValueHandler}.
 *
 * @since 6.2
 */
class TestMethodReturnValueHandlerTests extends AbstractJupiterTestEngineTests {

	@Test
	void testMethodReturningCompletableFutureIsDiscoveredAndExecuted() {
		var results = executeTestsForClass(CompletableFutureTestCase.class);

		results.testEvents().assertStatistics(
			stats -> stats.started(3).succeeded(2).failed(1));
	}

	@Test
	void successfulCompletableFutureTestSucceeds() {
		var results = executeTestsForClass(CompletableFutureTestCase.class);

		results.testEvents().succeeded().assertEventsMatchLoosely(
			event(test("successfulAsyncTest"), finishedSuccessfully()),
			event(test("voidTestStillWorks"), finishedSuccessfully()));
	}

	@Test
	void failedCompletableFutureTestFails() {
		var results = executeTestsForClass(CompletableFutureTestCase.class);

		results.testEvents().failed().assertEventsMatchExactly(
			event(test("failingAsyncTest"), finishedWithFailure(
				instanceOf(RuntimeException.class), message("async failure"))));
	}

	@Test
	void nullReturnValueIsHandledGracefully() {
		var results = executeTestsForClass(NullReturnTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
		results.testEvents().succeeded().assertEventsMatchExactly(
			event(test("returnsNull"), finishedSuccessfully()));
	}

	@Test
	void unsupportedReturnTypeIsNotDiscovered() {
		var results = executeTestsForClass(UnsupportedReturnTypeTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.started(0));
	}

	// -------------------------------------------------------------------

	static class CompletableFutureTestCase {

		@Test
		CompletableFuture<String> successfulAsyncTest() {
			return CompletableFuture.completedFuture("hello");
		}

		@Test
		CompletableFuture<String> failingAsyncTest() {
			return CompletableFuture.failedFuture(new RuntimeException("async failure"));
		}

		@Test
		void voidTestStillWorks() {
		}
	}

	static class NullReturnTestCase {

		@SuppressWarnings("NullAway")
		@Test
		CompletableFuture<Void> returnsNull() {
			return null;
		}
	}

	static class UnsupportedReturnTypeTestCase {

		@Test
		String unsupportedReturnType() {
			return "not supported";
		}
	}

}
