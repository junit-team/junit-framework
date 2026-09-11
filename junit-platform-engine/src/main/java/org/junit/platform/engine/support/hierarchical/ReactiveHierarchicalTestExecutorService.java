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

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.UnrecoverableExceptions;

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
		if (concurrent.isEmpty()) {
			return sequential;
		}
		if (testTasks.size() == concurrent.size()) {
			return CompletableFuture.allOf(concurrent.toArray(new CompletableFuture<?>[0]));
		}
		return sequential.thenCombine(CompletableFuture.allOf(concurrent.toArray(new CompletableFuture<?>[0])),
			(___, ____) -> null);
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

	private CompletionStage<@Nullable Void> acquireLockAndRun(TestTask testTask,
			AsyncResourcePermit.@Nullable Permit permit) {
		// Acquire the resource lock reactively first; then run the task body on a
		// worker thread, releasing the lock and the permit afterwards.
		// @formatter:off
		return resourceGate.acquire(testTask.getResourceLock())
				.thenCompose(lock -> runOnWorker(testTask, lock, permit));
		// @formatter:on
	}

	/**
	 * Run a task's body on a worker thread, freeing the worker as soon as a
	 * genuinely asynchronous body returns a pending stage, and release the lock
	 * and permit once the body terminates.
	 */
	@SuppressWarnings("FutureReturnValueIgnored")
	private CompletionStage<@Nullable Void> runOnWorker(TestTask testTask, ResourceLock lock,
			AsyncResourcePermit.@Nullable Permit permit) {
		CompletableFuture<@Nullable Void> running = new CompletableFuture<>();
		workerPool.execute(() -> {
			// Run only the synchronous preamble of the task on this worker thread
			// and chain the completion handler reactively instead of blocking on
			// join(). For an asynchronously-completing task the worker thread is
			// thereby freed immediately, so sibling CONCURRENT tasks can all be
			// dispatched without being throttled by the worker-pool size.
			final CompletableFuture<?> task;
			try {
				task = testTask.executeAsync().toCompletableFuture();
			}
			catch (Throwable throwable) {
				UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
				releaseResources(lock, permit);
				running.completeExceptionally(throwable);
				return;
			}
			task.whenComplete((___ignore, throwable) -> {
				try {
					if (throwable != null) {
						UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
						running.completeExceptionally(throwable);
					}
					else {
						running.complete(null);
					}
				}
				finally {
					releaseResources(lock, permit);
				}
			});
		});
		return running;
	}

	private void releaseResources(ResourceLock lock, AsyncResourcePermit.@Nullable Permit permit) {
		resourceGate.release(lock);
		if (permit != null) {
			permit.release();
		}
	}

	@Override
	public void close() {
		workerPool.shutdown();
	}
}
