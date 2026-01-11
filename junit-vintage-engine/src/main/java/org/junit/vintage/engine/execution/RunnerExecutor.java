/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.execution;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.engine.TestExecutionResult.failed;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.descriptor.TestSourceProvider;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class RunnerExecutor {

	private final EngineExecutionListener engineExecutionListener;
	private final CancellationToken cancellationToken;
	private final TestSourceProvider testSourceProvider = new TestSourceProvider();

	public RunnerExecutor(EngineExecutionListener engineExecutionListener, CancellationToken cancellationToken) {
		this.engineExecutionListener = engineExecutionListener;
		this.cancellationToken = cancellationToken;
	}

	public void execute(RunnerTestDescriptor runnerTestDescriptor) {
		if (cancellationToken.isCancellationRequested()) {
			engineExecutionListener.executionSkipped(runnerTestDescriptor, "Execution cancelled");
			return;
		}
		RunNotifier notifier = new CancellationTokenAwareRunNotifier(cancellationToken);
		var testRun = new TestRun(runnerTestDescriptor);
		var listener = new RunListenerAdapter(testRun, engineExecutionListener, testSourceProvider);
		notifier.addListener(listener);
		try {
			listener.testRunStarted(runnerTestDescriptor.getDescription());
			runnerTestDescriptor.getRunner().run(notifier);
			listener.testRunFinished();
		}
		catch (StoppedByUserException e) {
			reportEventsForCancellation(e, testRun);
		}
		catch (Throwable t) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(t);
			reportUnexpectedFailure(testRun, runnerTestDescriptor, failed(t));
		}
	}

	private void reportEventsForCancellation(StoppedByUserException exception, TestRun testRun) {
		testRun.getInProgressTestDescriptors().forEach(startedDescriptor -> {
			startedDescriptor.getChildren().forEach(child -> {
				if (!testRun.isFinishedOrSkipped(child)) {
					engineExecutionListener.executionSkipped(child, "Execution cancelled");
					testRun.markSkipped(child);
				}
			});
			engineExecutionListener.executionFinished(startedDescriptor, TestExecutionResult.aborted(exception));
			testRun.markFinished(startedDescriptor);
		});
	}

	private void reportUnexpectedFailure(TestRun testRun, RunnerTestDescriptor runnerTestDescriptor,
			TestExecutionResult result) {
		if (testRun.isNotStarted(runnerTestDescriptor)) {
			engineExecutionListener.executionStarted(runnerTestDescriptor);
		}
		engineExecutionListener.executionFinished(runnerTestDescriptor, result);
	}

}
