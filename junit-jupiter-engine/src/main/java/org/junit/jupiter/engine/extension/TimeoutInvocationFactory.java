/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.9
 */
class TimeoutInvocationFactory {

	private final Store store;

	TimeoutInvocationFactory(Store store) {
		this.store = Preconditions.notNull(store, "store must not be null");
	}

	<T extends @Nullable Object> Invocation<T> create(ThreadMode threadMode,
			TimeoutInvocationParameters<T> parameters) {
		Preconditions.notNull(parameters, "timeout invocation parameters must not be null");
		return switch (Preconditions.notNull(threadMode, "thread mode must not be null")) {
			case SAME_THREAD -> new SameThreadTimeoutInvocation<>(parameters,
				getThreadExecutorForSameThreadInvocation());
			case SEPARATE_THREAD -> new SeparateThreadTimeoutInvocation<>(parameters);
			case INFERRED -> throw new PreconditionViolationException("thread mode must not be INFERRED");
		};
	}

	@SuppressWarnings("resource")
	private ScheduledExecutorService getThreadExecutorForSameThreadInvocation() {
		return store.computeIfAbsent(SingleThreadExecutorResource.class).get();
	}

	@SuppressWarnings({ "deprecation", "try" })
	private abstract static class ExecutorResource implements Store.CloseableResource, AutoCloseable {

		private final ScheduledExecutorService executor;

		ExecutorResource(ScheduledExecutorService executor) {
			this.executor = executor;
		}

		ScheduledExecutorService get() {
			return executor;
		}

		@Override
		public void close() throws Exception {
			executor.shutdown();
			boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
			if (!terminated) {
				executor.shutdownNow();
				throw new JUnitException("Scheduled executor could not be stopped in an orderly manner");
			}
		}
	}

	@SuppressWarnings("try")
	static class SingleThreadExecutorResource extends ExecutorResource {

		@SuppressWarnings({ "unused", "ThreadPriorityCheck" })
		SingleThreadExecutorResource() {
			super(Executors.newSingleThreadScheduledExecutor(runnable -> {
				Thread thread = new Thread(runnable, "junit-jupiter-timeout-watcher");
				thread.setPriority(Thread.MAX_PRIORITY);
				return thread;
			}));
		}
	}

}
