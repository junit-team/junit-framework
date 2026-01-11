/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import static java.lang.String.join;
import static java.util.Collections.synchronizedList;

import java.io.PrintWriter;
import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Mutable, internal implementation of the {@link TestExecutionSummary} API.
 *
 * @since 1.0
 */
class MutableTestExecutionSummary implements TestExecutionSummary {

	private static final String TAB = "  ";
	private static final String DOUBLE_TAB = TAB + TAB;
	private static final int DEFAULT_MAX_STACKTRACE_LINES = 10;

	private static final String CAUSED_BY = "Caused by: ";
	private static final String SUPPRESSED = "Suppressed: ";
	private static final String CIRCULAR = "Circular reference: ";

	final AtomicLong containersFound = new AtomicLong();
	final AtomicLong containersStarted = new AtomicLong();
	final AtomicLong containersSkipped = new AtomicLong();
	final AtomicLong containersAborted = new AtomicLong();
	final AtomicLong containersSucceeded = new AtomicLong();
	final AtomicLong containersFailed = new AtomicLong();

	final AtomicLong testsFound = new AtomicLong();
	final AtomicLong testsStarted = new AtomicLong();
	final AtomicLong testsSkipped = new AtomicLong();
	final AtomicLong testsAborted = new AtomicLong();
	final AtomicLong testsSucceeded = new AtomicLong();
	final AtomicLong testsFailed = new AtomicLong();

	private final TestPlan testPlan;
	private final List<Failure> failures = synchronizedList(new ArrayList<>());
	private final long timeStarted;
	private final long timeStartedNanos;
	long timeFinished;
	long timeFinishedNanos;

	MutableTestExecutionSummary(TestPlan testPlan) {
		this.testPlan = testPlan;
		this.containersFound.set(testPlan.countTestIdentifiers(TestIdentifier::isContainer));
		this.testsFound.set(testPlan.countTestIdentifiers(TestIdentifier::isTest));
		this.timeStarted = System.currentTimeMillis();
		this.timeStartedNanos = System.nanoTime();
	}

	void addFailure(TestIdentifier testIdentifier, Throwable throwable) {
		this.failures.add(new DefaultFailure(testIdentifier, throwable));
	}

	@Override
	public long getTimeStarted() {
		return this.timeStarted;
	}

	@Override
	public long getTimeFinished() {
		return this.timeFinished;
	}

	@Override
	public long getTotalFailureCount() {
		return getTestsFailedCount() + getContainersFailedCount();
	}

	@Override
	public long getContainersFoundCount() {
		return this.containersFound.get();
	}

	@Override
	public long getContainersStartedCount() {
		return this.containersStarted.get();
	}

	@Override
	public long getContainersSkippedCount() {
		return this.containersSkipped.get();
	}

	@Override
	public long getContainersAbortedCount() {
		return this.containersAborted.get();
	}

	@Override
	public long getContainersSucceededCount() {
		return this.containersSucceeded.get();
	}

	@Override
	public long getContainersFailedCount() {
		return this.containersFailed.get();
	}

	@Override
	public long getTestsFoundCount() {
		return this.testsFound.get();
	}

	@Override
	public long getTestsStartedCount() {
		return this.testsStarted.get();
	}

	@Override
	public long getTestsSkippedCount() {
		return this.testsSkipped.get();
	}

	@Override
	public long getTestsAbortedCount() {
		return this.testsAborted.get();
	}

	@Override
	public long getTestsSucceededCount() {
		return this.testsSucceeded.get();
	}

	@Override
	public long getTestsFailedCount() {
		return this.testsFailed.get();
	}

	@Override
	public void printTo(PrintWriter writer) {
		// @formatter:off
		writer.printf("%nTest run finished after %d ms%n"

			+ "[%10d containers found      ]%n"
			+ "[%10d containers skipped    ]%n"
			+ "[%10d containers started    ]%n"
			+ "[%10d containers aborted    ]%n"
			+ "[%10d containers successful ]%n"
			+ "[%10d containers failed     ]%n"

			+ "[%10d tests found           ]%n"
			+ "[%10d tests skipped         ]%n"
			+ "[%10d tests started         ]%n"
			+ "[%10d tests aborted         ]%n"
			+ "[%10d tests successful      ]%n"
			+ "[%10d tests failed          ]%n"
			+ "%n",

			Duration.ofNanos(this.timeFinishedNanos - this.timeStartedNanos).toMillis(),

			getContainersFoundCount(),
			getContainersSkippedCount(),
			getContainersStartedCount(),
			getContainersAbortedCount(),
			getContainersSucceededCount(),
			getContainersFailedCount(),

			getTestsFoundCount(),
			getTestsSkippedCount(),
			getTestsStartedCount(),
			getTestsAbortedCount(),
			getTestsSucceededCount(),
			getTestsFailedCount()
		);
		// @formatter:on

		writer.flush();
	}

	@Override
	public void printFailuresTo(PrintWriter writer) {
		printFailuresTo(writer, DEFAULT_MAX_STACKTRACE_LINES);
	}

	@Override
	public void printFailuresTo(PrintWriter writer, int maxStackTraceLines) {
		Preconditions.notNull(writer, "PrintWriter must not be null");
		Preconditions.condition(maxStackTraceLines >= 0, "maxStackTraceLines must be a positive number");

		if (getTotalFailureCount() > 0) {
			writer.printf("%nFailures (%d):%n", getTotalFailureCount());
			this.failures.forEach(failure -> {
				writer.printf("%s%s%n", TAB, describeTest(failure.getTestIdentifier()));
				printSource(writer, failure.getTestIdentifier());
				writer.printf("%s=> %s%n", DOUBLE_TAB, failure.getException());
				printStackTrace(writer, failure.getException(), maxStackTraceLines);
			});
			writer.flush();
		}
	}

	@Override
	public List<Failure> getFailures() {
		return Collections.unmodifiableList(failures);
	}

	private String describeTest(TestIdentifier testIdentifier) {
		List<String> descriptionParts = new ArrayList<>();
		collectTestDescription(testIdentifier, descriptionParts);
		return join(":", descriptionParts);
	}

	private void collectTestDescription(TestIdentifier identifier, List<String> descriptionParts) {
		descriptionParts.add(0, identifier.getDisplayName());
		this.testPlan.getParent(identifier).ifPresent(parent -> collectTestDescription(parent, descriptionParts));
	}

	private void printSource(PrintWriter writer, TestIdentifier testIdentifier) {
		testIdentifier.getSource().ifPresent(source -> writer.printf("%s%s%n", DOUBLE_TAB, source));
	}

	private void printStackTrace(PrintWriter writer, Throwable throwable, int max) {
		if (throwable.getCause() != null
				|| (throwable.getSuppressed() != null && throwable.getSuppressed().length > 0)) {
			max = max / 2;
		}
		printStackTrace(writer, new StackTraceElement[] {}, throwable, "", DOUBLE_TAB + " ", new HashSet<>(), max);
		writer.flush();
	}

	private void printStackTrace(PrintWriter writer, StackTraceElement[] parentTrace, Throwable throwable,
			String caption, String indentation, Set<Throwable> seenThrowables, int max) {
		if (seenThrowables.contains(throwable)) {
			writer.printf("%s%s[%s%s]%n", indentation, TAB, CIRCULAR, throwable);
			return;
		}
		seenThrowables.add(throwable);

		StackTraceElement[] trace = throwable.getStackTrace();
		if (parentTrace.length > 0) {
			writer.printf("%s%s%s%n", indentation, caption, throwable);
		}
		int duplicates = numberOfCommonFrames(trace, parentTrace);
		int numDistinctFrames = trace.length - duplicates;
		int numDisplayLines = Math.min(numDistinctFrames, max);
		for (int i = 0; i < numDisplayLines; i++) {
			writer.printf("%s%s%s%n", indentation, TAB, trace[i]);
		}
		if (trace.length > max || duplicates != 0) {
			writer.printf("%s%s%s%n", indentation, TAB, "[...]");
		}

		for (Throwable suppressed : throwable.getSuppressed()) {
			printStackTrace(writer, trace, suppressed, SUPPRESSED, indentation + TAB, seenThrowables, max);
		}
		if (throwable.getCause() != null) {
			printStackTrace(writer, trace, throwable.getCause(), CAUSED_BY, indentation, seenThrowables, max);
		}
	}

	private int numberOfCommonFrames(StackTraceElement[] currentTrace, StackTraceElement[] parentTrace) {
		int currentIndex = currentTrace.length - 1;
		for (int parentIndex = parentTrace.length - 1; currentIndex >= 0
				&& parentIndex >= 0; currentIndex--, parentIndex--) {
			if (!currentTrace[currentIndex].equals(parentTrace[parentIndex])) {
				break;
			}
		}
		return currentTrace.length - 1 - currentIndex;
	}

	private record DefaultFailure(TestIdentifier testIdentifier, Throwable exception) implements Failure {

		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public TestIdentifier getTestIdentifier() {
			return testIdentifier;
		}

		@Override
		public Throwable getException() {
			return exception;
		}
	}

}
