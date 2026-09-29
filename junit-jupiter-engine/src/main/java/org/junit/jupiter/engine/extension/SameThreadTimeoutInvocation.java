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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * @since 5.5
 */
class SameThreadTimeoutInvocation<T extends @Nullable Object> implements Invocation<T> {

	private final TimeoutInvocationParameters<T> parameters;
	private final ScheduledExecutorService executor;

	SameThreadTimeoutInvocation(TimeoutInvocationParameters<T> parameters, ScheduledExecutorService executor) {
		this.parameters = parameters;
		this.executor = executor;
	}

	@SuppressWarnings("NullAway")
	@Override
	public T proceed() throws Throwable {
		InterruptTask interruptTask = new InterruptTask(Thread.currentThread(), parameters.preInterruptCallback());
		var timeout = parameters.timeout();
		ScheduledFuture<?> future = executor.schedule(interruptTask, timeout.value(), timeout.unit());
		Throwable failure = null;
		T result = null;
		try {
			result = parameters.invocation().proceed();
		}
		catch (Throwable t) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(t);
			failure = t;
		}
		finally {
			boolean cancelled = future.cancel(false);
			if (!cancelled) {
				future.get();
			}
			if (interruptTask.executed) {
				Thread.interrupted();
				failure = newTimeoutException(failure);
				interruptTask.attachSuppressedExceptions(failure);
			}
		}
		if (failure != null) {
			throw failure;
		}
		return result;
	}

	private TimeoutException newTimeoutException(@Nullable Throwable failure) {
		return TimeoutExceptionFactory.create(parameters.descriptionSupplier().get(), parameters.timeout(),
			parameters.threadDumpEnabled(), failure);
	}

	static class InterruptTask implements Runnable {
		private final PreInterruptCallbackInvocation preInterruptCallback;
		private final List<Throwable> exceptionsDuringInterruption = new CopyOnWriteArrayList<>();
		private final Thread thread;
		private volatile boolean executed;

		InterruptTask(Thread thread, PreInterruptCallbackInvocation preInterruptCallback) {
			this.thread = thread;
			this.preInterruptCallback = preInterruptCallback;
		}

		@Override
		public void run() {
			executed = true;
			preInterruptCallback.executePreInterruptCallback(thread, exceptionsDuringInterruption::add);
			thread.interrupt();
		}

		void attachSuppressedExceptions(Throwable outerException) {
			for (Throwable throwable : exceptionsDuringInterruption) {
				outerException.addSuppressed(throwable);
			}
		}
	}

}
