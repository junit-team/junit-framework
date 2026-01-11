/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;

import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * {@code TestExecutionResult} encapsulates the result of executing a single test
 * or container.
 *
 * <p>A {@code TestExecutionResult} consists of a mandatory
 * {@link #getStatus() Status} and an optional {@link #getThrowable() Throwable}.
 *
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public class TestExecutionResult {

	/**
	 * Status of executing a single test or container.
	 */
	public enum Status {

		/**
		 * Indicates that the execution of a test or container was
		 * <em>successful</em>.
		 */
		SUCCESSFUL,

		/**
		 * Indicates that the execution of a test or container was
		 * <em>aborted</em> (started but not finished).
		 */
		ABORTED,

		/**
		 * Indicates that the execution of a test or container <em>failed</em>.
		 */
		FAILED

	}

	private static final TestExecutionResult SUCCESSFUL_RESULT = new TestExecutionResult(SUCCESSFUL, null);

	/**
	 * Create a {@code TestExecutionResult} for a <em>successful</em> execution
	 * of a test or container.
	 *
	 * @return the {@code TestExecutionResult}; never {@code null}
	 */
	public static TestExecutionResult successful() {
		return SUCCESSFUL_RESULT;
	}

	/**
	 * Create a {@code TestExecutionResult} for an <em>aborted</em> execution
	 * of a test or container with the supplied {@link Throwable throwable}.
	 *
	 * @param throwable the throwable that caused the aborted execution; may be
	 * {@code null}
	 * @return the {@code TestExecutionResult}; never {@code null}
	 */
	public static TestExecutionResult aborted(@Nullable Throwable throwable) {
		return new TestExecutionResult(ABORTED, throwable);
	}

	/**
	 * Create a {@code TestExecutionResult} for a <em>failed</em> execution
	 * of a test or container with the supplied {@link Throwable throwable}.
	 *
	 * @param throwable the throwable that caused the failed execution; may be
	 * {@code null}
	 * @return the {@code TestExecutionResult}; never {@code null}
	 */
	public static TestExecutionResult failed(@Nullable Throwable throwable) {
		return new TestExecutionResult(FAILED, throwable);
	}

	private final Status status;

	private final @Nullable Throwable throwable;

	private TestExecutionResult(Status status, @Nullable Throwable throwable) {
		this.status = Preconditions.notNull(status, "Status must not be null");
		this.throwable = throwable;
	}

	/**
	 * Get the {@linkplain Status status} of this result.
	 *
	 * @return the status; never {@code null}
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Get the throwable that caused this result, if available.
	 *
	 * @return an {@code Optional} containing the throwable; never {@code null}
	 * but potentially empty
	 */
	public Optional<Throwable> getThrowable() {
		return Optional.ofNullable(throwable);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("status", status)
				.append("throwable", throwable)
				.toString();
		// @formatter:on
	}

}
