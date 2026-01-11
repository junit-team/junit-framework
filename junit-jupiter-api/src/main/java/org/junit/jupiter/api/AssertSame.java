/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * {@code AssertSame} is a collection of utility methods that support asserting
 * two objects are the same.
 *
 * @since 5.0
 */
class AssertSame {

	private AssertSame() {
		/* no-op */
	}

	static void assertSame(@Nullable Object expected, @Nullable Object actual) {
		assertSame(expected, actual, (String) null);
	}

	static void assertSame(@Nullable Object expected, @Nullable Object actual, @Nullable String message) {
		if (expected != actual) {
			failNotSame(expected, actual, message);
		}
	}

	static void assertSame(@Nullable Object expected, @Nullable Object actual,
			Supplier<@Nullable String> messageSupplier) {
		if (expected != actual) {
			failNotSame(expected, actual, messageSupplier);
		}
	}

	private static void failNotSame(@Nullable Object expected, @Nullable Object actual,
			@Nullable Object messageOrSupplier) {
		assertionFailure() //
				.message(messageOrSupplier) //
				.expected(expected) //
				.actual(actual) //
				.trimStacktrace(Assertions.class) //
				.buildAndThrow();
	}

}
