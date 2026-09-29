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

import static org.junit.jupiter.api.extension.PreInterruptCallback.THREAD_DUMP_ENABLED_PROPERTY_NAME;

import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.9
 */
class TimeoutExceptionFactory {

	private static final String DETAIL_MESSAGE_THREAD_DUMP_ENABLED = "see thread dump printed to System.out";
	private static final String DETAIL_MESSAGE_THREAD_DUMP_DISABLED = "to enable thread dumps, set the '"
			+ THREAD_DUMP_ENABLED_PROPERTY_NAME + "' configuration parameter to 'true'";

	private TimeoutExceptionFactory() {
	}

	static TimeoutException create(String methodSignature, TimeoutDuration timeoutDuration, boolean threadDumpEnabled,
			@Nullable Throwable failure) {
		String message = "%s timed out after %s (%s)".formatted(
			Preconditions.notNull(methodSignature, "method signature must not be null"),
			Preconditions.notNull(timeoutDuration, "timeout duration must not be null"),
			threadDumpEnabled ? DETAIL_MESSAGE_THREAD_DUMP_ENABLED : DETAIL_MESSAGE_THREAD_DUMP_DISABLED);
		TimeoutException timeoutException = new TimeoutException(message);
		if (failure != null) {
			timeoutException.addSuppressed(failure);
		}
		return timeoutException;
	}
}
