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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
			if (!taskContext.cancellationToken().isCancellationRequested()) {
				prepare();
			}
			if (throwableCollector.isEmpty()) {
				throwableCollector.execute(() -> skipResult = checkWhetherSkipped());
			}
			if (throwableCollector.isEmpty() && !requiredSkipResult().isSkipped()) {
				executeRecursively();
			}
			if (context != null) {
				cleanUp();
			}
			reportCompletion();
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
			try {
				finalizer.run();
			}
			finally {
				clearReferences();
			}
		}
	}

	/**
	 * Execute the node without blocking on asynchronous lifecycle phases.
	 * Preparation, skip checks, and cleanup retain their synchronous contracts.
	 */
	@Override
	public CompletionStage<Outcome<C>> executeAsync() {
		CompletionStage<Outcome<C>> flow;
		try {
			throwableCollector = taskContext.throwableCollectorFactory().create();
			if (!taskContext.cancellationToken().isCancellationRequested()) {
				prepare();
			}
			if (requiredThrowableCollector().isEmpty()) {
				requiredThrowableCollector().execute(() -> skipResult = checkWhetherSkipped());
			}
			if (requiredThrowableCollector().isEmpty() && !requiredSkipResult().isSkipped()) {
				taskContext.listener().executionStarted(testDescriptor);
				started = true;
				flow = executeRecursivelyAsync();
			}
			else {
				flow = completedFuture(Outcome.continued(context));
			}
		}
		catch (Throwable throwable) {
			flow = CompletableFuture.failedFuture(throwable);
		}
		return flow.thenCompose(outcome -> {
			if (outcome.unrecoverable() != null) {
				return CompletableFuture.<Outcome<C>>failedFuture(outcome.unrecoverable());
			}
			if (context != null) {
				cleanUp();
			}
			reportCompletion();
			return completedFuture(outcome);
		}).whenComplete((__, throwable) -> {
			try {
				finalizer.run();
			}
			finally {
				clearReferences();
			}
		});
	}

	private void clearReferences() {
		context = null;
		parentContext = null;
		throwableCollector = null;
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

	private void executeRecursively() {
		taskContext.listener().executionStarted(testDescriptor);
		started = true;

		var throwableCollector = requiredThrowableCollector();

		throwableCollector.execute(() -> {
			node.around(requiredContext(), ctx -> {
				context = ctx;
				final DynamicTestExecutor dynamicTestExecutor = new DefaultDynamicTestExecutor();
				throwableCollector.execute(() -> {
					// @formatter:off
					List<NodeTestTask<C>> children = testDescriptor.getChildren().stream()
							.map(descriptor -> new NodeTestTask<C>(taskContext, descriptor))
							.collect(toCollection(ArrayList::new));
					// @formatter:on

					context = node.before(requiredContext());

					context = node.execute(requiredContext(), dynamicTestExecutor);

					if (!children.isEmpty()) {
						children.forEach(child -> child.setParentContext(context));
						taskContext.executorService().invokeAll(children);
					}
				});

				// Await submitted dynamic tests even if execute() threw an exception
				throwableCollector.execute(dynamicTestExecutor::awaitFinished);
				throwableCollector.execute(() -> node.after(requiredContext()));
			});
		});
	}

	private CompletionStage<Outcome<C>> executeRecursivelyAsync() {
		return phase(() -> node.aroundAsync(requiredContext(), ctx -> {
			context = ctx;
			return runRecursivelyAsync().thenCompose(outcome -> {
				if (outcome.unrecoverable() != null) {
					return CompletableFuture.failedFuture(outcome.unrecoverable());
				}
				return completedFuture(null);
			});
		}), __ -> {
		});
	}

	private CompletionStage<Outcome<C>> runRecursivelyAsync() {
		final var dynamicTestExecutor = new DefaultDynamicTestExecutor();
		// Snapshot static children before before/execute can register dynamic tests.
		final List<NodeTestTask<C>> children = testDescriptor.getChildren().stream()
				.map(descriptor -> new NodeTestTask<C>(taskContext, descriptor))
				.collect(toCollection(ArrayList::new));
		return phase(() -> node.beforeAsync(requiredContext()), newContext -> context = newContext)
				.thenCompose(outcome -> {
					if (outcome.unrecoverable() != null || outcome.failureOccurred()) {
						return completedFuture(outcome);
					}
					return phase(() -> node.executeAsync(requiredContext(), dynamicTestExecutor),
						newContext -> context = newContext);
				})
				.thenCompose(outcome -> {
					if (outcome.unrecoverable() != null || outcome.failureOccurred() || children.isEmpty()) {
						return completedFuture(outcome);
					}
					children.forEach(child -> child.setParentContext(context));
					return phase(() -> taskContext.executorService().invokeAllAsync(children), __ -> {
					});
				})
				.thenCompose(outcome -> {
					if (outcome.unrecoverable() != null) {
						return completedFuture(outcome);
					}
					// Drain dynamic tests even if before, execute, or static children failed.
					return phase(dynamicTestExecutor::awaitFinishedAsync, __ -> {
					});
				})
				.thenCompose(outcome -> {
					if (outcome.unrecoverable() != null) {
						return completedFuture(outcome);
					}
					return phase(() -> node.afterAsync(requiredContext()), __ -> {
					});
				});
	}

	private <T> CompletionStage<Outcome<C>> phase(final Supplier<CompletionStage<T>> action, final Consumer<T> result) {
		try {
			return action.get().handle((value, throwable) -> {
				if (throwable != null) {
					return toOutcome(throwable);
				}
				try {
					result.accept(value);
					return Outcome.continued(context);
				}
				catch (Throwable failure) {
					return toOutcome(failure);
				}
			});
		}
		catch (Throwable throwable) {
			return completedFuture(toOutcome(throwable));
		}
	}

	private Outcome<C> toOutcome(Throwable throwable) {
		final var root = unwrap(throwable);
		if (UnrecoverableExceptions.isUnrecoverable(root)) {
			return Outcome.errored(root);
		}
		requiredThrowableCollector().execute(() -> ExceptionUtils.throwAsUncheckedException(root));
		return Outcome.failedRecoverably();
	}

	private static Throwable unwrap(Throwable throwable) {
		var current = throwable;
		while ((current instanceof CompletionException || current instanceof ExecutionException)
				&& current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

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
		private final Map<UniqueId, CompletableFuture<?>> pendingStages = new ConcurrentHashMap<>();
		private final AtomicReference<@Nullable Throwable> executionFailure = new AtomicReference<>();

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
				if (future instanceof CompletionStage<?> stage) {
					// The task finalizer runs before its returned stage settles. Retain a
					// separate completion signal until failures have been observed as well.
					final var observed = new CompletableFuture<Void>();
					pendingStages.put(uniqueId, observed);
					stage.whenComplete((__, throwable) -> {
						if (throwable == null || unwrap(throwable) instanceof CancellationException) {
							observed.complete(null);
						}
						else {
							final var failure = unwrap(throwable);
							if (UnrecoverableExceptions.isUnrecoverable(failure)) {
								executionFailure.set(failure);
							}
							else {
								executionFailure.compareAndSet(null, failure);
							}
							observed.completeExceptionally(failure);
						}
						pendingStages.remove(uniqueId);
					});
				}
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

		@Override
		public CompletionStage<?> awaitFinishedAsync() {
			final var stages = new ArrayList<CompletableFuture<?>>();
			stages.addAll(pendingStages.values());
			unfinishedTasks.values().stream()
					.map(DynamicTaskState::awaitFinishedAsync)
					.map(stage -> stage.handle((__, throwable) -> {
						if (throwable != null && !(unwrap(throwable) instanceof CancellationException)) {
							throw ExceptionUtils.throwAsUncheckedException(unwrap(throwable));
						}
						return null;
					}).toCompletableFuture())
					.forEach(stages::add);
			return CompletableFuture.allOf(stages.toArray(CompletableFuture<?>[]::new)).handle((__, throwable) -> {
				final var failure = executionFailure.get();
				if (failure != null) {
					throw ExceptionUtils.throwAsUncheckedException(failure);
				}
				if (throwable != null) {
					throw ExceptionUtils.throwAsUncheckedException(unwrap(throwable));
				}
				return null;
			});
		}
	}

	private record DynamicTaskState(@Nullable Future<@Nullable Void> future) {

		static DynamicTaskState unscheduled() {
			return new DynamicTaskState(null);
		}

		static DynamicTaskState scheduled(Future<@Nullable Void> future) {
			return new DynamicTaskState(future);
		}

		void awaitFinished() throws ExecutionException, InterruptedException {
			if (future != null) {
				future.get();
			}
		}

		CompletionStage<?> awaitFinishedAsync() {
			if (future instanceof CompletionStage<?> stage) {
				return stage;
			}
			// Only legacy services returning non-stage Future wrappers need a blocking fallback.
			return AsyncTestExecution.synchronous(this::awaitFinished);
		}
	}

}
