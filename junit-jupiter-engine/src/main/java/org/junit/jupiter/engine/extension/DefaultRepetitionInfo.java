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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RepetitionInfo;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of {@link RepetitionInfo}.
 */
record DefaultRepetitionInfo(int currentRepetition, int totalRepetitions, AtomicInteger failureCount,
		int failureThreshold) implements RepetitionInfo {

	@Override
	public int getCurrentRepetition() {
		return this.currentRepetition;
	}

	@Override
	public int getTotalRepetitions() {
		return this.totalRepetitions;
	}

	@Override
	public int getFailureCount() {
		return this.failureCount.get();
	}

	@Override
	public int getFailureThreshold() {
		return this.failureThreshold;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("currentRepetition", this.currentRepetition)
				.append("totalRepetitions", this.totalRepetitions)
				.append("failureCount", this.failureCount)
				.append("failureThreshold", this.failureThreshold)
				.toString();
		// @formatter:on
	}

}
