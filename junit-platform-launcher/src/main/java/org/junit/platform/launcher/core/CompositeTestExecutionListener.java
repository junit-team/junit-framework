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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

class CompositeTestExecutionListener implements TestExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(CompositeTestExecutionListener.class);

	private final List<TestExecutionListener> testExecutionListeners;
	private final List<EagerTestExecutionListener> eagerTestExecutionListeners;

	CompositeTestExecutionListener(List<TestExecutionListener> testExecutionListeners) {
		this.testExecutionListeners = new ArrayList<>(testExecutionListeners);
		this.eagerTestExecutionListeners = this.testExecutionListeners.stream() //
				.filter(EagerTestExecutionListener.class::isInstance) //
				.map(EagerTestExecutionListener.class::cast) //
				.toList();
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		notifyEach(testExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.dynamicTestRegistered(testIdentifier),
			() -> "dynamicTestRegistered(" + testIdentifier + ")");
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		notifyEach(testExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.executionSkipped(testIdentifier, reason),
			() -> "executionSkipped(" + testIdentifier + ", " + reason + ")");
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		notifyEach(eagerTestExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.executionJustStarted(testIdentifier),
			() -> "executionJustStarted(" + testIdentifier + ")");
		notifyEach(testExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.executionStarted(testIdentifier), () -> "executionStarted(" + testIdentifier + ")");
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		notifyEach(eagerTestExecutionListeners, IterationOrder.REVERSED,
			listener -> listener.executionJustFinished(testIdentifier, testExecutionResult),
			() -> "executionJustFinished(" + testIdentifier + ", " + testExecutionResult + ")");
		notifyEach(testExecutionListeners, IterationOrder.REVERSED,
			listener -> listener.executionFinished(testIdentifier, testExecutionResult),
			() -> "executionFinished(" + testIdentifier + ", " + testExecutionResult + ")");
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		notifyEach(testExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.testPlanExecutionStarted(testPlan),
			() -> "testPlanExecutionStarted(" + testPlan + ")");
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		notifyEach(testExecutionListeners, IterationOrder.REVERSED,
			listener -> listener.testPlanExecutionFinished(testPlan),
			() -> "testPlanExecutionFinished(" + testPlan + ")");
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		notifyEach(testExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.reportingEntryPublished(testIdentifier, entry),
			() -> "reportingEntryPublished(" + testIdentifier + ", " + entry + ")");
	}

	@Override
	public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
		notifyEach(testExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.fileEntryPublished(testIdentifier, file),
			() -> "fileEntryPublished(" + testIdentifier + ", " + file + ")");
	}

	private static <T extends TestExecutionListener> void notifyEach(List<T> listeners, IterationOrder iterationOrder,
			Consumer<T> consumer, Supplier<String> description) {
		iterationOrder.forEach(listeners, listener -> {
			try {
				consumer.accept(listener);
			}
			catch (Throwable throwable) {
				UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
				logger.warn(throwable, () -> "TestExecutionListener [%s] threw exception for method: %s".formatted(
					listener.getClass().getName(), description.get()));
			}
		});
	}

	interface EagerTestExecutionListener extends TestExecutionListener {
		default void executionJustStarted(TestIdentifier testIdentifier) {
		}

		default void executionJustFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		}
	}

}
