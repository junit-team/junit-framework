/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.timeout;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.time.Duration;

import org.apiguardian.api.API;

@API(status = INTERNAL, since = "6.2")
public final class TimeoutUtils {

	private static final Duration MAX_NANO_DURATION = Duration.ofNanos(Long.MAX_VALUE);

	private TimeoutUtils() {
		/* no-op */
	}

	public static boolean isRepresentableInNanos(Duration duration) {
		return duration.compareTo(MAX_NANO_DURATION) <= 0;
	}
}
