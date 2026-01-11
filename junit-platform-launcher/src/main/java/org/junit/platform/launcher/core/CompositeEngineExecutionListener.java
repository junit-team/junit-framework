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
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;

class CompositeEngineExecutionListener implements EngineExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(CompositeEngineExecutionListener.class);

	private final List<EngineExecutionListener> engineExecutionListeners;

	CompositeEngineExecutionListener(List<EngineExecutionListener> engineExecutionListeners) {
		this.engineExecutionListeners = new ArrayList<>(engineExecutionListeners);
	}

	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		notifyEach(engineExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.dynamicTestRegistered(testDescriptor),
			() -> "dynamicTestRegistered(" + testDescriptor + ")");
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		notifyEach(engineExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.executionSkipped(testDescriptor, reason),
			() -> "executionSkipped(" + testDescriptor + ", " + reason + ")");
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		notifyEach(engineExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.executionStarted(testDescriptor), () -> "executionStarted(" + testDescriptor + ")");
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		notifyEach(engineExecutionListeners, IterationOrder.REVERSED,
			listener -> listener.executionFinished(testDescriptor, testExecutionResult),
			() -> "executionFinished(" + testDescriptor + ", " + testExecutionResult + ")");
	}

	@Override
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		notifyEach(engineExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.reportingEntryPublished(testDescriptor, entry),
			() -> "reportingEntryPublished(" + testDescriptor + ", " + entry + ")");
	}

	@Override
	public void fileEntryPublished(TestDescriptor testDescriptor, FileEntry file) {
		notifyEach(engineExecutionListeners, IterationOrder.ORIGINAL,
			listener -> listener.fileEntryPublished(testDescriptor, file),
			() -> "fileEntryPublished(" + testDescriptor + ", " + file + ")");
	}

	private static <T extends EngineExecutionListener> void notifyEach(List<T> listeners, IterationOrder iterationOrder,
			Consumer<T> consumer, Supplier<String> description) {
		iterationOrder.forEach(listeners, listener -> {
			try {
				consumer.accept(listener);
			}
			catch (Throwable throwable) {
				UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
				logger.warn(throwable, () -> "EngineExecutionListener [%s] threw exception for method: %s".formatted(
					listener.getClass().getName(), description.get()));
			}
		});
	}
}
