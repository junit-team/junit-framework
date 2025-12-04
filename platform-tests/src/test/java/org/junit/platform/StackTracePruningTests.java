/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Test cases for stacktrace pruning.
 *
 * <p>Note: the package {@code org.junit.platform} this class resides in is
 * chosen on purpose. If it was in {@code org.junit.platform.launcher}
 * stack traces would be fully pruned.
 *
 * @since 5.10
 */
class StackTracePruningTests {

	@Test
	void shouldPruneStackTraceByDefault() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.selectors(selectMethod(FailingTestTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceDoesNotContain(stackTrace,
			"jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:");
	}

	@Test
	void shouldPruneStackTraceWhenEnabled() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(FailingTestTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceDoesNotContain(stackTrace,
			"jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:");
	}

	@Test
	void shouldNotPruneStackTraceWhenDisabled() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "false") //
				.selectors(selectMethod(FailingTestTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				\\Qorg.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:\\E.+
				>>>>
				\\Qorg.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:\\E.+
				>>>>
				""");
	}

	@Test
	void shouldAlwaysKeepJupiterAssertionStackTraceElement() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(FailingTestTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				>>>>
				\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
				>>>>
				""");
	}

	@Test
	void shouldAlwaysKeepJupiterAssumptionStackTraceElement() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(FailingTestTestCase.class, "failingAssumption")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				>>>>
				\\Qorg.junit.jupiter.api.Assumptions.assumeTrue(Assumptions.java:\\E.+
				>>>>
				""");
	}

	@Test
	void shouldKeepExactlyEverythingAfterTestCall() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(FailingTestTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace,
			"""
					\\Qorg.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:\\E.+
					\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
					\\Qorg.junit.platform.StackTracePruningTests$FailingTestTestCase.failingAssertion(StackTracePruningTests.java:\\E.+
					""");
	}

	@ParameterizedTest
	@ValueSource(strings = { "org.junit.platform.StackTracePruningTests$FailingBeforeEachTestCase",
			"org.junit.platform.StackTracePruningTests$FailingBeforeEachTestCase$NestedTestCase",
			"org.junit.platform.StackTracePruningTests$FailingBeforeEachTestCase$NestedTestCase$NestedNestedTestCase" })
	void shouldKeepExactlyEverythingAfterLifecycleMethodCall(Class<?> methodClass) {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(methodClass, "test")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace,
			"""
					\\Qorg.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:\\E.+
					\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
					\\Qorg.junit.platform.StackTracePruningTests$FailingBeforeEachTestCase.setUp(StackTracePruningTests.java:\\E.+
					""");
	}

	@Test
	void shouldPruneStackTracesOfSuppressedExceptions() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(FailingTestTestCase.class, "multipleFailingAssertions")) //
				.execute();

		Throwable throwable = getThrowable(results);

		for (Throwable suppressed : throwable.getSuppressed()) {
			List<StackTraceElement> stackTrace = Arrays.asList(suppressed.getStackTrace());
			assertStackTraceDoesNotContain(stackTrace,
				"jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:");
		}
	}

	private static List<StackTraceElement> extractStackTrace(EngineExecutionResults results) {
		return Arrays.asList(getThrowable(results).getStackTrace());
	}

	private static Throwable getThrowable(EngineExecutionResults results) {
		var failedTestEvent = results.testEvents().failed().list().getFirst();
		var testResult = failedTestEvent.getRequiredPayload(TestExecutionResult.class);
		return testResult.getThrowable().orElseThrow();
	}

	private static void assertStackTraceMatch(List<StackTraceElement> stackTrace, String expectedLines) {
		List<String> stackStraceAsLines = stackTrace.stream() //
				.map(StackTraceElement::toString) //
				.toList();
		assertLinesMatch(expectedLines.lines().toList(), stackStraceAsLines);
	}

	private static void assertStackTraceDoesNotContain(List<StackTraceElement> stackTrace, String element) {
		String stackStraceAsString = stackTrace.stream() //
				.map(StackTraceElement::toString) //
				.collect(Collectors.joining());
		assertThat(stackStraceAsString).doesNotContain(element);
	}

	// -------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FailingTestTestCase {

		@Test
		void failingAssertion() {
			Assertions.fail();
		}

		@Test
		void multipleFailingAssertions() {
			Assertions.assertAll(Assertions::fail, Assertions::fail);
		}

		@Test
		void failingAssumption() {
			Assumptions.assumeTrue(() -> {
				throw new RuntimeException();
			});
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FailingBeforeEachTestCase {

		@BeforeEach
		void setUp() {
			Assertions.fail();
		}

		@Test
		void test() {
		}

		@Nested
		class NestedTestCase {

			@Test
			void test() {
			}

			@Nested
			class NestedNestedTestCase {

				@Test
				void test() {
				}

			}

		}

	}

}
