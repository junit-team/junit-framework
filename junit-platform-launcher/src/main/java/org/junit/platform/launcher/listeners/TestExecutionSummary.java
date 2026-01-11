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

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.launcher.TestIdentifier;

/**
 * Summary of test plan execution.
 *
 * @since 1.0
 * @see SummaryGeneratingListener
 */
@API(status = MAINTAINED, since = "1.0")
public interface TestExecutionSummary {

	/**
	 * Get the timestamp (in milliseconds) when the test plan started.
	 */
	long getTimeStarted();

	/**
	 * Get the timestamp (in milliseconds) when the test plan finished.
	 */
	long getTimeFinished();

	/**
	 * Get the total number of {@linkplain #getContainersFailedCount failed
	 * containers} and {@linkplain #getTestsFailedCount failed tests}.
	 *
	 * @see #getTestsFailedCount()
	 * @see #getContainersFailedCount()
	 */
	long getTotalFailureCount();

	/**
	 * Get the number of containers found.
	 */
	long getContainersFoundCount();

	/**
	 * Get the number of containers started.
	 */
	long getContainersStartedCount();

	/**
	 * Get the number of containers skipped.
	 */
	long getContainersSkippedCount();

	/**
	 * Get the number of containers aborted.
	 */
	long getContainersAbortedCount();

	/**
	 * Get the number of containers that succeeded.
	 */
	long getContainersSucceededCount();

	/**
	 * Get the number of containers that failed.
	 *
	 * @see #getTestsFailedCount()
	 * @see #getTotalFailureCount()
	 */
	long getContainersFailedCount();

	/**
	 * Get the number of tests found.
	 */
	long getTestsFoundCount();

	/**
	 * Get the number of tests started.
	 */
	long getTestsStartedCount();

	/**
	 * Get the number of tests skipped.
	 */
	long getTestsSkippedCount();

	/**
	 * Get the number of tests aborted.
	 */
	long getTestsAbortedCount();

	/**
	 * Get the number of tests that succeeded.
	 */
	long getTestsSucceededCount();

	/**
	 * Get the number of tests that failed.
	 *
	 * @see #getContainersFailedCount()
	 * @see #getTotalFailureCount()
	 */
	long getTestsFailedCount();

	/**
	 * Print this summary to the supplied {@link PrintWriter}.
	 *
	 * <p>This method does not print failure messages.
	 *
	 * @see #printFailuresTo(PrintWriter)
	 */
	void printTo(PrintWriter writer);

	/**
	 * Print failed containers and tests, including sources and exception
	 * messages, to the supplied {@link PrintWriter}.
	 *
	 * @param writer the {@code PrintWriter} to which to print; never {@code null}
	 * @see #printTo(PrintWriter)
	 * @see #printFailuresTo(PrintWriter, int)
	 */
	void printFailuresTo(PrintWriter writer);

	/**
	 * Print failed containers and tests, including sources and exception
	 * messages, to the supplied {@link PrintWriter}.
	 *
	 * <p>The maximum number of lines to print for exception stack traces (if any)
	 * can be specified via the {@code maxStackTraceLines} argument.
	 *
	 * <p>By default, this method delegates to {@link #printFailuresTo(PrintWriter)},
	 * effectively ignoring the {@code maxStackTraceLines} argument. Concrete
	 * implementations of this interface should therefore override this default
	 * method in order to honor the {@code maxStackTraceLines} argument.
	 *
	 * @param writer the {@code PrintWriter} to which to print; never {@code null}
	 * @param maxStackTraceLines the maximum number of lines to print for exception
	 * stack traces; must be a positive value
	 * @since 1.6
	 * @see #printTo(PrintWriter)
	 * @see #printFailuresTo(PrintWriter)
	 */
	@API(status = MAINTAINED, since = "1.6")
	default void printFailuresTo(PrintWriter writer, int maxStackTraceLines) {
		printFailuresTo(writer);
	}

	/**
	 * Get an immutable list of the failures of the test plan execution.
	 */
	List<Failure> getFailures();

	/**
	 * Failure of a test or container.
	 */
	interface Failure extends Serializable {

		/**
		 * Get the identifier of the failed test or container.
		 *
		 * @return the {@link TestIdentifier} for this failure; never {@code null}
		 */
		TestIdentifier getTestIdentifier();

		/**
		 * Get the {@link Throwable} causing the failure.
		 *
		 * @return the {@link Throwable} for this failure; never {@code null}
		 */
		Throwable getException();
	}

}
