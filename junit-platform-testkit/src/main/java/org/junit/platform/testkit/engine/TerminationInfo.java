/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestExecutionResult;

/**
 * {@code TerminationInfo} is a union type that allows propagation of terminated
 * container/test state, supporting either the <em>reason</em> if the container/test
 * was skipped or the {@link TestExecutionResult} if the container/test was executed.
 *
 * @since 1.4
 * @see Execution#getTerminationInfo()
 */
@API(status = MAINTAINED, since = "1.7")
public class TerminationInfo {

	// --- Factories -----------------------------------------------------------

	/**
	 * Create a <em>skipped</em> {@code TerminationInfo} instance for the
	 * supplied reason.
	 *
	 * @param reason the reason the execution was skipped; may be {@code null}
	 * @return the created {@code TerminationInfo}; never {@code null}
	 * @see #executed(TestExecutionResult)
	 */
	public static TerminationInfo skipped(@Nullable String reason) {
		return new TerminationInfo(true, reason, null);
	}

	/**
	 * Create an <em>executed</em> {@code TerminationInfo} instance for the
	 * supplied {@link TestExecutionResult}.
	 *
	 * @param testExecutionResult the result of the execution; never {@code null}
	 * @return the created {@code TerminationInfo}; never {@code null}
	 * @see #skipped(String)
	 */
	public static TerminationInfo executed(TestExecutionResult testExecutionResult) {
		Preconditions.notNull(testExecutionResult, "TestExecutionResult must not be null");
		return new TerminationInfo(false, null, testExecutionResult);
	}

	// -------------------------------------------------------------------------

	private final boolean skipped;

	private final @Nullable String skipReason;

	private final @Nullable TestExecutionResult testExecutionResult;

	private TerminationInfo(boolean skipped, @Nullable String skipReason,
			@Nullable TestExecutionResult testExecutionResult) {
		boolean executed = (testExecutionResult != null);
		Preconditions.condition((skipped ^ executed),
			"TerminationInfo must represent either a skipped execution or a TestExecutionResult but not both");

		this.skipped = skipped;
		this.skipReason = skipReason;
		this.testExecutionResult = testExecutionResult;
	}

	/**
	 * Determine if this {@code TerminationInfo} represents a skipped execution.
	 *
	 * @return {@code true} if this this {@code TerminationInfo} represents a
	 * skipped execution
	 */
	public boolean skipped() {
		return this.skipped;
	}

	/**
	 * Determine if this {@code TerminationInfo} does not represent a skipped
	 * execution.
	 *
	 * @return {@code true} if this this {@code TerminationInfo} does not
	 * represent a skipped execution
	 */
	public boolean notSkipped() {
		return !skipped();
	}

	/**
	 * Determine if this {@code TerminationInfo} represents a completed execution.
	 *
	 * @return {@code true} if this this {@code TerminationInfo} represents a
	 * completed execution
	 */
	public boolean executed() {
		return (this.testExecutionResult != null);
	}

	/**
	 * Get the reason the execution was skipped.
	 *
	 * @return the reason the execution was skipped
	 * @throws UnsupportedOperationException if this {@code TerminationInfo}
	 * does not represent a skipped execution
	 */
	public @Nullable String getSkipReason() throws UnsupportedOperationException {
		if (skipped()) {
			return this.skipReason;
		}
		// else
		throw new UnsupportedOperationException("No skip reason contained in this TerminationInfo");
	}

	/**
	 * Get the {@link TestExecutionResult} for the completed execution.
	 *
	 * @return the result of the completed execution
	 * @throws UnsupportedOperationException if this {@code TerminationInfo}
	 * does not represent a completed execution
	 */
	public TestExecutionResult getExecutionResult() throws UnsupportedOperationException {
		if (this.testExecutionResult != null) {
			return this.testExecutionResult;
		}
		// else
		throw new UnsupportedOperationException("No TestExecutionResult contained in this TerminationInfo");
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this);
		if (skipped()) {
			builder.append("skipped", true).append("reason", this.skipReason);
		}
		else {
			builder.append("executed", true).append("result", this.testExecutionResult);
		}
		return builder.toString();
	}

}
