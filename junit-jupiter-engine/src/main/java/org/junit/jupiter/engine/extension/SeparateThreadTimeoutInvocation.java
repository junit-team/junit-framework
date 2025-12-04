/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.timeout.PreemptiveTimeoutUtils.executeWithPreemptiveTimeout;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;

/**
 * @since 5.9
 */
class SeparateThreadTimeoutInvocation<T extends @Nullable Object> implements Invocation<T> {

	private final Invocation<T> delegate;
	private final TimeoutDuration timeout;
	private final Supplier<String> descriptionSupplier;
	private final PreInterruptCallbackInvocation preInterruptCallback;

	SeparateThreadTimeoutInvocation(Invocation<T> delegate, TimeoutDuration timeout,
			Supplier<String> descriptionSupplier, PreInterruptCallbackInvocation preInterruptCallback) {
		this.delegate = delegate;
		this.timeout = timeout;
		this.descriptionSupplier = descriptionSupplier;
		this.preInterruptCallback = preInterruptCallback;
	}

	@Override
	@SuppressWarnings("NullAway")
	public T proceed() throws Throwable {
		return executeWithPreemptiveTimeout(timeout.toDuration(), delegate::proceed, descriptionSupplier,
			(__, ___, cause, testThread) -> {
				TimeoutException exception = TimeoutExceptionFactory.create(descriptionSupplier.get(), timeout, null);
				if (testThread != null) {
					preInterruptCallback.executePreInterruptCallback(testThread, exception::addSuppressed);
				}
				exception.initCause(cause);
				return exception;
			});
	}
}
