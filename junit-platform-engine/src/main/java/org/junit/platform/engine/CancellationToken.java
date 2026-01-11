/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;

/**
 * Token that should be checked to determine whether an operation was requested
 * to be cancelled.
 *
 * <p>For example, this is used by the
 * {@link org.junit.platform.launcher.Launcher} and
 * {@link org.junit.platform.engine.TestEngine} implementations to determine
 * whether the current test execution should be cancelled.
 *
 * <p>This interface is not intended to be implemented by clients.
 *
 * @since 6.0
 * @see org.junit.platform.launcher.core.LauncherExecutionRequestBuilder#cancellationToken(CancellationToken)
 * @see org.junit.platform.launcher.LauncherExecutionRequest#getCancellationToken()
 * @see ExecutionRequest#getCancellationToken()
 */
@API(status = EXPERIMENTAL, since = "6.0")
public sealed interface CancellationToken permits RegularCancellationToken, DisabledCancellationToken {

	/**
	 * Create a new, uncancelled cancellation token.
	 */
	static CancellationToken create() {
		return new RegularCancellationToken();
	}

	/**
	 * Get a new cancellation token that cannot be cancelled.
	 *
	 * <p>This is only useful for cases when a cancellation token is required
	 * but is not supported or irrelevant, for example, in tests.
	 */
	static CancellationToken disabled() {
		return DisabledCancellationToken.INSTANCE;
	}

	/**
	 * {@return whether cancellation has been requested}
	 *
	 * <p>Once this method returns {@code true}, it will never return
	 * {@code false} in a subsequent call.
	 */
	boolean isCancellationRequested();

	/**
	 * Request cancellation.
	 *
	 * <p>This will call subsequent calls to {@link #isCancellationRequested()}
	 * to return {@code true}.
	 */
	void cancel();

}
