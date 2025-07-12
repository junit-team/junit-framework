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
 * {@code AssertInstanceOf} is a collection of utility methods that support
 * asserting that an object is of an expected type &mdash; in other words, if it
 * can be assigned to the expected type.
 *
 * @since 5.8
 */
class AssertInstanceOf {

	private AssertInstanceOf() {
		/* no-op */
	}

	@Contract("_, null -> fail")
	static <T> T assertInstanceOf(Class<T> expectedType, @Nullable Object actualValue) {
		return assertInstanceOf(expectedType, actualValue, (Object) null);
	}

	@Contract("_, null, _ -> fail")
	static <T> T assertInstanceOf(Class<T> expectedType, @Nullable Object actualValue, @Nullable String message) {
		return assertInstanceOf(expectedType, actualValue, (Object) message);
	}

	@Contract("_, null, _ -> fail")
	static <T> T assertInstanceOf(Class<T> expectedType, @Nullable Object actualValue,
			Supplier<@Nullable String> messageSupplier) {
		return assertInstanceOf(expectedType, actualValue, (Object) messageSupplier);
	}

	@SuppressWarnings("NullAway")
	private static <T> T assertInstanceOf(Class<T> expectedType, @Nullable Object actualValue,
			@Nullable Object messageOrSupplier) {
		if (!expectedType.isInstance(actualValue)) {
			assertionFailure() //
					.message(messageOrSupplier) //
					.reason(actualValue == null ? "Unexpected null value" : "Unexpected type") //
					.expected(expectedType) //
					.actual(actualValue == null ? null : actualValue.getClass()) //
					.buildAndThrow();
		}
		return expectedType.cast(actualValue);
	}

}
