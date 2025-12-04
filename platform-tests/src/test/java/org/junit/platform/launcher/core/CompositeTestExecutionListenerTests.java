/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.DemoMethodTestDescriptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.CompositeTestExecutionListener.EagerTestExecutionListener;
import org.mockito.InOrder;

@TrackLogRecords
class CompositeTestExecutionListenerTests {

	private final List<TestExecutionListener> listeners = new ArrayList<>(List.of(new ThrowingTestExecutionListener()));

	@Test
	void shouldNotThrowExceptionButLogIfDynamicTestRegisteredListenerMethodFails(LogRecordListener logRecordListener) {
		compositeTestExecutionListener().dynamicTestRegistered(anyTestIdentifier());

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingTestExecutionListener.class,
			"dynamicTestRegistered");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionStartedListenerMethodFails(LogRecordListener logRecordListener) {
		compositeTestExecutionListener().executionStarted(anyTestIdentifier());

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingTestExecutionListener.class, "executionStarted");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionSkippedListenerMethodFails(LogRecordListener logRecordListener) {
		compositeTestExecutionListener().executionSkipped(anyTestIdentifier(), "deliberately skipped container");

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingTestExecutionListener.class, "executionSkipped");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionFinishedListenerMethodFails(LogRecordListener logRecordListener) {
		compositeTestExecutionListener().executionFinished(anyTestIdentifier(), anyTestExecutionResult());

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingTestExecutionListener.class, "executionFinished");
	}

	@Test
	void shouldNotThrowExceptionButLogIfReportingEntryPublishedListenerMethodFails(
			LogRecordListener logRecordListener) {
		compositeTestExecutionListener().reportingEntryPublished(anyTestIdentifier(), ReportEntry.from("one", "two"));

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingTestExecutionListener.class,
			"reportingEntryPublished");
	}

	@Test
	void shouldNotThrowExceptionButLogIfTesPlanExecutionStartedListenerMethodFails(
			LogRecordListener logRecordListener) {
		compositeTestExecutionListener().testPlanExecutionStarted(anyTestPlan());

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingTestExecutionListener.class,
			"testPlanExecutionStarted");
	}

	@Test
	void shouldNotThrowExceptionButLogIfTesPlanExecutionFinishedListenerMethodFails(
			LogRecordListener logRecordListener) {
		compositeTestExecutionListener().testPlanExecutionFinished(anyTestPlan());

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingTestExecutionListener.class,
			"testPlanExecutionFinished");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionJustStartedEagerTestListenerMethodFails(
			LogRecordListener logRecordListener) {
		listeners.add(new ThrowingEagerTestExecutionListener());

		compositeTestExecutionListener().executionStarted(anyTestIdentifier());

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingEagerTestExecutionListener.class,
			"executionJustStarted");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionJustFinishedEagerTestListenerMethodFails(
			LogRecordListener logRecordListener) {
		listeners.add(new ThrowingEagerTestExecutionListener());

		compositeTestExecutionListener().executionFinished(anyTestIdentifier(), anyTestExecutionResult());

		assertThatTestListenerErrorLogged(logRecordListener, ThrowingEagerTestExecutionListener.class,
			"executionJustFinished");
	}

	@Test
	void shouldThrowOutOfMemoryExceptionAndStopListenerWithoutLog(LogRecordListener logRecordListener) {
		listeners.clear();
		listeners.add(new TestExecutionListener() {
			@Override
			public void executionStarted(TestIdentifier testIdentifier) {
				throw new OutOfMemoryError();
			}
		});

		assertThatThrownBy(() -> compositeTestExecutionListener().executionStarted(anyTestIdentifier())).isInstanceOf(
			OutOfMemoryError.class);

		assertNotLogs(logRecordListener);
	}

	@Test
	void shouldThrowOutOfMemoryExceptionAndStopEagerListenerWithoutLog(LogRecordListener logRecordListener) {
		listeners.add(new EagerTestExecutionListener() {
			@Override
			public void executionJustStarted(TestIdentifier testIdentifier) {
				throw new OutOfMemoryError();
			}
		});

		assertThatThrownBy(() -> compositeTestExecutionListener().executionStarted(anyTestIdentifier())).isInstanceOf(
			OutOfMemoryError.class);

		assertNotLogs(logRecordListener);
	}

	@Test
	void callsListenersInReverseOrderForFinishedEvents() {
		listeners.clear();
		var firstListener = mock(TestExecutionListener.class, "firstListener");
		var secondListener = mock(TestExecutionListener.class, "secondListener");
		listeners.add(firstListener);
		listeners.add(secondListener);

		var testPlan = anyTestPlan();
		var testIdentifier = anyTestIdentifier();
		var testExecutionResult = anyTestExecutionResult();

		var composite = compositeTestExecutionListener();
		composite.testPlanExecutionStarted(testPlan);
		composite.executionStarted(testIdentifier);
		composite.executionFinished(testIdentifier, testExecutionResult);
		composite.testPlanExecutionFinished(testPlan);

		InOrder inOrder = inOrder(firstListener, secondListener);
		inOrder.verify(firstListener).testPlanExecutionStarted(testPlan);
		inOrder.verify(secondListener).testPlanExecutionStarted(testPlan);
		inOrder.verify(firstListener).executionStarted(testIdentifier);
		inOrder.verify(secondListener).executionStarted(testIdentifier);
		inOrder.verify(secondListener).executionFinished(testIdentifier, testExecutionResult);
		inOrder.verify(firstListener).executionFinished(testIdentifier, testExecutionResult);
		inOrder.verify(secondListener).testPlanExecutionFinished(testPlan);
		inOrder.verify(firstListener).testPlanExecutionFinished(testPlan);
	}

	private TestExecutionListener compositeTestExecutionListener() {
		return new CompositeTestExecutionListener(listeners);
	}

	private LogRecord firstWarnLogRecord(LogRecordListener logRecordListener) throws AssertionError {
		return logRecordListener.stream(CompositeTestExecutionListener.class, Level.WARNING).findFirst().orElseThrow(
			() -> new AssertionError("Failed to find error log record"));
	}

	private void assertNotLogs(LogRecordListener logRecordListener) throws AssertionError {
		assertThat(logRecordListener.stream(CompositeTestExecutionListener.class, Level.WARNING).count()).isZero();
	}

	private static TestExecutionResult anyTestExecutionResult() {
		return TestExecutionResult.successful();
	}

	private static TestIdentifier anyTestIdentifier() {
		return TestIdentifier.from(anyTestDescriptor());
	}

	private void assertThatTestListenerErrorLogged(LogRecordListener logRecordListener, Class<?> listenerClass,
			String methodName) {
		assertThat(firstWarnLogRecord(logRecordListener).getMessage()).startsWith(
			"TestExecutionListener [" + listenerClass.getName() + "] threw exception for method: " + methodName);
	}

	private static TestPlan anyTestPlan() {
		return TestPlan.from(true, Set.of(anyTestDescriptor()), mock(), dummyOutputDirectoryCreator());
	}

	private static DemoMethodTestDescriptor anyTestDescriptor() {
		var testClass = CompositeTestExecutionListenerTests.class;
		var method = ReflectionUtils.findMethod(testClass, "anyTestDescriptor", new Class<?>[0]).orElseThrow();
		return new DemoMethodTestDescriptor(UniqueId.root("method", "unique_id"), method);
	}

	private static class ThrowingEagerTestExecutionListener extends ThrowingTestExecutionListener
			implements EagerTestExecutionListener {
		@Override
		public void executionJustStarted(TestIdentifier testIdentifier) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void executionJustFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
			throw new RuntimeException("failed to invoke listener");
		}
	}

	private static class ThrowingTestExecutionListener implements TestExecutionListener {
		@Override
		public void testPlanExecutionStarted(TestPlan testPlan) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void testPlanExecutionFinished(TestPlan testPlan) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void dynamicTestRegistered(TestIdentifier testIdentifier) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void executionStarted(TestIdentifier testIdentifier) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void executionSkipped(TestIdentifier testIdentifier, String reason) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
			throw new RuntimeException("failed to invoke listener");
		}
	}

}
