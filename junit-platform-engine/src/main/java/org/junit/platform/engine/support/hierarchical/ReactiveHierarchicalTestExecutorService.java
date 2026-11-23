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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.jspecify.annotations.Nullable;

/**
 * A {@link HierarchicalTestExecutorService} that schedules tasks reactively.
 *
 * <p>Concurrency is capped by an {@link AsyncResourcePermit} and resource locks
 * are acquired through a {@link ReactiveResourceGate}; both hand off waiters via
 * completed {@linkplain CompletionStage stages} instead of parking threads.
 * Task bodies run on a bounded worker pool.
 *
 * <p>This is an opt-in, additive service. The default engine path keeps using
 * the existing blocking services.
 *
 * @since 6.2
 */
@API(status = EXPERIMENTAL, since = "6.2")
final class ReactiveHierarchicalTestExecutorService implements HierarchicalTestExecutorService {

	private final @Nullable AsyncResourcePermit permit;
	private final ReactiveResourceGate resourceGate = new ReactiveResourceGate();
	private final ExecutorService workerPool;

	ReactiveHierarchicalTestExecutorService(int parallelism) {
		this(parallelism, Executors.newFixedThreadPool(parallelism, runnable -> {
			var thread = new Thread(runnable, "junit-reactive-worker");
			thread.setDaemon(true);
			return thread;
		}));
	}

	/**
	 * Create a reactive executor for the standalone cooperative lane (no thread
	 * configuration). Concurrency is derived from the async test methods' own
	 * returned contexts; a small trigger pool starts the async bodies.
	 */
	ReactiveHierarchicalTestExecutorService() {
		this(null, defaultTriggerPool());
	}

	ReactiveHierarchicalTestExecutorService(@Nullable Integer parallelism, ExecutorService workerPool) {
		this.permit = parallelism != null ? new AsyncResourcePermit(parallelism) : null;
		this.workerPool = workerPool;
	}

	private static ExecutorService defaultTriggerPool() {
		int size = min(4, max(1, Runtime.getRuntime().availableProcessors()));
		return Executors.newFixedThreadPool(size, runnable -> {
			var thread = new Thread(runnable, "junit-reactive-trigger");
			thread.setDaemon(true);
			return thread;
		});
	}

	@Override
	public Future<@Nullable Void> submit(TestTask testTask) {
		return executeTask(testTask).toCompletableFuture();
	}

	@Override
	public CompletionStage<?> submitAsync(TestTask testTask) {
		return executeTask(testTask);
	}

	@Override
	public void invokeAll(List<? extends TestTask> testTasks) {
		invokeAllAsync(testTasks).toCompletableFuture().join();
	}

	@Override
	public CompletionStage<?> invokeAllAsync(List<? extends TestTask> testTasks) {
		if (testTasks.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		// CONCURRENT children (including implicitly-concurrent async test methods)
		// may overlap; SAME_THREAD children run sequentially in discovery order.
		CompletionStage<Void> sequential = CompletableFuture.completedFuture(null);
		List<CompletableFuture<?>> concurrent = new ArrayList<>();
		for (TestTask testTask : testTasks) {
			if (testTask.getExecutionMode() == Node.ExecutionMode.SAME_THREAD) {
				sequential = sequential.thenCompose(__ -> executeTask(testTask).toCompletableFuture());
			}
			else {
				concurrent.add(executeTask(testTask).toCompletableFuture());
			}
		}
		concurrent.add(sequential.toCompletableFuture());
		return awaitAll(concurrent);
	}

	private static CompletionStage<?> awaitAll(final List<CompletableFuture<?>> futures) {
		final var fatal = new AtomicReference<@Nullable Throwable>();
		final var observed = futures.stream().map(future -> future.whenComplete((__, throwable) -> {
			if (throwable != null) {
				var cause = throwable;
				while ((cause instanceof CompletionException || cause instanceof ExecutionException)
						&& cause.getCause() != null) {
					cause = cause.getCause();
				}
				if (UnrecoverableExceptions.isUnrecoverable(cause)) {
					fatal.compareAndSet(null, cause);
				}
			}
		})).toArray(CompletableFuture<?>[]::new);
		return CompletableFuture.allOf(observed).handle((__, throwable) -> {
			final var failure = fatal.get();
			if (failure != null) {
				throw ExceptionUtils.throwAsUncheckedException(failure);
			}
			if (throwable != null) {
				throw ExceptionUtils.throwAsUncheckedException(throwable);
			}
			return null;
		});
	}

	/**
	 * Acquire a concurrency permit and the task's resource lock, execute the
	 * task, then release the lock and the permit. Every step is reactive; no
	 * thread is parked.
	 *
	 * <p>Container nodes do not consume a concurrency permit: they coordinate
	 * their descendant nodes, and each descendant leaf is what actually holds a
	 * permit. This avoids an effective deadlock where a container occupying the
	 * only permit waits for a child that needs the same permit.
	 *
	 * <p>In the standalone cooperative lane ({@code permit == null}) no permit is
	 * acquired or released at all: concurrency comes from the async methods'
	 * returned contexts.
	 */
	private CompletionStage<@Nullable Void> executeTask(TestTask testTask) {
		if (isContainer(testTask) || this.permit == null) {
			return acquireLockAndRun(testTask, null);
		}
		// @formatter:off
		return this.permit.acquire()
				.thenCompose(permitToken -> acquireLockAndRun(testTask, permitToken));
		// @formatter:on
	}

	/**
	 * Determine whether the supplied task is a container node that coordinates
	 * its descendants (and therefore must not hold a concurrency permit while
	 * awaiting them). Generic tasks that are not {@link NodeTestTask}s are
	 * treated as leaves and always hold a permit.
	 */
	private static boolean isContainer(TestTask testTask) {
		if (testTask instanceof NodeTestTask<?> nodeTestTask) {
			return !nodeTestTask.getTestDescriptor().isTest();
		}
		return false;
	}

	private CompletionStage<@Nullable Void> acquireLockAndRun(final TestTask testTask,
			final AsyncResourcePermit.@Nullable Permit permit) {
		final CompletionStage<ResourceLock> acquisition;
		try {
			acquisition = resourceGate.acquire(testTask.getResourceLock());
		}
		catch (Throwable throwable) {
			if (permit != null) {
				permit.release();
			}
			return CompletableFuture.failedFuture(throwable);
		}
		return acquisition.whenComplete((__, throwable) -> {
			if (throwable != null && permit != null) {
				permit.release();
			}
		}).thenCompose(lock -> runOnWorker(testTask, lock, permit));
	}

	/**
	 * Run only the synchronous preamble on a worker and release resources before
	 * publishing completion, including fatal failures and rejected submissions.
	 */
	@SuppressWarnings("FutureReturnValueIgnored")
	private CompletionStage<@Nullable Void> runOnWorker(final TestTask testTask, final ResourceLock lock,
			final AsyncResourcePermit.@Nullable Permit permit) {
		final CompletableFuture<@Nullable Void> running = new CompletableFuture<>();
		try {
			workerPool.execute(() -> {
				try {
					testTask.executeAsync().whenComplete((__, throwable) -> finish(running, lock, permit, throwable));
				}
				catch (Throwable throwable) {
					finish(running, lock, permit, throwable);
				}
				finally {
					// Clear only this worker's interrupt status, not an external completion thread's.
					Thread.interrupted();
				}
			});
		}
		catch (Throwable throwable) {
			finish(running, lock, permit, throwable);
		}
		return running;
	}

	@SuppressWarnings("ReferenceEquality") // Suppressing the same Throwable instance is illegal.
	private void finish(final CompletableFuture<@Nullable Void> running, final ResourceLock lock,
			final AsyncResourcePermit.@Nullable Permit permit, final @Nullable Throwable throwable) {
		try {
			releaseResources(lock, permit);
		}
		catch (Throwable failure) {
			if (throwable != null && throwable != failure) {
				failure.addSuppressed(throwable);
			}
			running.completeExceptionally(failure);
			return;
		}
		// Completion can synchronously run parent callbacks that need these resources.
		if (throwable != null) {
			running.completeExceptionally(throwable);
		}
		else {
			running.complete(null);
		}
	}

	private void releaseResources(final ResourceLock lock, final AsyncResourcePermit.@Nullable Permit permit) {
		try {
			resourceGate.release(lock);
		}
		finally {
			if (permit != null) {
				permit.release();
			}
		}
	}

	@Override
	public void close() {
		workerPool.shutdown();
	}
}
