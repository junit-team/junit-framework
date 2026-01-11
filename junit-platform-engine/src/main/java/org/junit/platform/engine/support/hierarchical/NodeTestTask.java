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

	private @Nullable C parentContext;

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
			finalizer.run();
		}

		// Clear reference to context to allow it to be garbage collected.
		// See https://github.com/junit-team/junit-framework/issues/1578
		context = null;
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
				throwableCollector.execute(() -> {
					// @formatter:off
					List<NodeTestTask<C>> children = testDescriptor.getChildren().stream()
							.map(descriptor -> new NodeTestTask<C>(taskContext, descriptor))
							.collect(toCollection(ArrayList::new));
					// @formatter:on

					context = node.before(requiredContext());

					final DynamicTestExecutor dynamicTestExecutor = new DefaultDynamicTestExecutor();
					context = node.execute(requiredContext(), dynamicTestExecutor);

					if (!children.isEmpty()) {
						children.forEach(child -> child.setParentContext(context));
						taskContext.executorService().invokeAll(children);
					}

					throwableCollector.execute(dynamicTestExecutor::awaitFinished);
				});

				throwableCollector.execute(() -> node.after(requiredContext()));
			});
		});
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
