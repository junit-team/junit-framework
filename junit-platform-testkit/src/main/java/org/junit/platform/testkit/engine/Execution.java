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

import java.time.Duration;
import java.time.Instant;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;

/**
 * {@code Execution} encapsulates metadata for the execution of a single
 * {@link TestDescriptor}.
 *
 * @since 1.4
 */
@API(status = MAINTAINED, since = "1.7")
public class Execution {

	// --- Factories -----------------------------------------------------------

	/**
	 * Create a new instance of an {@code Execution} that finished with the
	 * provided {@link TestExecutionResult}.
	 *
	 * @param testDescriptor the {@code TestDescriptor} that finished;
	 * never {@code null}
	 * @param startInstant the {@code Instant} that the {@code Execution} started;
	 * never {@code null}
	 * @param endInstant the {@code Instant} that the {@code Execution} completed;
	 * never {@code null}
	 * @param executionResult the {@code TestExecutionResult} of the finished
	 * {@code TestDescriptor}; never {@code null}
	 * @return the newly created {@code Execution} instance; never {@code null}
	 */
	public static Execution finished(TestDescriptor testDescriptor, Instant startInstant, Instant endInstant,
			TestExecutionResult executionResult) {

		return new Execution(testDescriptor, startInstant, endInstant, TerminationInfo.executed(executionResult));
	}

	/**
	 * Create a new instance of an {@code Execution} that was skipped with the
	 * provided {@code skipReason}.
	 *
	 * @param testDescriptor the {@code TestDescriptor} that finished;
	 * never {@code null}
	 * @param startInstant the {@code Instant} that the {@code Execution} started;
	 * never {@code null}
	 * @param endInstant the {@code Instant} that the {@code Execution} completed;
	 * never {@code null}
	 * @param skipReason the reason the {@code TestDescriptor} was skipped;
	 * may be {@code null}
	 * @return the newly created {@code Execution} instance; never {@code null}
	 */
	public static Execution skipped(TestDescriptor testDescriptor, Instant startInstant, Instant endInstant,
			String skipReason) {

		return new Execution(testDescriptor, startInstant, endInstant, TerminationInfo.skipped(skipReason));
	}

	// -------------------------------------------------------------------------

	private final TestDescriptor testDescriptor;
	private final Instant startInstant;
	private final Instant endInstant;
	private final Duration duration;
	private final TerminationInfo terminationInfo;

	private Execution(TestDescriptor testDescriptor, Instant startInstant, Instant endInstant,
			TerminationInfo terminationInfo) {

		Preconditions.notNull(testDescriptor, "TestDescriptor must not be null");
		Preconditions.notNull(startInstant, "Start Instant must not be null");
		Preconditions.notNull(endInstant, "End Instant must not be null");
		Preconditions.notNull(terminationInfo, "TerminationInfo must not be null");

		this.testDescriptor = testDescriptor;
		this.startInstant = startInstant;
		this.endInstant = endInstant;
		this.duration = Duration.between(startInstant, endInstant);
		this.terminationInfo = terminationInfo;
	}

	/**
	 * Get the {@link TestDescriptor} for this {@code Execution}.
	 *
	 * @return the {@code TestDescriptor} for this {@code Execution}
	 */
	public TestDescriptor getTestDescriptor() {
		return this.testDescriptor;
	}

	/**
	 * Get the start {@link Instant} of this {@code Execution}.
	 *
	 * @return the start {@code Instant} of this {@code Execution}
	 */
	public Instant getStartInstant() {
		return this.startInstant;
	}

	/**
	 * Get the end {@link Instant} of this {@code Execution}.
	 *
	 * @return the end {@code Instant} of this {@code Execution}
	 */
	public Instant getEndInstant() {
		return this.endInstant;
	}

	/**
	 * Get the {@link Duration} of this {@code Execution}.
	 *
	 * @return the {@code Duration} of this {@code Execution}
	 */
	public Duration getDuration() {
		return this.duration;
	}

	/**
	 * Get the {@link TerminationInfo} for this {@code Execution}.
	 *
	 * @return the {@code TerminationInfo} for this {@code Execution}
	 */
	public TerminationInfo getTerminationInfo() {
		return this.terminationInfo;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("testDescriptor", this.testDescriptor)
				.append("startInstant", this.startInstant)
				.append("endInstant", this.endInstant)
				.append("duration", this.duration)
				.append("terminationInfo", this.terminationInfo)
				.toString();
		// @formatter:on
	}

}
