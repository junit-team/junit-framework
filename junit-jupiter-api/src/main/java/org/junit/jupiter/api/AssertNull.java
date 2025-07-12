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

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.annotation.Contract;

/**
 * {@code AssertNull} is a collection of utility methods that support asserting
 * there is no object.
 *
 * @since 5.0
 */
class AssertNull {

	private AssertNull() {
		/* no-op */
	}

	@Contract("!null -> fail")
	static void assertNull(@Nullable Object actual) {
		assertNull(actual, (String) null);
	}

	@Contract("!null, _ -> fail")
	static void assertNull(@Nullable Object actual, @Nullable String message) {
		if (actual != null) {
			failNotNull(actual, message);
		}
	}

	@Contract("!null, _ -> fail")
	static void assertNull(@Nullable Object actual, Supplier<@Nullable String> messageSupplier) {
		if (actual != null) {
			failNotNull(actual, messageSupplier);
		}
	}

	private static void failNotNull(@Nullable Object actual, @Nullable Object messageOrSupplier) {
		assertionFailure() //
				.message(messageOrSupplier) //
				.expected(null) //
				.actual(actual) //
				.buildAndThrow();
	}

}
