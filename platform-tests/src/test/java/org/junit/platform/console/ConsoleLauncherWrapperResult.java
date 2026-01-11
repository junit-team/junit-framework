/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.platform.console.command.CommandResult;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * @since 1.0
 */
class ConsoleLauncherWrapperResult implements TestExecutionSummary {

	final String[] args;
	final String out;
	final String err;
	final int code;

	private final @Nullable TestExecutionSummary summary;

	ConsoleLauncherWrapperResult(String[] args, String out, String err, CommandResult<?> result) {
		this.args = args;
		this.out = out;
		this.err = err;
		this.code = result.getExitCode();
		this.summary = (TestExecutionSummary) result.getValue() //
				.filter(TestExecutionSummary.class::isInstance) //
				.orElse(null);
	}

	private void checkTestExecutionSummaryState() {
		if (summary == null) {
			throw new IllegalStateException("TestExecutionSummary not assigned. Exit code is: " + code);
		}
	}

	@Override
	public long getTimeStarted() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTimeStarted();
	}

	@Override
	public long getTimeFinished() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTimeFinished();
	}

	@Override
	public long getTotalFailureCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTotalFailureCount();
	}

	@Override
	public long getContainersFoundCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getContainersFoundCount();
	}

	@Override
	public long getContainersStartedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getContainersStartedCount();
	}

	@Override
	public long getContainersSkippedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getContainersSkippedCount();
	}

	@Override
	public long getContainersAbortedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getContainersAbortedCount();
	}

	@Override
	public long getContainersSucceededCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getContainersSucceededCount();
	}

	@Override
	public long getContainersFailedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getContainersFailedCount();
	}

	@Override
	public long getTestsFoundCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTestsFoundCount();
	}

	@Override
	public long getTestsStartedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTestsStartedCount();
	}

	@Override
	public long getTestsSkippedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTestsSkippedCount();
	}

	@Override
	public long getTestsAbortedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTestsAbortedCount();
	}

	@Override
	public long getTestsSucceededCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTestsSucceededCount();
	}

	@Override
	public long getTestsFailedCount() {
		checkTestExecutionSummaryState();
		return requiredSummary().getTestsFailedCount();
	}

	@Override
	public void printTo(PrintWriter writer) {
		checkTestExecutionSummaryState();
		requiredSummary().printTo(writer);
	}

	@Override
	public void printFailuresTo(PrintWriter writer) {
		checkTestExecutionSummaryState();
		requiredSummary().printFailuresTo(writer);
	}

	@Override
	public void printFailuresTo(PrintWriter writer, int maxStackTraceLines) {
		checkTestExecutionSummaryState();
		requiredSummary().printFailuresTo(writer, maxStackTraceLines);
	}

	@Override
	public List<Failure> getFailures() {
		checkTestExecutionSummaryState();
		return requiredSummary().getFailures();
	}

	private TestExecutionSummary requiredSummary() {
		return requireNonNull(summary);
	}
}
