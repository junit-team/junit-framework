/*
 * Copyright 2015-2026 the original author or authors.
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.DemoMethodTestDescriptor;
import org.mockito.InOrder;

@TrackLogRecords
class CompositeEngineExecutionListenerTests {

	private final List<EngineExecutionListener> listeners = new ArrayList<>(
		List.of(new ThrowingEngineExecutionListener()));

	@Test
	void shouldNotThrowExceptionButLogIfDynamicTestRegisteredListenerMethodFails(LogRecordListener logRecordListener) {
		compositeEngineExecutionListener().dynamicTestRegistered(anyTestDescriptor());

		assertThatTestListenerErrorLogged(logRecordListener, "dynamicTestRegistered");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionStartedListenerMethodFails(LogRecordListener logRecordListener) {
		compositeEngineExecutionListener().executionStarted(anyTestDescriptor());

		assertThatTestListenerErrorLogged(logRecordListener, "executionStarted");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionSkippedListenerMethodFails(LogRecordListener logRecordListener) {
		compositeEngineExecutionListener().executionSkipped(anyTestDescriptor(), "deliberately skipped container");

		assertThatTestListenerErrorLogged(logRecordListener, "executionSkipped");
	}

	@Test
	void shouldNotThrowExceptionButLogIfExecutionFinishedListenerMethodFails(LogRecordListener logRecordListener) {
		compositeEngineExecutionListener().executionFinished(anyTestDescriptor(), anyTestExecutionResult());

		assertThatTestListenerErrorLogged(logRecordListener, "executionFinished");
	}

	@Test
	void shouldNotThrowExceptionButLogIfReportingEntryPublishedListenerMethodFails(
			LogRecordListener logRecordListener) {
		compositeEngineExecutionListener().reportingEntryPublished(anyTestDescriptor(), ReportEntry.from("one", "two"));

		assertThatTestListenerErrorLogged(logRecordListener, "reportingEntryPublished");
	}

	@Test
	void shouldThrowOutOfMemoryExceptionAndStopListenerWithoutLog(LogRecordListener logRecordListener) {
		listeners.clear();
		listeners.add(new EngineExecutionListener() {
			@Override
			public void executionStarted(TestDescriptor testDescriptor) {
				throw new OutOfMemoryError();
			}
		});
		var testDescriptor = anyTestDescriptor();

		assertThatThrownBy(() -> compositeEngineExecutionListener().executionStarted(testDescriptor)).isInstanceOf(
			OutOfMemoryError.class);

		assertNotLogs(logRecordListener);
	}

	@Test
	void callsListenersInReverseOrderForFinishedEvents() {
		listeners.clear();
		var firstListener = mock(EngineExecutionListener.class, "firstListener");
		var secondListener = mock(EngineExecutionListener.class, "secondListener");
		listeners.add(firstListener);
		listeners.add(secondListener);

		var testDescriptor = anyTestDescriptor();
		var testExecutionResult = anyTestExecutionResult();

		var composite = compositeEngineExecutionListener();
		composite.executionStarted(testDescriptor);
		composite.executionFinished(testDescriptor, testExecutionResult);

		InOrder inOrder = inOrder(firstListener, secondListener);
		inOrder.verify(firstListener).executionStarted(testDescriptor);
		inOrder.verify(secondListener).executionStarted(testDescriptor);
		inOrder.verify(secondListener).executionFinished(testDescriptor, testExecutionResult);
		inOrder.verify(firstListener).executionFinished(testDescriptor, testExecutionResult);
	}

	private EngineExecutionListener compositeEngineExecutionListener() {
		return new CompositeEngineExecutionListener(listeners);
	}

	private LogRecord firstWarnLogRecord(LogRecordListener logRecordListener) throws AssertionError {
		return logRecordListener.stream(CompositeEngineExecutionListener.class, Level.WARNING).findFirst().orElseThrow(
			() -> new AssertionError("Failed to find error log record"));
	}

	private void assertNotLogs(LogRecordListener logRecordListener) throws AssertionError {
		assertThat(logRecordListener.stream(CompositeEngineExecutionListener.class, Level.WARNING).count()).isZero();
	}

	private static TestExecutionResult anyTestExecutionResult() {
		return mock();
	}

	private void assertThatTestListenerErrorLogged(LogRecordListener logRecordListener, String methodName) {
		assertThat(firstWarnLogRecord(logRecordListener).getMessage()).startsWith("EngineExecutionListener ["
				+ ThrowingEngineExecutionListener.class.getName() + "] threw exception for method: " + methodName);
	}

	private static TestDescriptor anyTestDescriptor() {
		var testClass = CompositeEngineExecutionListenerTests.class;
		var method = ReflectionUtils.findMethod(testClass, "anyTestDescriptor", new Class<?>[0]).orElseThrow();
		return new DemoMethodTestDescriptor(UniqueId.root("method", "unique_id"), method);
	}

	private static class ThrowingEngineExecutionListener implements EngineExecutionListener {

		@Override
		public void dynamicTestRegistered(TestDescriptor testDescriptor) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void executionStarted(TestDescriptor testDescriptor) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void executionSkipped(TestDescriptor testDescriptor, String reason) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
			throw new RuntimeException("failed to invoke listener");
		}

		@Override
		public void fileEntryPublished(TestDescriptor testDescriptor, FileEntry file) {
			throw new RuntimeException("failed to invoke listener");
		}
	}

}
