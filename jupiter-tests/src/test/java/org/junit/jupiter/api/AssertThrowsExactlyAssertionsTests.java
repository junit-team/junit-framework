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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageContains;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.AssertionTestUtils.expectAssertionFailedError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;
import java.io.Serial;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.test.TestClassLoader;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.8
 */
@SuppressWarnings("ExcessiveLambdaUsage")
class AssertThrowsExactlyAssertionsTests {

	private static final Executable nix = () -> {
	};

	@Test
	void assertThrowsExactlyTheSpecifiedExceptionClass() {
		var actual = assertThrowsExactly(EnigmaThrowable.class, () -> {
			throw new EnigmaThrowable();
		});
		assertNotNull(actual);
	}

	@Test
	void assertThrowsExactlyWithTheExpectedChildException() {
		try {
			assertThrowsExactly(RuntimeException.class, () -> {
				throw new Exception();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "Unexpected exception type thrown, ");
			assertMessageContains(ex, "expected: <java.lang.RuntimeException>");
			assertMessageContains(ex, "but was: <java.lang.Exception>");
			assertThat(ex).hasCauseExactlyInstanceOf(Exception.class);
		}
	}

	@Test
	void assertThrowsExactlyWithTheExpectedParentException() {
		try {
			assertThrowsExactly(RuntimeException.class, () -> {
				throw new NumberFormatException();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "Unexpected exception type thrown, ");
			assertMessageContains(ex, "expected: <java.lang.RuntimeException>");
			assertMessageContains(ex, "but was: <java.lang.NumberFormatException>");
			assertThat(ex).hasCauseExactlyInstanceOf(NumberFormatException.class);
		}
	}

	@Test
	void assertThrowsWithMethodReferenceForNonVoidReturnType() {
		FutureTask<String> future = new FutureTask<>(() -> {
			throw new RuntimeException("boom");
		});
		future.run();

		ExecutionException exception = assertThrowsExactly(ExecutionException.class, future::get);
		assertNotNull(exception.getCause());
		assertEquals("boom", exception.getCause().getMessage());
	}

	@Test
	void assertThrowsWithMethodReferenceForVoidReturnType() {
		var object = new Object();
		IllegalMonitorStateException exception;

		exception = assertThrowsExactly(IllegalMonitorStateException.class, object::notify);
		assertNotNull(exception);

		// Note that Object.wait(...) is an overloaded method with a void return type
		exception = assertThrowsExactly(IllegalMonitorStateException.class, object::wait);
		assertNotNull(exception);
	}

	@Test
	void assertThrowsWithExecutableThatThrowsThrowable() {
		EnigmaThrowable enigmaThrowable = assertThrowsExactly(EnigmaThrowable.class, () -> {
			throw new EnigmaThrowable();
		});
		assertNotNull(enigmaThrowable);
	}

	@Test
	void assertThrowsWithExecutableThatThrowsThrowableWithMessage() {
		EnigmaThrowable enigmaThrowable = assertThrowsExactly(EnigmaThrowable.class, () -> {
			throw new EnigmaThrowable();
		}, "message");
		assertNotNull(enigmaThrowable);
	}

	@Test
	void assertThrowsWithExecutableThatThrowsThrowableWithMessageSupplier() {
		EnigmaThrowable enigmaThrowable = assertThrowsExactly(EnigmaThrowable.class, () -> {
			throw new EnigmaThrowable();
		}, () -> "message");
		assertNotNull(enigmaThrowable);
	}

	@Test
	void assertThrowsWithExecutableThatThrowsCheckedException() {
		IOException exception = assertThrowsExactly(IOException.class, () -> {
			throw new IOException();
		});
		assertNotNull(exception);
	}

	@Test
	void assertThrowsWithExecutableThatThrowsRuntimeException() {
		IllegalStateException illegalStateException = assertThrowsExactly(IllegalStateException.class, () -> {
			throw new IllegalStateException();
		});
		assertNotNull(illegalStateException);
	}

	@Test
	void assertThrowsWithExecutableThatThrowsError() {
		StackOverflowError stackOverflowError = assertThrowsExactly(StackOverflowError.class,
			AssertionTestUtils::recurseIndefinitely);
		assertNotNull(stackOverflowError);
	}

	@Test
	void assertThrowsWithExecutableThatDoesNotThrowAnException() {
		try {
			assertThrowsExactly(IllegalStateException.class, nix);
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageEquals(ex, "Expected java.lang.IllegalStateException to be thrown, but nothing was thrown.");
		}
	}

	@Test
	void assertThrowsWithExecutableThatDoesNotThrowAnExceptionWithMessageString() {
		try {
			assertThrowsExactly(IOException.class, nix, "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionError ex) {
			assertMessageEquals(ex,
				"Custom message ==> Expected java.io.IOException to be thrown, but nothing was thrown.");
		}
	}

	@Test
	void assertThrowsWithExecutableThatDoesNotThrowAnExceptionWithMessageSupplier() {
		try {
			assertThrowsExactly(IOException.class, nix, () -> "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionError ex) {
			assertMessageEquals(ex,
				"Custom message ==> Expected java.io.IOException to be thrown, but nothing was thrown.");
		}
	}

	@Test
	void assertThrowsWithExecutableThatThrowsAnUnexpectedException() {
		try {
			assertThrowsExactly(IllegalStateException.class, () -> {
				throw new NumberFormatException();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "Unexpected exception type thrown, ");
			assertMessageContains(ex, "expected: <java.lang.IllegalStateException>");
			assertMessageContains(ex, "but was: <java.lang.NumberFormatException>");
			assertThat(ex).hasCauseExactlyInstanceOf(NumberFormatException.class);
		}
	}

	@Test
	void assertThrowsWithExecutableThatThrowsAnUnexpectedExceptionWithMessageString() {
		try {
			assertThrowsExactly(IllegalStateException.class, () -> {
				throw new NumberFormatException();
			}, "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			// Should look something like this:
			// Custom message ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.NumberFormatException>
			assertMessageStartsWith(ex, "Custom message ==> ");
			assertMessageContains(ex, "Unexpected exception type thrown, ");
			assertMessageContains(ex, "expected: <java.lang.IllegalStateException>");
			assertMessageContains(ex, "but was: <java.lang.NumberFormatException>");
			assertThat(ex).hasCauseExactlyInstanceOf(NumberFormatException.class);
		}
	}

	@Test
	void assertThrowsWithExecutableThatThrowsAnUnexpectedExceptionWithMessageSupplier() {
		try {
			assertThrowsExactly(IllegalStateException.class, () -> {
				throw new NumberFormatException();
			}, () -> "Custom message");
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			// Should look something like this:
			// Custom message ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.NumberFormatException>
			assertMessageStartsWith(ex, "Custom message ==> ");
			assertMessageContains(ex, "Unexpected exception type thrown, ");
			assertMessageContains(ex, "expected: <java.lang.IllegalStateException>");
			assertMessageContains(ex, "but was: <java.lang.NumberFormatException>");
			assertThat(ex).hasCauseExactlyInstanceOf(NumberFormatException.class);
		}
	}

	@Test
	@SuppressWarnings("serial")
	void assertThrowsWithExecutableThatThrowsInstanceOfAnonymousInnerClassAsUnexpectedException() {
		try {
			assertThrowsExactly(IllegalStateException.class, () -> {
				throw new NumberFormatException() {
				};
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "Unexpected exception type thrown, ");
			assertMessageContains(ex, "expected: <java.lang.IllegalStateException>");
			// As of the time of this writing, the class name of the above anonymous inner
			// class is org.junit.jupiter.api.AssertThrowsExactlyAssertionsTests$2; however, hard
			// coding "$2" is fragile. So we just check for the presence of the "$"
			// appended to this class's name.
			assertMessageContains(ex, "but was: <" + getClass().getName() + "$");
			assertThat(ex).hasCauseInstanceOf(NumberFormatException.class);
		}
	}

	@Test
	void assertThrowsWithExecutableThatThrowsInstanceOfStaticNestedClassAsUnexpectedException() {
		try {
			assertThrowsExactly(IllegalStateException.class, () -> {
				throw new LocalException();
			});
			expectAssertionFailedError();
		}
		catch (AssertionFailedError ex) {
			assertMessageStartsWith(ex, "Unexpected exception type thrown, ");
			assertMessageContains(ex, "expected: <java.lang.IllegalStateException>");
			// The following verifies that the canonical name is used (i.e., "." instead of "$").
			assertMessageContains(ex, "but was: <" + LocalException.class.getName().replace("$", ".") + ">");
			assertThat(ex).hasCauseExactlyInstanceOf(LocalException.class);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void assertThrowsWithExecutableThatThrowsSameExceptionTypeFromDifferentClassLoader() throws Exception {
		try (var testClassLoader = TestClassLoader.forClasses(EnigmaThrowable.class)) {
			// Load expected exception type from different class loader
			Class<? extends Throwable> enigmaThrowableClass = (Class<? extends Throwable>) testClassLoader.loadClass(
				EnigmaThrowable.class.getName());

			try {
				assertThrowsExactly(enigmaThrowableClass, () -> {
					throw new EnigmaThrowable();
				});
				expectAssertionFailedError();
			}
			catch (AssertionFailedError ex) {
				// Example Output:
				//
				// Unexpected exception type thrown,
				// expected: <org.junit.jupiter.api.EnigmaThrowable@5d3411d>
				// but was: <org.junit.jupiter.api.EnigmaThrowable@2471cca7>

				assertMessageStartsWith(ex, "Unexpected exception type thrown, ");
				// The presence of the "@" sign is sufficient to indicate that the hash was
				// generated to disambiguate between the two identical class names.
				assertMessageContains(ex, "expected: <org.junit.jupiter.api.EnigmaThrowable@");
				assertMessageContains(ex, "but was: <org.junit.jupiter.api.EnigmaThrowable@");
				assertThat(ex).hasCauseExactlyInstanceOf(EnigmaThrowable.class);
			}
		}
	}

	// -------------------------------------------------------------------------

	private static class LocalException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 1L;
	}

}
