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

import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.2
 */
class AssertDoesNotThrowAssertionsTests {

	private static final Executable nix = () -> {
	};

	private static final ThrowingSupplier<String> something = () -> "enigma";

	@Test
	void assertDoesNotThrowWithMethodReferenceForNonVoidReturnType() {
		FutureTask<String> future = new FutureTask<>(() -> "foo");
		future.run();

		String result;

		// Current compiler's type inference: does NOT compile since the compiler
		// cannot figure out which overloaded variant of assertDoesNotThrow() to
		// invoke (i.e., Executable vs. ThrowingSupplier).
		//
		// result = assertDoesNotThrow(future::get);

		// Explicitly as an Executable
		assertDoesNotThrow((Executable) future::get);

		// Explicitly as a ThrowingSupplier
		result = assertDoesNotThrow((ThrowingSupplier<String>) future::get);
		assertEquals("foo", result);
	}

	@Test
	void assertDoesNotThrowWithMethodReferenceForVoidReturnType() {
		var foo = new Foo();

		// Note: the following does not compile since the compiler cannot properly
		// perform type inference for a method reference for an overloaded method
		// that has a void return type such as Foo.overloaded(...), IFF the
		// compiler is simultaneously trying to pick which overloaded variant
		// of assertDoesNotThrow() to invoke.
		//
		// assertDoesNotThrow(foo::overloaded);

		// Current compiler's type inference
		assertDoesNotThrow(foo::normalMethod);

		// Explicitly as an Executable
		assertDoesNotThrow(foo::normalMethod);
		assertDoesNotThrow((Executable) foo::overloaded);
	}

	// --- executable ----------------------------------------------------------

	@Test
	void assertDoesNotThrowAnythingWithExecutable() {
		assertDoesNotThrow(nix);
	}

	@Test
	void assertDoesNotThrowAnythingWithExecutableAndMessage() {
		assertDoesNotThrow(nix, "message");
	}

	@Test
	void assertDoesNotThrowAnythingWithExecutableAndMessageSupplier() {
		assertDoesNotThrow(nix, () -> "message");
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsACheckedException() {
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IOException();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Unexpected exception thrown: " + IOException.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsACheckedExceptionWithMessage() {
		String message = "Checked exception message";
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IOException(message);
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Unexpected exception thrown: " + IOException.class.getName() + ": " + message);
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsARuntimeException() {
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IllegalStateException();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Unexpected exception thrown: " + IllegalStateException.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsARuntimeExceptionWithMessage() {
		String message = "Runtime exception message";
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IllegalStateException(message);
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex,
				"Unexpected exception thrown: " + IllegalStateException.class.getName() + ": " + message);
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsAnError() {
		try {
			assertDoesNotThrow(AssertionTestUtils::recurseIndefinitely);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Unexpected exception thrown: " + StackOverflowError.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsAnExceptionWithMessageString() {
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IllegalStateException();
			}, "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex,
				"Custom message ==> Unexpected exception thrown: " + IllegalStateException.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsAnExceptionWithMessageWithMessageString() {
		String message = "Runtime exception message";
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IllegalStateException(message);
			}, "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Custom message ==> Unexpected exception thrown: "
					+ IllegalStateException.class.getName() + ": " + message);
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsAnExceptionWithMessageSupplier() {
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IllegalStateException();
			}, () -> "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex,
				"Custom message ==> Unexpected exception thrown: " + IllegalStateException.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithExecutableThatThrowsAnExceptionWithMessageWithMessageSupplier() {
		String message = "Runtime exception message";
		try {
			assertDoesNotThrow((Executable) () -> {
				throw new IllegalStateException(message);
			}, () -> "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Custom message ==> Unexpected exception thrown: "
					+ IllegalStateException.class.getName() + ": " + message);
		}
	}

	// --- supplier ------------------------------------------------------------

	@Test
	void assertDoesNotThrowAnythingWithSupplier() {
		assertEquals("enigma", assertDoesNotThrow(something));
	}

	@Test
	void assertDoesNotThrowAnythingWithSupplierAndMessage() {
		assertEquals("enigma", assertDoesNotThrow(something, "message"));
	}

	@Test
	void assertDoesNotThrowAnythingWithSupplierAndMessageSupplier() {
		assertEquals("enigma", assertDoesNotThrow(something, () -> "message"));
	}

	@Test
	void assertDoesNotThrowWithSupplierThatThrowsACheckedException() {
		try {
			assertDoesNotThrow((ThrowingSupplier<?>) () -> {
				throw new IOException();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Unexpected exception thrown: " + IOException.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithSupplierThatThrowsARuntimeException() {
		try {
			assertDoesNotThrow((ThrowingSupplier<?>) () -> {
				throw new IllegalStateException();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Unexpected exception thrown: " + IllegalStateException.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithSupplierThatThrowsAnError() {
		try {
			assertDoesNotThrow((ThrowingSupplier<?>) () -> {
				throw new StackOverflowError();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Unexpected exception thrown: " + StackOverflowError.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithSupplierThatThrowsAnExceptionWithMessageString() {
		try {
			assertDoesNotThrow((ThrowingSupplier<?>) () -> {
				throw new IllegalStateException();
			}, "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex,
				"Custom message ==> Unexpected exception thrown: " + IllegalStateException.class.getName());
		}
	}

	@Test
	void assertDoesNotThrowWithSupplierThatThrowsAnExceptionWithMessageSupplier() {
		try {
			assertDoesNotThrow((ThrowingSupplier<?>) () -> {
				throw new IllegalStateException();
			}, () -> "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex,
				"Custom message ==> Unexpected exception thrown: " + IllegalStateException.class.getName());
		}
	}

	// -------------------------------------------------------------------------

	private static class Foo {

		void normalMethod() {
		}

		void overloaded() {
		}

		@SuppressWarnings("unused")
		void overloaded(int i) {
		}

	}

}
