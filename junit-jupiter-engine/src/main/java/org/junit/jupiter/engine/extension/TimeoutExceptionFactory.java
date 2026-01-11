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

import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.9
 */
class TimeoutExceptionFactory {

	private TimeoutExceptionFactory() {
	}

	static TimeoutException create(String methodSignature, TimeoutDuration timeoutDuration,
			@Nullable Throwable failure) {
		String message = "%s timed out after %s".formatted(
			Preconditions.notNull(methodSignature, "method signature must not be null"),
			Preconditions.notNull(timeoutDuration, "timeout duration must not be null"));
		TimeoutException timeoutException = new TimeoutException(message);
		if (failure != null) {
			timeoutException.addSuppressed(failure);
		}
		return timeoutException;
	}

	static TimeoutException create(String methodSignature, TimeoutDuration timeoutDuration) {
		return create(methodSignature, timeoutDuration, null);
	}
}
