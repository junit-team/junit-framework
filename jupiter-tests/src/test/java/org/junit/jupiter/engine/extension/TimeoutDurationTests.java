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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @since 5.5
 */
class TimeoutDurationTests {

	@Test
	void formatsDurationNicely() {
		assertThat(new TimeoutDuration(1, SECONDS)).hasToString("1 second");
		assertThat(new TimeoutDuration(2, SECONDS)).hasToString("2 seconds");
	}

	@Test
	void fulfillsEqualsAndHashCodeContract() {
		var oneSecond = new TimeoutDuration(1, SECONDS);

		assertThat(oneSecond) //
				.isEqualTo(oneSecond) //
				.isEqualTo(new TimeoutDuration(1, SECONDS)) //
				.hasSameHashCodeAs(new TimeoutDuration(1, SECONDS)) //
				.isNotEqualTo("foo") //
				.isNotEqualTo(new TimeoutDuration(2, SECONDS)) //
				.isNotEqualTo(new TimeoutDuration(1, MINUTES));
	}

}
