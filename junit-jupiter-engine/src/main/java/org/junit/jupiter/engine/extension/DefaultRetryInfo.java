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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RetryInfo;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of {@link RetryInfo}.
 *
 * @since 6.1
 */
record DefaultRetryInfo(int currentAttempt, int maxAttempts, long delayMs, AtomicInteger failureCount,
		AtomicBoolean succeeded) implements RetryInfo {

	@Override
	public int getCurrentAttempt() {
		return this.currentAttempt;
	}

	@Override
	public int getMaxAttempts() {
		return this.maxAttempts;
	}

	@Override
	public int getFailureCount() {
		return this.failureCount.get();
	}

	@Override
	public long getDelayMs() {
		return this.delayMs;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("currentAttempt", this.currentAttempt)
				.append("maxAttempts", this.maxAttempts)
				.append("delayMs", this.delayMs)
				.append("failureCount", this.failureCount)
				.append("succeeded", this.succeeded)
				.toString();
		// @formatter:on
	}

}
