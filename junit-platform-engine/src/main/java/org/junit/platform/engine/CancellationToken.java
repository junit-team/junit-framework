/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import org.apiguardian.api.API;

/**
 * Token that should be checked to determine whether an operation was requested
 * to be cancelled.
 *
 * <p>For example, this is used by {@link org.junit.platform.engine.TestEngine}
 * implementations to determine whether
 *
 * <p>This interface is not intended to be implemented by clients.
 *
 * @since 6.0
 * @see ExecutionRequest#getCancellationToken()
 */
@API(status = API.Status.EXPERIMENTAL, since = "6.0")
public interface CancellationToken {

	static CancellationToken create() {
		return new DefaultCancellationToken();
	}

	boolean isCancellationRequested();

	void cancel();

}
