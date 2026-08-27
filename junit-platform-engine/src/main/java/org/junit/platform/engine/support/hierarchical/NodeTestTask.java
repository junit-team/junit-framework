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

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toCollection;
import static org.junit.platform.engine.TestExecutionResult.failed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService.TestTask;
import org.junit.platform.engine.support.hierarchical.Node.DynamicTestExecutor;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;
import org.junit.platform.engine.support.hierarchical.Node.SkipResult;

/**
 * @since 1.3
 */
class NodeTestTask<C extends EngineExecutionContext> implements TestTask {

	private static final Logger logger = LoggerFactory.getLogger(NodeTestTask.class);
	private static final Runnable NOOP = () -> {
	};

	static final SkipResult CANCELLED_SKIP_RESULT = SkipResult.skip("Execution cancelled");

	private final NodeTestTaskContext taskContext;
	private final TestDescriptor testDescriptor;
	private final Node<C> node;
	private final Runnable finalizer;

	private volatile @Nullable C parentContext;

	private @Nullable C context;

	private @Nullable SkipResult skipResult;

	private boolean started;

	private @Nullable ThrowableCollector throwableCollector;

	NodeTestTask(NodeTestTaskContext taskContext, TestDescriptor testDescriptor) {
		this(taskContext, testDescriptor, NOOP);
	}

	NodeTestTask(NodeTestTaskContext taskContext, TestDescriptor testDescriptor, Runnable finalizer) {
		this.taskContext = taskContext;
		this.testDescriptor = testDescriptor;
		this.node = NodeUtils.asNode(testDescriptor);
		this.finalizer = finalizer;
	}

	@Override
	public ResourceLock getResourceLock() {
		return taskContext.executionAdvisor().getResourceLock(testDescriptor);
	}

	@Override
	public ExecutionMode getExecutionMode() {
		return taskContext.executionAdvisor().getForcedExecutionMode(testDescriptor) //
				.orElseGet(node::getExecutionMode);
	}

	@Override
	public TestDescriptor getTestDescriptor() {
		return testDescriptor;
	}

	@Override
	public String toString() {
		return "NodeTestTask [" + testDescriptor + "]";
	}

	void setParentContext(@Nullable C parentContext) {
		this.parentContext = parentContext;
	}

	@Override
	public void execute() {
		try {
			throwableCollector = taskContext.throwableCollectorFactory().create();
			Outcome<C> outcome = awaitRecursively(executeAsync());
			UnrecoverableExceptions.rethrowIfUnrecoverable(outcome.unrecoverable());
		}
		finally {
			// Ensure that the 'interrupted status' flag for the current thread
			// is cleared for reuse of the thread in subsequent task executions.
			// See https://github.com/junit-team/junit-framework/issues/1688
			if (Thread.interrupted()) {
				logger.debug(() -> """
						Execution of TestDescriptor with display name [%s] \
						and unique ID [%s] failed to clear the 'interrupted status' flag for the \
						current thread. JUnit has cleared the flag, but you may wish to investigate \
						why the flag was not cleared by user code.""".formatted(this.testDescriptor.getDisplayName(),
					this.testDescriptor.getUniqueId()));
			}
			finalizer.run();
		}

		// Clear reference to context to allow it to be garbage collected.
		// See https://github.com/junit-team/junit-framework/issues/1578
		context = null;
	}

	/**
	 * Execute this task reactively.
	 *
	 * <p>This method drives the node's {@linkplain Node#prepareAsync prepare},
	 * skip check, and {@link #executeRecursivelyAsync() recursive execution}
	 * behavior via {@link CompletionStage} composition so that a suspending node
	 * does not park a platform thread.
	 *
	 * <p>Note: it would be preferable to drive the whole hierarchy from the top
	 * down without any intermediate blocking (option "A"), but that is
	 * intentionally deferred here to limit the impact of this first round.
	 *
	 * @return a completion stage carrying this node's {@link Outcome}; never
	 * {@code null}
	 * @since 6.2
	 */
	@Override
	public CompletionStage<Outcome<C>> executeAsync() {
		if (throwableCollector == null) {
			throwableCollector = taskContext.throwableCollectorFactory().create();
		}
		if (!taskContext.cancellationToken().isCancellationRequested()) {
			prepare();
		}
		if (requiredThrowableCollector().isEmpty()) {
			requiredThrowableCollector().execute(() -> skipResult = checkWhetherSkipped());
		}
		CompletionStage<Outcome<C>> flow;
		if (requiredThrowableCollector().isEmpty() && !requiredSkipResult().isSkipped()) {
			taskContext.listener().executionStarted(testDescriptor);
			started = true;
			flow = executeRecursivelyAsync();
		}
		else {
			flow = completedFuture(Outcome.continued(context));
		}
		return flow.thenApply(outcome -> {
			if (context != null && outcome.unrecoverable() == null) {
				cleanUp();
			}
			if (outcome.unrecoverable() == null) {
				reportCompletion();
			}
			return outcome;
		});
	}

	private void prepare() {
		requiredThrowableCollector().execute(() -> context = node.prepare(requireNonNull(parentContext)));

		// Clear reference to parent context to allow it to be garbage collected.
		// See https://github.com/junit-team/junit-framework/issues/1578
		parentContext = null;
	}

	private SkipResult checkWhetherSkipped() throws Exception {
		return taskContext.cancellationToken().isCancellationRequested() //
				? CANCELLED_SKIP_RESULT //
				: node.shouldBeSkipped(requiredContext());
	}

	/**
	 * Execute the recursive part of this task reactively, producing an
	 * {@link Outcome} that bubbles unrecoverable errors to the caller.
	 *
	 * @return a completion stage carrying this subtree's {@link Outcome}; never
	 * {@code null}
	 */
	private CompletionStage<Outcome<C>> executeRecursivelyAsync() {
		return runRecursivelyAsync();
	}

	/**
	 * Run the before/execute/children/dynamic-test/{@code after} phases
	 * reactively, producing an {@link Outcome} that bubbles unrecoverable
	 * errors to the caller. Recoverable failures are aggregated in the shared
	 * {@link ThrowableCollector}, honoring the collect-all semantics of the
	 * blocking implementation.
	 *
	 * @return a completion stage carrying this subtree's {@link Outcome}; never
	 * {@code null}
	 */
	private CompletionStage<Outcome<C>> runRecursivelyAsync() {
		return node.beforeAsync(requiredContext()) //
				.<Outcome<C>> handle((newContext, throwable) -> {
					if (throwable != null) {
						return toOutcome(throwable);
					}
					context = newContext;
					return Outcome.continued(newContext);
				}) //
				.thenCompose(outcome -> {
					if (outcome.unrecoverable() != null) {
						return completedFuture(outcome);
					}
					if (outcome.failureOccurred()) {
						return runAfterAsync(outcome);
					}
					return executeBodyThenChildrenThenAwaitAsync() //
							.thenCompose(bodyOutcome -> {
								if (bodyOutcome.unrecoverable() != null) {
									return completedFuture(bodyOutcome);
								}
								return runAfterAsync(bodyOutcome);
							});
				});
	}

	/**
	 * Execute the node's body, run its children, then await the dynamic-test
	 * executor, honoring the blocking implementation's control flow and
	 * aggregating recoverable failures into the collector.
	 */
	private CompletionStage<Outcome<C>> executeBodyThenChildrenThenAwaitAsync() {
		final DefaultDynamicTestExecutor dynamicTestExecutor = new DefaultDynamicTestExecutor();

		return node.executeAsync(requiredContext(), dynamicTestExecutor) //
				.<Outcome<C>> handle((newContext, throwable) -> {
					if (throwable != null) {
						return toOutcome(throwable);
					}
					context = newContext;
					return Outcome.continued(newContext);
				}) //
				.thenCompose(outcome -> {
					if (outcome.unrecoverable() != null || outcome.failureOccurred()) {
						return completedFuture(outcome);
					}
					List<NodeTestTask<C>> children = testDescriptor.getChildren().stream() //
							.map(descriptor -> new NodeTestTask<C>(taskContext, descriptor)) //
							.collect(toCollection(ArrayList::new));
					if (children.isEmpty()) {
						return completedFuture(outcome).thenCompose(o -> awaitDynamicFinished(dynamicTestExecutor, o));
					}
					children.forEach(child -> child.setParentContext(outcome.context()));
					return taskContext.executorService().invokeAllAsync(children) //
							.<Outcome<C>> handle((___, throwable) -> {
								if (throwable != null) {
									return toOutcome(throwable);
								}
								return outcome;
							}) //
							.thenCompose(o -> {
								if (o.unrecoverable() != null || o.failureOccurred()) {
									return completedFuture(o);
								}
								return awaitDynamicFinished(dynamicTestExecutor, outcome);
							});
				});
	}

	private CompletionStage<Outcome<C>> awaitDynamicFinished(DefaultDynamicTestExecutor dynamicTestExecutor,
			Outcome<C> outcome) {
		return dynamicTestExecutor.awaitFinishedAsync() //
				.<Outcome<C>> handle((___, throwable) -> {
					if (throwable != null) {
						return toOutcome(throwable);
					}
					return outcome;
				});
	}

	private CompletionStage<Outcome<C>> runAfterAsync(Outcome<C> outcome) {
		return node.afterAsync(requiredContext()) //
				.<Outcome<C>> handle((___, throwable) -> {
					if (throwable != null) {
						return toOutcome(throwable);
					}
					return outcome;
				});
	}

	/**
	 * Convert a throwable obtained from an asynchronous phase into an
	 * {@link Outcome}: recoverable throwables are aggregated in the collector
	 * and reported as a failure that does not abort the subtree, while
	 * unrecoverable throwables (e.g. {@link OutOfMemoryError}) are bubbled to
	 * the caller.
	 */
	private Outcome<C> toOutcome(Throwable throwable) {
		Throwable root = unwrap(throwable);
		if (UnrecoverableExceptions.isUnrecoverable(root)) {
			return Outcome.errored(root);
		}
		requiredThrowableCollector().execute(() -> ExceptionUtils.throwAsUncheckedException(root));
		return Outcome.failedRecoverably();
	}

	/**
	 * Unwrap {@link CompletionException}s and {@link ExecutionException}s to
	 * their root cause.
	 */
	private static Throwable unwrap(Throwable throwable) {
		Throwable current = throwable;
		while ((current instanceof CompletionException || current instanceof ExecutionException)
				&& current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

	/**
	 * A value that carries the outcome of an asynchronous phase. Recoverable
	 * failures are aggregated in the {@link ThrowableCollector} and only signal
	 * {@link #failureOccurred()}; unrecoverable errors (e.g.
	 * {@link OutOfMemoryError}) are carried in {@link #unrecoverable()} so they
	 * can be rethrown by the root caller.
	 *
	 * @param context the context produced by the phase, if the phase continued
	 * @param unrecoverable the unrecoverable error to bubble, if any
	 * @param failureOccurred whether a recoverable failure was aggregated
	 * @param <C> the context type
	 */
	record Outcome<C>(@Nullable C context, @Nullable Throwable unrecoverable, boolean failureOccurred) {

		static <C> Outcome<C> continued(@Nullable C context) {
			return new Outcome<>(context, null, false);
		}

		static <C> Outcome<C> failedRecoverably() {
			return new Outcome<>(null, null, true);
		}

		static <C> Outcome<C> errored(Throwable unrecoverable) {
			return new Outcome<>(null, unrecoverable, false);
		}
	}

	/**
	 * Block the calling thread until the supplied stage has completed, returning
	 * its {@link Outcome}. A stage that fails with an unrecoverable cause is
	 * rethrown synchronously as the same instance.
	 *
	 * @param stage the stage to await
	 * @return the completed stage's {@link Outcome}
	 */
	private Outcome<C> awaitRecursively(CompletionStage<Outcome<C>> stage) {
		try {
			return stage.toCompletableFuture().join();
		}
		catch (CompletionException ex) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(ex.getCause());
			throw ex;
		}
	}

	private void cleanUp() {
		requiredThrowableCollector().execute(() -> node.cleanUp(requiredContext()));
	}

	private void reportCompletion() {

		var throwableCollector = requiredThrowableCollector();

		if (throwableCollector.isEmpty() && requiredSkipResult().isSkipped()) {
			var skipResult = requiredSkipResult();
			try {
				node.nodeSkipped(requireNonNullElse(context, parentContext), testDescriptor, skipResult);
			}
			catch (Throwable throwable) {
				UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
				logger.debug(throwable,
					() -> "Failed to invoke nodeSkipped() on Node %s".formatted(testDescriptor.getUniqueId()));
			}
			taskContext.listener().executionSkipped(testDescriptor, skipResult.getReason().orElse("<unknown>"));
			return;
		}
		if (!started) {
			// Call executionStarted first to comply with the contract of EngineExecutionListener.
			taskContext.listener().executionStarted(testDescriptor);
		}
		try {
			node.nodeFinished(requiredContext(), testDescriptor, throwableCollector.toTestExecutionResult());
		}
		catch (Throwable throwable) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
			logger.debug(throwable,
				() -> "Failed to invoke nodeFinished() on Node %s".formatted(testDescriptor.getUniqueId()));
		}
		taskContext.listener().executionFinished(testDescriptor, throwableCollector.toTestExecutionResult());
		this.throwableCollector = null;
	}

	private C requiredContext() {
		return requireNonNull(context);
	}

	private SkipResult requiredSkipResult() {
		return requireNonNull(skipResult);
	}

	private ThrowableCollector requiredThrowableCollector() {
		return requireNonNull(throwableCollector);
	}

	private class DefaultDynamicTestExecutor implements DynamicTestExecutor {
		private final Map<UniqueId, DynamicTaskState> unfinishedTasks = new ConcurrentHashMap<>();

		@Override
		@SuppressWarnings("FutureReturnValueIgnored")
		public void execute(TestDescriptor testDescriptor) {
			execute(testDescriptor, taskContext.listener());
		}

		@Override
		public Future<?> execute(TestDescriptor testDescriptor, EngineExecutionListener executionListener) {
			Preconditions.notNull(testDescriptor, "testDescriptor must not be null");
			Preconditions.notNull(executionListener, "executionListener must not be null");

			executionListener.dynamicTestRegistered(testDescriptor);
			Set<ExclusiveResource> exclusiveResources = NodeUtils.asNode(testDescriptor).getExclusiveResources();
			if (!exclusiveResources.isEmpty()) {
				executionListener.executionStarted(testDescriptor);
				String message = "Dynamic test descriptors must not declare exclusive resources: " + exclusiveResources;
				executionListener.executionFinished(testDescriptor, failed(new JUnitException(message)));
				return completedFuture(null);
			}
			else {
				UniqueId uniqueId = testDescriptor.getUniqueId();
				NodeTestTask<C> nodeTestTask = new NodeTestTask<>(taskContext.withListener(executionListener),
					testDescriptor, () -> unfinishedTasks.remove(uniqueId));
				nodeTestTask.setParentContext(context);
				unfinishedTasks.put(uniqueId, DynamicTaskState.unscheduled());
				var future = taskContext.executorService().submit(nodeTestTask);
				unfinishedTasks.computeIfPresent(uniqueId, (__, state) -> DynamicTaskState.scheduled(future));
				return future;
			}
		}

		@Override
		public void awaitFinished() throws InterruptedException {
			for (DynamicTaskState state : unfinishedTasks.values()) {
				try {
					state.awaitFinished();
				}
				catch (CancellationException ignore) {
					// Futures returned by execute() may have been cancelled
				}
				catch (ExecutionException e) {
					throw ExceptionUtils.throwAsUncheckedException(requireNonNullElse(e.getCause(), e));
				}
			}
		}

		/**
		 * Asynchronous variant of {@link #awaitFinished()}, used by the
		 * reactive driver. Dynamic-test execution itself is still scheduled on
		 * the executor thread pool, so this currently bridges the blocking
		 * wait; it does not block when no dynamic tests were registered.
		 *
		 * @return a completion stage signaling that all registered dynamic test
		 * tasks have finished; never {@code null}
		 */
		@Override
		public CompletionStage<?> awaitFinishedAsync() {
			return AsyncTestExecution.synchronous(this::awaitFinished);
		}
	}

	@FunctionalInterface
	private interface DynamicTaskState {

		DynamicTaskState UNSCHEDULED = () -> {
		};

		static DynamicTaskState unscheduled() {
			return UNSCHEDULED;
		}

		static DynamicTaskState scheduled(Future<@Nullable Void> future) {
			return future::get;
		}

		void awaitFinished() throws CancellationException, ExecutionException, InterruptedException;
	}

}
