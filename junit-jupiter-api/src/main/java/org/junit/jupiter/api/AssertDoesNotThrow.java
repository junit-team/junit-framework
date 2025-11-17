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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.opentest4j.AssertionFailedError;

/**
 * {@code AssertDoesNotThrow} is a collection of utility methods that support
 * explicitly asserting that a given code block does not throw an exception.
 *
 * @since 5.2
 */
class AssertDoesNotThrow {

	private AssertDoesNotThrow() {
		/* no-op */
	}

	static void assertDoesNotThrow(Executable executable) {
		assertDoesNotThrow(executable, (Object) null);
	}

	static void assertDoesNotThrow(Executable executable, @Nullable String message) {
		assertDoesNotThrow(executable, (Object) message);
	}

	static void assertDoesNotThrow(Executable executable, Supplier<@Nullable String> messageSupplier) {
		assertDoesNotThrow(executable, (Object) messageSupplier);
	}

	private static void assertDoesNotThrow(Executable executable, @Nullable Object messageOrSupplier) {
		try {
			executable.execute();
		}
		catch (Throwable t) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(t);
			throw createAssertionFailedError(messageOrSupplier, t);
		}
	}

	static <T extends @Nullable Object> T assertDoesNotThrow(ThrowingSupplier<T> supplier) {
		return assertDoesNotThrow(supplier, (Object) null);
	}

	static <T extends @Nullable Object> T assertDoesNotThrow(ThrowingSupplier<T> supplier, @Nullable String message) {
		return assertDoesNotThrow(supplier, (Object) message);
	}

	static <T extends @Nullable Object> T assertDoesNotThrow(ThrowingSupplier<T> supplier,
			Supplier<@Nullable String> messageSupplier) {
		return assertDoesNotThrow(supplier, (Object) messageSupplier);
	}

	private static <T extends @Nullable Object> T assertDoesNotThrow(ThrowingSupplier<T> supplier,
			@Nullable Object messageOrSupplier) {
		try {
			return supplier.get();
		}
		catch (Throwable t) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(t);
			throw createAssertionFailedError(messageOrSupplier, t);
		}
	}

	@API(status = INTERNAL, since = "6.0")
	public static AssertionFailedError createAssertionFailedError(@Nullable Object messageOrSupplier, Throwable t) {
		return assertionFailure() //
				.message(messageOrSupplier) //
				.reason("Unexpected exception thrown: " + t.getClass().getName() + buildSuffix(t.getMessage())) //
				.cause(t) //
				.trimStacktrace(Assertions.class) //
				.build();
	}

	private static String buildSuffix(@Nullable String message) {
		return StringUtils.isNotBlank(message) ? ": " + message : "";
	}

}
