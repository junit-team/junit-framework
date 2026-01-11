/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.legacy.xml;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * @since 1.0
 */
final class IncrementingClock extends Clock {

	private final Duration duration;
	private final ZoneId zone;

	private int counter;

	IncrementingClock(int start, Duration duration) {
		this(start, duration, ZoneId.systemDefault());
	}

	private IncrementingClock(int start, Duration duration, ZoneId zone) {
		this.counter = start;
		this.duration = duration;
		this.zone = zone;
	}

	@Override
	public Instant instant() {
		return Instant.EPOCH.plus(duration.multipliedBy(counter++));
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new IncrementingClock(counter, duration, zone);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}
}
