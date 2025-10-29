/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;

/**
 * @since 6.1
 */
abstract class BlockingAwareFuture<T extends @Nullable Object> extends DelegatingFuture<T> {

	BlockingAwareFuture(Future<T> delegate) {
		super(delegate);
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (delegate.isDone()) {
			return delegate.get();
		}
		return handleSafely(delegate::get);
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (delegate.isDone()) {
			return delegate.get();
		}
		return handleSafely(() -> delegate.get(timeout, unit));
	}

	private T handleSafely(Callable<T> callable) {
		try {
			return handle(callable);
		}
		catch (Exception e) {
			throw throwAsUncheckedException(e);
		}
	}

	protected abstract T handle(Callable<T> callable) throws Exception;

}
