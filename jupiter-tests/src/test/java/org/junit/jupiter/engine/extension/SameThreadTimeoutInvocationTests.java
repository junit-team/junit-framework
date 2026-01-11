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

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

/**
 * @since 5.5
 */
class SameThreadTimeoutInvocationTests {

	@Test
	void resetsInterruptFlag() {
		var exception = assertThrows(TimeoutException.class, () -> withExecutor(executor -> {
			var delegate = new EventuallyInterruptibleInvocation();
			var duration = new TimeoutDuration(1, NANOSECONDS);
			var timeoutInvocation = new SameThreadTimeoutInvocation<>(delegate, duration, executor, () -> "execution",
				PreInterruptCallbackInvocation.NOOP);
			timeoutInvocation.proceed();
		}));
		assertFalse(Thread.currentThread().isInterrupted());
		assertThat(exception).hasMessage("execution timed out after 1 nanosecond");
	}

	private void withExecutor(ThrowingConsumer<ScheduledExecutorService> consumer) throws Throwable {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		try {
			consumer.accept(executor);
		}
		finally {
			executor.shutdown();
			assertTrue(executor.awaitTermination(5, SECONDS));
		}
	}
}
