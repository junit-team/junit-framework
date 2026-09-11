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

import static java.util.Collections.emptySet;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;

/**
 * A <em>node</em> within the execution hierarchy.
 *
 * @param <C> the type of {@code EngineExecutionContext} used by the
 * {@code HierarchicalTestEngine}
 * @since 1.0
 * @see HierarchicalTestEngine
 */
@API(status = MAINTAINED, since = "1.0", consumers = "org.junit.platform.engine.support.hierarchical")
public interface Node<C extends EngineExecutionContext> {

	/**
	 * Prepare the supplied {@code context} prior to execution.
	 *
	 * <p>The default implementation returns the supplied {@code context} unmodified.
	 *
	 * @see #cleanUp(EngineExecutionContext)
	 */
	default C prepare(C context) throws Exception {
		return context;
	}

	/**
	 * Asynchronous variant of {@link #prepare(EngineExecutionContext)}.
	 *
	 * <p>The result is used purely as a promise that the preparation has
	 * finished; the {@code CompletionStage} payload is intentionally ignored.
	 *
	 * <p>The default implementation delegates to the (blocking)
	 * {@link #prepare(EngineExecutionContext)} method, bridging it into the
	 * reactive world.
	 *
	 * @param context the context to prepare
	 * @return a completion stage signaling termination of the preparation;
	 * never {@code null}
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default CompletionStage<?> prepareAsync(C context) {
		return AsyncTestExecution.synchronous(() -> prepare(context));
	}

	/**
	 * Clean up the supplied {@code context} after execution.
	 *
	 * <p>The default implementation does nothing.
	 *
	 * @param context the context to execute in
	 * @since 1.1
	 * @see #prepare(EngineExecutionContext)
	 */
	default void cleanUp(C context) throws Exception {
	}

	/**
	 * Asynchronous variant of {@link #cleanUp(EngineExecutionContext)}.
	 *
	 * <p>The result is used purely as a promise that the cleanup has finished;
	 * the {@code CompletionStage} payload is intentionally ignored.
	 *
	 * <p>The default implementation delegates to the (blocking)
	 * {@link #cleanUp(EngineExecutionContext)} method, bridging it into the
	 * reactive world.
	 *
	 * @param context the context to clean up
	 * @return a completion stage signaling termination of the cleanup; never
	 * {@code null}
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default CompletionStage<?> cleanUpAsync(C context) {
		return AsyncTestExecution.synchronous(() -> cleanUp(context));
	}

	/**
	 * Determine if the execution of the supplied {@code context} should be
	 * <em>skipped</em>.
	 *
	 * <p>The default implementation returns {@link SkipResult#doNotSkip()}.
	 */
	default SkipResult shouldBeSkipped(C context) throws Exception {
		return SkipResult.doNotSkip();
	}

	/**
	 * Asynchronous variant of {@link #shouldBeSkipped(EngineExecutionContext)}.
	 *
	 * <p>The result is used purely as a promise that the decision has been
	 * made; the {@code CompletionStage} payload is intentionally ignored (it is
	 * never a skip result). Use the (blocking) {@link #shouldBeSkipped} method
	 * to inspect the actual skip decision.
	 *
	 * <p>The default implementation delegates to the (blocking)
	 * {@link #shouldBeSkipped(EngineExecutionContext)} method, bridging it into
	 * the reactive world.
	 *
	 * @param context the context to inspect
	 * @return a completion stage signaling that a skip decision has been made;
	 * never {@code null}
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default CompletionStage<?> shouldBeSkippedAsync(C context) {
		return AsyncTestExecution.synchronous(() -> shouldBeSkipped(context));
	}

	/**
	 * Execute the <em>before</em> behavior of this node.
	 *
	 * <p>This method will be called once <em>before</em> {@linkplain #execute
	 * execution} of this node.
	 *
	 * <p>The default implementation returns the supplied {@code context} unmodified.
	 *
	 * @param context the context to execute in
	 * @return the new context to be used for children of this node; never
	 * {@code null}
	 * @see #execute(EngineExecutionContext, DynamicTestExecutor)
	 * @see #after(EngineExecutionContext)
	 */
	default C before(C context) throws Exception {
		return context;
	}

	/**
	 * Asynchronous variant of {@link #before(EngineExecutionContext)}.
	 *
	 * <p>The result is used purely as a promise that the before-behavior has
	 * finished; the {@code CompletionStage} payload is intentionally ignored.
	 * The produced context is carried by the (blocking)
	 * {@link #before(EngineExecutionContext)} method which the default
	 * implementation invokes.
	 *
	 * <p>The default implementation delegates to the (blocking)
	 * {@link #before(EngineExecutionContext)} method, bridging it into the
	 * reactive world.
	 *
	 * @param context the context to execute in
	 * @return a completion stage providing the context to use for children of
	 * this node; never {@code null}
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default CompletionStage<C> beforeAsync(C context) {
		return AsyncTestExecution.synchronousResult(() -> before(context));
	}

	/**
	 * Execute the <em>behavior</em> of this node.
	 *
	 * <p>Containers typically do not implement this method since the
	 * {@link HierarchicalTestEngine} handles execution of their children.
	 *
	 * <p>The supplied {@code dynamicTestExecutor} may be used to submit
	 * additional dynamic tests for immediate execution.
	 *
	 * <p>The default implementation returns the supplied {@code context} unmodified.
	 *
	 * @param context the context to execute in
	 * @param dynamicTestExecutor the executor to submit dynamic tests to
	 * @return the new context to be used for children of this node and for the
	 * <em>after</em> behavior of the parent of this node, if any
	 * @see #before
	 * @see #after
	 */
	default C execute(C context, DynamicTestExecutor dynamicTestExecutor) throws Exception {
		return context;
	}

	/**
	 * Asynchronous variant of
	 * {@link #execute(EngineExecutionContext, DynamicTestExecutor)}.
	 *
	 * <p>The produced context is carried by the (blocking)
	 * {@link #execute(EngineExecutionContext, DynamicTestExecutor)} method which
	 * the default implementation invokes.
	 *
	 * <p>The default implementation delegates to the (blocking)
	 * {@link #execute(EngineExecutionContext, DynamicTestExecutor)} method,
	 * bridging it into the reactive world.
	 *
	 * @param context the context to execute in
	 * @param dynamicTestExecutor a mechanism to register additional test tasks
	 * @return a completion stage providing the context to use for the node's
	 * after-behavior; never {@code null}
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default CompletionStage<C> executeAsync(C context, DynamicTestExecutor dynamicTestExecutor) {
		return AsyncTestExecution.synchronousResult(() -> execute(context, dynamicTestExecutor));
	}

	/**
	 * Execute the <em>after</em> behavior of this node.
	 *
	 * <p>This method will be called once <em>after</em> {@linkplain #execute
	 * execution} of this node.
	 *
	 * <p>The default implementation does nothing.
	 *
	 * @param context the context to execute in
	 * @see #before
	 * @see #execute
	 */
	default void after(C context) throws Exception {
	}

	/**
	 * Asynchronous variant of {@link #after(EngineExecutionContext)}.
	 *
	 * <p>The result is used purely as a promise that the after-behavior has
	 * finished; the {@code CompletionStage} payload is intentionally ignored.
	 *
	 * <p>The default implementation delegates to the (blocking)
	 * {@link #after(EngineExecutionContext)} method, bridging it into the
	 * reactive world.
	 *
	 * @param context the context to execute in
	 * @return a completion stage signaling termination of the after-behavior;
	 * never {@code null}
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default CompletionStage<?> afterAsync(C context) {
		return AsyncTestExecution.synchronous(() -> after(context));
	}

	/**
	 * Wraps around the invocation of {@link #before(EngineExecutionContext)},
	 * {@link #execute(EngineExecutionContext, DynamicTestExecutor)}, and
	 * {@link #after(EngineExecutionContext)}.
	 *
	 * @param context context the context to execute in
	 * @param invocation the wrapped invocation (must be invoked exactly once)
	 * @since 1.4
	 */
	@API(status = STABLE, since = "1.10")
	default void around(C context, Invocation<C> invocation) throws Exception {
		invocation.invoke(context);
	}

	/**
	 * Asynchronous variant of {@link #around(EngineExecutionContext, Invocation)}.
	 *
	 * <p>The result is used purely as a promise that the around-behavior has
	 * finished; the {@code CompletionStage} payload is intentionally ignored.
	 *
	 * <p>The default implementation delegates to the (blocking)
	 * {@link #around(EngineExecutionContext, Invocation)} method, bridging it
	 * into the reactive world.
	 *
	 * @param context the context to execute in
	 * @param invocation the wrapped invocation (must be invoked exactly once)
	 * @return a completion stage signaling termination of the around-behavior;
	 * never {@code null}
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default CompletionStage<?> aroundAsync(C context, Invocation<C> invocation) {
		return AsyncTestExecution.synchronous(() -> around(context, invocation));
	}

	/**
	 * Callback invoked when the execution of this node has been skipped.
	 *
	 * <p>The default implementation does nothing.
	 *
	 * @param context the execution context
	 * @param testDescriptor the test descriptor that was skipped
	 * @param result the result of skipped execution
	 * @since 1.4
	 */
	@API(status = STABLE, since = "1.10", consumers = "org.junit.platform.engine.support.hierarchical")
	default void nodeSkipped(C context, TestDescriptor testDescriptor, SkipResult result) {
	}

	/**
	 * Callback invoked when the execution of this node has finished.
	 *
	 * <p>The default implementation does nothing.
	 *
	 * @param context the execution context
	 * @param testDescriptor the test descriptor that was executed
	 * @param result the result of the execution
	 * @since 1.4
	 */
	@API(status = STABLE, since = "1.10", consumers = "org.junit.platform.engine.support.hierarchical")
	default void nodeFinished(C context, TestDescriptor testDescriptor, TestExecutionResult result) {
	}

	/**
	 * Get the set of {@linkplain ExclusiveResource exclusive resources}
	 * required to execute this node.
	 *
	 * <p>The default implementation returns an empty set.
	 *
	 * @return the set of exclusive resources required by this node; never
	 * {@code null} but potentially empty
	 * @since 1.3
	 * @see ExclusiveResource
	 */
	@API(status = STABLE, since = "1.10", consumers = "org.junit.platform.engine.support.hierarchical")
	default Set<ExclusiveResource> getExclusiveResources() {
		return emptySet();
	}

	/**
	 * {@return whether this node requires the global read-write lock}
	 *
	 * <p>Engines should return {@code true} for all nodes that have behavior,
	 * including tests but also containers with before/after lifecycle hooks.
	 *
	 * <p>The default implementation returns {@code true}. The value returned by
	 * engine-level nodes is ignored. Otherwise, if a container node returns
	 * {@code true}, the value returned by its descendants is ignored.
	 *
	 * @since 6.1
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	default boolean isGlobalReadLockRequired() {
		return true;
	}

	/**
	 * Get the preferred of {@linkplain ExecutionMode execution mode} for
	 * parallel execution of this node.
	 *
	 * <p>The default implementation returns {@link ExecutionMode#CONCURRENT}.
	 *
	 * @return the preferred execution mode of this node; never {@code null}
	 * @since 1.3
	 * @see ExecutionMode
	 */
	@API(status = STABLE, since = "1.10", consumers = "org.junit.platform.engine.support.hierarchical")
	default ExecutionMode getExecutionMode() {
		return ExecutionMode.CONCURRENT;
	}

	/**
	 * The result of determining whether the execution of a given {@code context}
	 * should be <em>skipped</em>.
	 *
	 * @see Node#shouldBeSkipped(EngineExecutionContext)
	 */
	class SkipResult {

		private static final SkipResult alwaysExecuteSkipResult = new SkipResult(false, null);

		private final boolean skipped;
		private final @Nullable String reason;

		/**
		 * Factory for creating <em>skipped</em> results.
		 *
		 * <p>A context that is skipped will be not be executed.
		 *
		 * @param reason the reason that the context should be skipped,
		 * may be {@code null}
		 * @return a skipped {@code SkipResult} with the given reason
		 */
		public static SkipResult skip(@Nullable String reason) {
			return new SkipResult(true, reason);
		}

		/**
		 * Factory for creating <em>do not skip</em> results.
		 *
		 * <p>A context that is not skipped will be executed as normal.
		 *
		 * @return a <em>do not skip</em> {@code SkipResult}
		 */
		public static SkipResult doNotSkip() {
			return alwaysExecuteSkipResult;
		}

		private SkipResult(boolean skipped, @Nullable String reason) {
			this.skipped = skipped;
			this.reason = reason;
		}

		/**
		 * Whether execution of the context should be skipped.
		 *
		 * @return {@code true} if the execution should be skipped
		 */
		public boolean isSkipped() {
			return this.skipped;
		}

		/**
		 * Get the reason that execution of the context should be skipped,
		 * if available.
		 */
		public Optional<String> getReason() {
			return Optional.ofNullable(this.reason);
		}

		@Override
		public String toString() {
			// @formatter:off
			return new ToStringBuilder(this)
					.append("skipped", this.skipped)
					.append("reason", this.reason != null ? this.reason : "<unknown>")
					.toString();
			// @formatter:on
		}

	}

	/**
	 * Executor for additional, dynamic test descriptors discovered during
	 * execution of a {@link Node}.
	 *
	 * <p>The test descriptors will be executed by the same
	 * {@link HierarchicalTestExecutor} that executes the submitting node.
	 *
	 * <p>This interface is not intended to be implemented by clients.
	 *
	 * @see Node#execute(EngineExecutionContext, DynamicTestExecutor)
	 * @see HierarchicalTestExecutor
	 */
	interface DynamicTestExecutor {

		/**
		 * Submit a dynamic test descriptor for immediate execution.
		 *
		 * @param testDescriptor the test descriptor to be executed; never
		 * {@code null}
		 */
		void execute(TestDescriptor testDescriptor);

		/**
		 * Submit a dynamic test descriptor for immediate execution with a
		 * custom, potentially no-op, execution listener.
		 *
		 * @param testDescriptor the test descriptor to be executed; never
		 * {@code null}
		 * @param executionListener the executionListener to be notified; never
		 * {@code null}
		 * @return a future to cancel or wait for the execution
		 * @since 1.7
		 * @see EngineExecutionListener#NOOP
		 */
		@API(status = STABLE, since = "1.10")
		Future<?> execute(TestDescriptor testDescriptor, EngineExecutionListener executionListener);

		/**
		 * Block until all dynamic test descriptors submitted to this executor
		 * are finished.
		 *
		 * <p>This method is useful if the node needs to perform actions in its
		 * {@link #execute(EngineExecutionContext, DynamicTestExecutor)} method
		 * after all its dynamic children have finished.
		 *
		 * @throws InterruptedException if interrupted while waiting
		 */
		void awaitFinished() throws InterruptedException;

		/**
		 * Asynchronous variant of {@link #awaitFinished()}.
		 *
		 * <p>The returned {@link CompletionStage} is used purely as a promise
		 * that all submitted dynamic tests have finished; its payload is
		 * intentionally ignored.
		 *
		 * <p>The default implementation bridges the (blocking)
		 * {@link #awaitFinished()} method into the reactive world.
		 *
		 * @return a completion stage signaling that all dynamic tests have
		 * finished; never {@code null}
		 * @since 6.2
		 */
		@API(status = EXPERIMENTAL, since = "6.2")
		default CompletionStage<?> awaitFinishedAsync() {
			return AsyncTestExecution.synchronous(() -> {
				try {
					awaitFinished();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new JUnitException("Interrupted while waiting for dynamic tests to finish", e);
				}
			});
		}
	}

	/**
	 * Supported execution modes for parallel execution.
	 *
	 * @since 1.3
	 * @see #SAME_THREAD
	 * @see #CONCURRENT
	 * @see Node#getExecutionMode()
	 */
	@API(status = STABLE, since = "1.10", consumers = "org.junit.platform.engine.support.hierarchical")
	enum ExecutionMode {

		/**
		 * Force execution in same thread as the parent node.
		 *
		 * @see #CONCURRENT
		 */
		SAME_THREAD,

		/**
		 * Allow concurrent execution with any other node.
		 *
		 * @see #SAME_THREAD
		 */
		CONCURRENT
	}

	/**
	 * Represents an invocation that runs with the supplied context.
	 *
	 * @param <C> the type of {@code EngineExecutionContext} used by the {@code HierarchicalTestEngine}
	 * @since 1.4
	 */
	@API(status = STABLE, since = "1.10")
	interface Invocation<C extends EngineExecutionContext> {

		/**
		 * Invoke this invocation with the supplied context.
		 *
		 * @param context the context to invoke in
		 */
		void invoke(C context) throws Exception;
	}
}
