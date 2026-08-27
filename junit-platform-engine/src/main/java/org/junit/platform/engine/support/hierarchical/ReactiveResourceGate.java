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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A non-blocking FIFO gate for acquiring {@linkplain ResourceLock resource
 * locks} without ever parking a thread.
 *
 * <p>Mutual exclusion is enforced per resource key: only one exclusive holder
 * (or multiple shared holders) may own a given key at any point in time.
 * Waiting acquirers register a {@link CompletableFuture} in the FIFO of the
 * first busy key; when that key is released the longest-waiting acquirer is
 * handed the lock directly ({@code FIFO}) without involving a thread pool.
 *
 * <p>Composite locks (multiple resource keys) are acquired all-or-nothing:
 * if any constituent key is busy, the acquisition defers on that key and
 * releases any keys already acquired, waiting for all to become free (no
 * hold-and-wait, so no deadlock).
 *
 * <p>All bookkeeping is guarded by a single monitor, which makes the
 * enqueue-then-recheck sequence atomic and therefore free of lost-wakeup
 * races. Task execution happens outside the monitor.
 *
 * <p>The returned {@link CompletionStage} is used purely as a signal that the
 * lock is held; it completes with the same {@link ResourceLock} instance, which
 * MUST be released exactly once via {@link ResourceLock#release()}.
 *
 * @since 6.2
 */
final class ReactiveResourceGate {

	private final Map<String, KeyState> states = new HashMap<>();

	/**
	 * Acquire the supplied lock asynchronously, completing only once all of its
	 * resources are held.
	 *
	 * @param lock the resource lock to acquire; never {@code null}
	 * @return a completion stage that completes with {@code lock} once held
	 */
	synchronized CompletionStage<ResourceLock> acquire(ResourceLock lock) {
		List<ExclusiveResource> resources = lock.getResources();
		if (resources.isEmpty()) {
			return CompletableFuture.completedFuture(lock);
		}
		CompletableFuture<ResourceLock> result = new CompletableFuture<>();
		performAcquire(lock, resources, result);
		return result;
	}

	/**
	 * Release a previously acquired lock and hand each of its resources to the
	 * next waiter in FIFO order.
	 *
	 * @param lock the acquired resource lock to release; never {@code null}
	 */
	synchronized void release(ResourceLock lock) {
		List<ExclusiveResource> resources = lock.getResources();
		if (resources.isEmpty()) {
			return;
		}
		for (int i = resources.size() - 1; i >= 0; i--) {
			KeyState state = stateFor(resources.get(i).getKey());
			state.release();
			state.advance();
		}
	}

	/**
	 * Must be called while holding {@code this} monitor. Acquires all resources
	 * all-or-nothing; on conflict defers on the first busy key and re-checks
	 * once that key is released.
	 */
	private void performAcquire(ResourceLock lock, List<ExclusiveResource> resources,
			CompletableFuture<ResourceLock> result) {
		List<KeyState> acquired = new ArrayList<>(resources.size());
		for (ExclusiveResource resource : resources) {
			KeyState state = stateFor(resource.getKey());
			if (!state.acquire(resource.getLockMode())) {
				// Release what we already hold (reverse order) and defer on the
				// first busy key.
				releaseAcquired(acquired);
				state.enqueue(this, lock, resources, result);
				return;
			}
			acquired.add(state);
		}
		result.complete(lock);
	}

	private void releaseAcquired(List<KeyState> acquired) {
		for (int i = acquired.size() - 1; i >= 0; i--) {
			acquired.get(i).release();
		}
	}

	private KeyState stateFor(String key) {
		return states.computeIfAbsent(key, __ -> new KeyState());
	}

	/**
	 * Per-key holder state plus the FIFO of acquirers waiting to overtake it.
	 */
	private static final class KeyState {

		private int sharedHolders;
		private boolean heldExclusive;
		private final Deque<Acquisition> waiters = new ArrayDeque<>();

		/** Must be called while holding the surrounding gate monitor. */
		boolean acquire(ExclusiveResource.LockMode mode) {
			if (mode == ExclusiveResource.LockMode.READ) {
				if (heldExclusive) {
					return false;
				}
				sharedHolders++;
				return true;
			}
			// WRITE or READ_WRITE: exclusive access required.
			if (heldExclusive || sharedHolders > 0) {
				return false;
			}
			heldExclusive = true;
			return true;
		}

		/** Must be called while holding the surrounding gate monitor. */
		void release() {
			if (heldExclusive) {
				heldExclusive = false;
			}
			else if (sharedHolders > 0) {
				sharedHolders--;
			}
			else {
				return;
			}
		}

		/** Must be called while holding the surrounding gate monitor. */
		void enqueue(ReactiveResourceGate gate, ResourceLock lock, List<ExclusiveResource> resources,
				CompletableFuture<ResourceLock> result) {
			waiters.addLast(new Acquisition(gate, lock, resources, result));
		}

		/**
		 * Advance the FIFO: after a holder releases, grant the key to the head
		 * waiter and let it attempt to acquire all of its resources.
		 *
		 * Must be called while holding the surrounding gate monitor. Returns
		 * {@code true} if the head waiter overtook this key.
		 */
		boolean advance() {
			while (!waiters.isEmpty()) {
				if (heldExclusive || sharedHolders > 0) {
					return false;
				}
				Acquisition next = waiters.pollFirst();
				next.attempt();
				// Only one waiter may hold this key at a time; attempt() either
				// acquired this key (stopping the loop) or deferred somewhere else.
				if (heldExclusive || sharedHolders > 0) {
					return true;
				}
				// If attempt() did not acquire this key (it deferred on another),
				// the head has advanced; try the next waiter.
			}
			return false;
		}
	}

	/**
	 * A pending acquisition that must obtain every one of its resources before
	 * completing.
	 */
	private static final class Acquisition {

		private final ReactiveResourceGate gate;
		private final ResourceLock lock;
		private final List<ExclusiveResource> resources;
		private final CompletableFuture<ResourceLock> result;

		Acquisition(ReactiveResourceGate gate, ResourceLock lock, List<ExclusiveResource> resources,
				CompletableFuture<ResourceLock> result) {
			this.gate = gate;
			this.lock = lock;
			this.resources = resources;
			this.result = result;
		}

		/** Must be called while holding the gate's monitor. */
		void attempt() {
			gate.performAcquire(lock, resources, result);
		}
	}
}
