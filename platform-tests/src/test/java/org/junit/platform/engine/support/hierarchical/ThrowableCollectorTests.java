/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @since 1.6
 */
class ThrowableCollectorTests {

	@Test
	void successfulExecution() {
		var collector = new ThrowableCollector(x -> true);
		collector.execute(() -> {
		});
		var result = collector.toTestExecutionResult();

		assertEquals(SUCCESSFUL, result.getStatus());
		assertEquals(Optional.empty(), result.getThrowable());
	}

	@Test
	void abortedExecution() {
		var customAbort = new CustomAbort();

		var collector = new ThrowableCollector(CustomAbort.class::isInstance);
		collector.execute(() -> {
			throw customAbort;
		});
		var result = collector.toTestExecutionResult();

		assertEquals(ABORTED, result.getStatus());
		assertSame(customAbort, result.getThrowable().get());
	}

	@Test
	void failedExecution() {
		var assertionError = new AssertionError("assertion violated");

		var collector = new ThrowableCollector(CustomAbort.class::isInstance);
		collector.execute(() -> {
			throw assertionError;
		});
		var result = collector.toTestExecutionResult();

		assertEquals(FAILED, result.getStatus());
		assertSame(assertionError, result.getThrowable().get());
	}

	private static class CustomAbort extends Error {
		private static final long serialVersionUID = 1L;
	}
}
