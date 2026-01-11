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
import static org.junit.jupiter.api.AssertionUtils.getCanonicalName;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * {@code AssertThrows} is a collection of utility methods that support asserting
 * an exception of an expected type is thrown.
 *
 * @since 5.0
 */
class AssertThrows {

	private AssertThrows() {
		/* no-op */
	}

	static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) {
		return assertThrows(expectedType, executable, (Object) null);
	}

	static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable,
			@Nullable String message) {
		return assertThrows(expectedType, executable, (Object) message);
	}

	static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable,
			Supplier<@Nullable String> messageSupplier) {

		return assertThrows(expectedType, executable, (Object) messageSupplier);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable,
			@Nullable Object messageOrSupplier) {

		try {
			executable.execute();
		}
		catch (Throwable actualException) {
			if (expectedType.isInstance(actualException)) {
				return (T) actualException;
			}
			else {
				UnrecoverableExceptions.rethrowIfUnrecoverable(actualException);
				throw assertionFailure() //
						.message(messageOrSupplier) //
						.expected(expectedType) //
						.actual(actualException.getClass()) //
						.reason("Unexpected exception type thrown") //
						.cause(actualException) //
						.trimStacktrace(Assertions.class) //
						.build();
			}
		}
		throw assertionFailure() //
				.message(messageOrSupplier) //
				.reason("Expected %s to be thrown, but nothing was thrown.".formatted(getCanonicalName(expectedType))) //
				.trimStacktrace(Assertions.class) //
				.build();
	}

}
