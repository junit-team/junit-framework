/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.io.IOException;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.junit.rules.ExpectedException;

/**
 * Integration tests for {@link ExpectedExceptionSupport}.
 *
 * @since 5.0
 */
@SuppressWarnings("removal")
class ExpectedExceptionSupportTests {

	@Test
	void expectedExceptionIsProcessedCorrectly() {
		Events tests = executeTestsForClass(ExpectedExceptionTestCase.class);

		tests.assertStatistics(stats -> stats.started(4).succeeded(1).aborted(0).failed(3));

		tests.succeeded().assertThatEvents().have(
			event(test("correctExceptionExpectedThrown"), finishedSuccessfully()));

		tests.failed().assertThatEvents()//
				.haveExactly(1, //
					event(test("noExceptionExpectedButThrown"), //
						finishedWithFailure(message("no exception expected")))) //
				.haveExactly(1, //
					event(test("exceptionExpectedButNotThrown"), //
						finishedWithFailure(instanceOf(AssertionError.class), //
							message("Expected test to throw an instance of java.lang.RuntimeException")))) //
				.haveExactly(1, //
					event(test("wrongExceptionExpected"), //
						finishedWithFailure(instanceOf(AssertionError.class), //
							message(value -> value.contains("Expected: an instance of java.io.IOException")))));
	}

	@Test
	void expectedExceptionSupportWithoutExpectedExceptionRule() {
		Class<?> testClass = ExpectedExceptionSupportWithoutExpectedExceptionRuleTestCase.class;
		Events tests = executeTestsForClass(testClass);

		tests.assertStatistics(stats -> stats.started(2).succeeded(1).aborted(0).failed(1));

		tests.succeeded().assertThatEvents().have(event(test("success"), finishedSuccessfully()));

		tests.failed().assertThatEvents()//
				.haveExactly(1, event(test("failure"), finishedWithFailure(message("must fail"))));
	}

	private Events executeTestsForClass(Class<?> testClass) {
		return EngineTestKit.execute("junit-jupiter", request().selectors(selectClass(testClass)).build()).testEvents();
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(ExpectedExceptionSupport.class)
	static class ExpectedExceptionTestCase {

		@SuppressWarnings("deprecation")
		@Rule
		public ExpectedException thrown = ExpectedException.none();

		@Test
		void noExceptionExpectedButThrown() {
			throw new RuntimeException("no exception expected");
		}

		@Test
		void exceptionExpectedButNotThrown() {
			thrown.expect(RuntimeException.class);
		}

		@Test
		void wrongExceptionExpected() {
			thrown.expect(IOException.class);
			throw new RuntimeException("wrong exception");
		}

		@Test
		void correctExceptionExpectedThrown() {
			thrown.expect(RuntimeException.class);
			throw new RuntimeException("right exception");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(ExpectedExceptionSupport.class)
	static class ExpectedExceptionSupportWithoutExpectedExceptionRuleTestCase {

		@Test
		void success() {
			/* no-op */
		}

		@Test
		void failure() {
			fail("must fail");
		}

	}

}
