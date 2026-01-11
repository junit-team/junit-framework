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

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;

/**
 * Delays reporting of engine skipped/finished events so that exceptions thrown
 * by engines can be reported to listeners.
 *
 * @since 1.6
 */
class OutcomeDelayingEngineExecutionListener extends DelegatingEngineExecutionListener {

	private final TestDescriptor engineDescriptor;

	private volatile boolean engineStarted;

	private volatile @Nullable Outcome outcome;

	private volatile @Nullable String skipReason;

	private volatile @Nullable TestExecutionResult executionResult;

	OutcomeDelayingEngineExecutionListener(EngineExecutionListener delegate, TestDescriptor engineDescriptor) {
		super(delegate);
		this.engineDescriptor = engineDescriptor;
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		if (testDescriptor == engineDescriptor) {
			outcome = Outcome.SKIPPED;
			skipReason = reason;
		}
		else {
			super.executionSkipped(testDescriptor, reason);
		}
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		if (testDescriptor == engineDescriptor) {
			engineStarted = true;
		}
		super.executionStarted(testDescriptor);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult executionResult) {
		if (testDescriptor == engineDescriptor) {
			outcome = Outcome.FINISHED;
			this.executionResult = executionResult;
		}
		else {
			super.executionFinished(testDescriptor, executionResult);
		}
	}

	void reportEngineOutcome() {
		if (outcome == Outcome.FINISHED) {
			super.executionFinished(engineDescriptor, requireNonNull(executionResult));
		}
		else if (outcome == Outcome.SKIPPED) {
			super.executionSkipped(engineDescriptor, requireNonNull(skipReason));
		}
	}

	void reportEngineStartIfNecessary() {
		if (!engineStarted) {
			super.executionStarted(engineDescriptor);
		}
	}

	void reportEngineFailure(Throwable throwable) {
		Optional.ofNullable(this.executionResult) //
				.flatMap(TestExecutionResult::getThrowable) //
				.ifPresent(throwable::addSuppressed);
		super.executionFinished(engineDescriptor, TestExecutionResult.failed(throwable));
	}

	private enum Outcome {
		SKIPPED, FINISHED
	}

}
