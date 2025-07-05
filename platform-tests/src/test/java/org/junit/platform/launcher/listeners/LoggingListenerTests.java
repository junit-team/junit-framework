/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.ArgumentMatchers;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

public class LoggingListenerTests {

	@Test
	void logsExecutionEvents() {
		BiConsumer<Throwable, String> logger = mock();

		executeTestCase(LoggingListener.forBiConsumer((t, m) -> {
			System.out.println(m.get());
			logger.accept(t, m.get());
		}));

		var inOrder = inOrder(logger);
		inOrder.verify(logger).accept(isNull(),
			startsWith("TestPlan Execution Started: org.junit.platform.launcher.TestPlan@"));
		inOrder.verify(logger).accept(isNull(), eq("Execution Started: JUnit Jupiter - [engine:junit-jupiter]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Started: LoggingListenerTests$TestCase - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Started: success() - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[test-factory:success()]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Dynamic Test Registered: dynamic - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[test-factory:success()]/[dynamic-test:#1]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Started: dynamic - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[test-factory:success()]/[dynamic-test:#1]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Finished: dynamic - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[test-factory:success()]/[dynamic-test:#1] - TestExecutionResult [status = SUCCESSFUL, throwable = null]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Finished: success() - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[test-factory:success()] - TestExecutionResult [status = SUCCESSFUL, throwable = null]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Skipped: skipped() - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[method:skipped()] - void org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase.skipped() is @Disabled"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Started: failed() - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[method:failed()]"));
		inOrder.verify(logger).accept(ArgumentMatchers.notNull(AssertionFailedError.class), eq(
			"Execution Finished: failed() - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[method:failed()] - TestExecutionResult [status = FAILED, throwable = org.opentest4j.AssertionFailedError]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Started: aborted() - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[method:aborted()]"));
		inOrder.verify(logger).accept(ArgumentMatchers.notNull(TestAbortedException.class), eq(
			"Execution Finished: aborted() - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase]/[method:aborted()] - TestExecutionResult [status = ABORTED, throwable = org.opentest4j.TestAbortedException]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Finished: LoggingListenerTests$TestCase - [engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.LoggingListenerTests$TestCase] - TestExecutionResult [status = SUCCESSFUL, throwable = null]"));
		inOrder.verify(logger).accept(isNull(), eq(
			"Execution Finished: JUnit Jupiter - [engine:junit-jupiter] - TestExecutionResult [status = SUCCESSFUL, throwable = null]"));
		inOrder.verify(logger).accept(isNull(),
			startsWith("TestPlan Execution Finished: org.junit.platform.launcher.TestPlan@"));
		inOrder.verifyNoMoreInteractions();
	}

	private static void executeTestCase(LoggingListener listener) {
		var config = LauncherConfig.builder() //
				.enableTestExecutionListenerAutoRegistration(false) //
				.addTestExecutionListeners() //
				.build();
		var request = request() //
				.selectors(selectClass(TestCase.class)) //
				.filters(includeEngines("junit-jupiter")) //
				.forExecution() //
				.listeners(listener) //
				.build();
		LauncherFactory.create(config) //
				.execute(request);
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	static class TestCase {
		@TestFactory
		@Order(1)
		Stream<DynamicTest> success() {
			return Stream.of(dynamicTest("dynamic", () -> {
			}));
		}

		@Test
		@Disabled
		@Order(2)
		void skipped() {
		}

		@Test
		@Order(3)
		void failed() {
			fail();
		}

		@Test
		@Order(4)
		void aborted() {
			abort();
		}
	}
}
