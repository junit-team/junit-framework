/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;

import java.time.Duration;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;

/**
 * {@code AssertTimeout} is a collection of utility methods that support asserting
 * the execution of the code under test did not take longer than the timeout duration.
 *
 * @since 5.0
 */
class AssertTimeout {

	private AssertTimeout() {
		/* no-op */
	}

	static void assertTimeout(Duration timeout, Executable executable) {
		assertTimeout(timeout, executable, (String) null);
	}

	static void assertTimeout(Duration timeout, Executable executable, @Nullable String message) {
		AssertTimeout.<@Nullable Object> assertTimeout(timeout, () -> {
			executable.execute();
			return null;
		}, message);
	}

	static void assertTimeout(Duration timeout, Executable executable, Supplier<@Nullable String> messageSupplier) {
		AssertTimeout.<@Nullable Object> assertTimeout(timeout, () -> {
			executable.execute();
			return null;
		}, messageSupplier);
	}

	static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier) {
		return assertTimeout(timeout, supplier, (Object) null);
	}

	static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier,
			@Nullable String message) {
		return assertTimeout(timeout, supplier, (Object) message);
	}

	static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier,
			Supplier<@Nullable String> messageSupplier) {
		return assertTimeout(timeout, supplier, (Object) messageSupplier);
	}

	private static <T extends @Nullable Object> T assertTimeout(Duration timeout, ThrowingSupplier<T> supplier,
			@Nullable Object messageOrSupplier) {
		long timeoutInMillis = timeout.toMillis();
		long start = System.currentTimeMillis();
		T result;
		try {
			result = supplier.get();
		}
		catch (Throwable ex) {
			throw throwAsUncheckedException(ex);
		}

		long timeElapsed = System.currentTimeMillis() - start;
		if (timeElapsed > timeoutInMillis) {
			assertionFailure() //
					.message(messageOrSupplier) //
					.reason("execution exceeded timeout of " + timeoutInMillis + " ms by "
							+ (timeElapsed - timeoutInMillis) + " ms") //
					.buildAndThrow();
		}
		return result;
	}

}
