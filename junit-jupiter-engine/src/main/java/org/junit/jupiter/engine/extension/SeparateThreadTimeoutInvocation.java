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

import static org.junit.jupiter.api.timeout.PreemptiveTimeoutUtils.executeWithPreemptiveTimeout;

import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;

/**
 * @since 5.9
 */
class SeparateThreadTimeoutInvocation<T extends @Nullable Object> implements Invocation<T> {

	private final TimeoutInvocationParameters<T> parameters;

	SeparateThreadTimeoutInvocation(TimeoutInvocationParameters<T> parameters) {
		this.parameters = parameters;
	}

	@Override
	@SuppressWarnings("NullAway")
	public T proceed() throws Throwable {
		var timeout = parameters.timeout();
		var delegate = parameters.invocation();
		var descriptionSupplier = parameters.descriptionSupplier();
		return executeWithPreemptiveTimeout(timeout.toDuration(), delegate::proceed, descriptionSupplier,
			(__, ___, cause, testThread) -> newTimeoutException(cause, testThread));
	}

	private TimeoutException newTimeoutException(@Nullable Throwable cause, @Nullable Thread testThread) {
		TimeoutException exception = TimeoutExceptionFactory.create(parameters.descriptionSupplier().get(),
			parameters.timeout(), parameters.threadDumpEnabled(), null);
		if (testThread != null) {
			parameters.preInterruptCallback().executePreInterruptCallback(testThread, exception::addSuppressed);
		}
		exception.initCause(cause);
		return exception;
	}
}
