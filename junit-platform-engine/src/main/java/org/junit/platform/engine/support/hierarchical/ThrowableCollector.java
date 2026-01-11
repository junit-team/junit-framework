/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.TestExecutionResult;

/**
 * Simple component that can be used to collect one or more instances of
 * {@link Throwable}.
 *
 * <p>This class distinguishes between {@code Throwables} that <em>abort</em>
 * and those that <em>fail</em> test execution. The latter take precedence over
 * the former, i.e. if both types of {@code Throwables} were collected, the ones
 * that abort execution are reported as
 * {@linkplain Throwable#addSuppressed(Throwable) suppressed} {@code Throwables}
 * of the first {@code Throwable} that failed execution.
 *
 * @since 1.3
 * @see OpenTest4JAwareThrowableCollector
 */
@API(status = MAINTAINED, since = "1.3")
public class ThrowableCollector {

	private final Predicate<? super Throwable> abortedExecutionPredicate;

	private @Nullable Throwable throwable;

	/**
	 * Create a new {@code ThrowableCollector} that uses the supplied
	 * {@link Predicate} to determine whether a {@link Throwable}
	 * <em>aborted</em> or <em>failed</em> execution.
	 *
	 * @param abortedExecutionPredicate the predicate used to decide whether a
	 * {@code Throwable} aborted execution; never {@code null}.
	 */
	public ThrowableCollector(Predicate<? super Throwable> abortedExecutionPredicate) {
		this.abortedExecutionPredicate = Preconditions.notNull(abortedExecutionPredicate,
			"abortedExecutionPredicate must not be null");
	}

	/**
	 * Execute the supplied {@link Executable} and collect any {@link Throwable}
	 * thrown during the execution.
	 *
	 * <p>If the {@code Executable} throws an <em>unrecoverable</em> exception
	 * &mdash; for example, an {@link OutOfMemoryError} &mdash; this method will
	 * rethrow it.
	 *
	 * @param executable the {@code Executable} to execute
	 * @see #assertEmpty()
	 */
	public void execute(Executable executable) {
		try {
			executable.execute();
		}
		catch (Throwable t) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(t);
			add(t);
		}
	}

	/**
	 * Add the supplied {@link Throwable} to this {@code ThrowableCollector}.
	 *
	 * @param t the {@code Throwable} to add
	 * @see #execute(Executable)
	 * @see #assertEmpty()
	 */
	private void add(Throwable t) {
		Preconditions.notNull(t, "Throwable must not be null");

		if (this.throwable == null) {
			this.throwable = t;
		}
		else if (hasAbortedExecution(this.throwable) && !hasAbortedExecution(t)) {
			t.addSuppressed(this.throwable);
			this.throwable = t;
		}
		else if (throwable != t) {
			// Jupiter does not throw the same Throwable from Node.after() anymore but other engines might
			this.throwable.addSuppressed(t);
		}
	}

	/**
	 * Get the first {@link Throwable} collected by this
	 * {@code ThrowableCollector}.
	 *
	 * <p>If this collector is not empty, the first collected {@code Throwable}
	 * will be returned with any additional {@code Throwables}
	 * {@linkplain Throwable#addSuppressed(Throwable) suppressed} in the
	 * first {@code Throwable}.
	 *
	 * <p>If the first collected {@code Throwable} <em>aborted</em> execution
	 * and at least one later collected {@code Throwable} <em>failed</em>
	 * execution, the first <em>failing</em> {@code Throwable} will be returned
	 * with the previous <em>aborting</em> and any additional {@code Throwables}
	 * {@linkplain Throwable#addSuppressed(Throwable) suppressed} inside.
	 *
	 * @return the first collected {@code Throwable} or {@code null} if this
	 * {@code ThrowableCollector} is empty
	 * @see #isEmpty()
	 * @see #assertEmpty()
	 */
	public @Nullable Throwable getThrowable() {
		return this.throwable;
	}

	/**
	 * Determine if this {@code ThrowableCollector} is <em>empty</em> (i.e.,
	 * has not collected any {@code Throwables}).
	 */
	public boolean isEmpty() {
		return (this.throwable == null);
	}

	/**
	 * Determine if this {@code ThrowableCollector} is <em>not empty</em> (i.e.,
	 * has collected at least one {@code Throwable}).
	 */
	public boolean isNotEmpty() {
		return (this.throwable != null);
	}

	/**
	 * Assert that this {@code ThrowableCollector} is <em>empty</em> (i.e.,
	 * has not collected any {@code Throwables}).
	 *
	 * <p>If this collector is not empty, the first collected {@code Throwable}
	 * will be thrown with any additional {@code Throwables}
	 * {@linkplain Throwable#addSuppressed(Throwable) suppressed} in the
	 * first {@code Throwable}. Note, however, that the {@code Throwable}
	 * will not be wrapped. Rather, it will be
	 * {@linkplain ExceptionUtils#throwAsUncheckedException masked}
	 * as an unchecked exception.
	 *
	 * @see #getThrowable()
	 * @see ExceptionUtils#throwAsUncheckedException(Throwable)
	 */
	public void assertEmpty() {
		if (this.throwable != null) {
			throw ExceptionUtils.throwAsUncheckedException(this.throwable);
		}
	}

	/**
	 * Convert the collected {@link Throwable Throwables} into a {@link TestExecutionResult}.
	 *
	 * @return {@linkplain TestExecutionResult#aborted aborted} if the collected
	 * {@code Throwable} <em>aborted</em> execution;
	 * {@linkplain TestExecutionResult#failed failed} if it <em>failed</em>
	 * execution; and {@linkplain TestExecutionResult#successful successful}
	 * otherwise
	 * @since 1.6
	 */
	@API(status = MAINTAINED, since = "1.6")
	public TestExecutionResult toTestExecutionResult() {
		if (this.throwable == null) {
			return successful();
		}
		if (hasAbortedExecution(this.throwable)) {
			return aborted(this.throwable);
		}
		return failed(this.throwable);
	}

	private boolean hasAbortedExecution(Throwable t) {
		return this.abortedExecutionPredicate.test(t);
	}

	/**
	 * Functional interface for an executable block of code that may throw a
	 * {@link Throwable}.
	 */
	@FunctionalInterface
	public interface Executable {

		/**
		 * Execute this executable, potentially throwing a {@link Throwable}
		 * that signals abortion or failure.
		 */
		void execute() throws Throwable;

	}

	/**
	 * Factory for {@code ThrowableCollector} instances.
	 */
	public interface Factory {

		/**
		 * Create a new instance of a {@code ThrowableCollector}.
		 */
		ThrowableCollector create();

	}

}
