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

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for {@link AssertTimeoutPreemptively}.
 *
 * @since 5.0
 */
class AssertTimeoutPreemptivelyAssertionsTests {

	private static final Duration PREEMPTIVE_TIMEOUT = ofMillis(WINDOWS.isCurrentOs() ? 1000 : 100);

	private static final ThreadLocal<AtomicBoolean> changed = ThreadLocal.withInitial(() -> new AtomicBoolean(false));

	private final Executable nix = () -> {
	};

	// --- executable ----------------------------------------------------------

	@Test
	void assertTimeoutPreemptivelyForExecutableThatCompletesBeforeTheTimeout() {
		changed.get().set(false);
		assertTimeoutPreemptively(ofMillis(500), () -> changed.get().set(true));
		assertFalse(changed.get().get(), "should have executed in a different thread");
		assertTimeoutPreemptively(ofMillis(500), nix, "message");
		assertTimeoutPreemptively(ofMillis(500), nix, () -> "message");
	}

	@Test
	void assertTimeoutPreemptivelyForExecutableThatThrowsAnException() {
		RuntimeException exception = assertThrows(RuntimeException.class,
			() -> assertTimeoutPreemptively(ofMillis(500), () -> {
				throw new RuntimeException("not this time");
			}));
		assertMessageEquals(exception, "not this time");
	}

	@Test
	void assertTimeoutPreemptivelyForExecutableThatThrowsAnAssertionFailedError() {
		AssertionFailedError exception = assertThrows(AssertionFailedError.class,
			() -> assertTimeoutPreemptively(ofMillis(500), () -> fail("enigma")));
		assertMessageEquals(exception, "enigma");
	}

	@Test
	void assertTimeoutPreemptivelyForExecutableThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class,
			() -> assertTimeoutPreemptively(PREEMPTIVE_TIMEOUT, this::waitForInterrupt));
		assertMessageEquals(error, "execution timed out after " + PREEMPTIVE_TIMEOUT.toMillis() + " ms");
		assertMessageStartsWith(error.getCause(), "Execution timed out in ");
		assertStackTraceContains(error.getCause(), "CountDownLatch", "await");
	}

	@Test
	void assertTimeoutPreemptivelyWithMessageForExecutableThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class,
			() -> assertTimeoutPreemptively(PREEMPTIVE_TIMEOUT, this::waitForInterrupt, "Tempus Fugit"));
		assertMessageEquals(error,
			"Tempus Fugit ==> execution timed out after " + PREEMPTIVE_TIMEOUT.toMillis() + " ms");
		assertMessageStartsWith(error.getCause(), "Execution timed out in ");
		assertStackTraceContains(error.getCause(), "CountDownLatch", "await");
	}

	@Test
	void assertTimeoutPreemptivelyWithMessageSupplierForExecutableThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class,
			() -> assertTimeoutPreemptively(PREEMPTIVE_TIMEOUT, this::waitForInterrupt,
				() -> "Tempus" + " " + "Fugit"));
		assertMessageEquals(error,
			"Tempus Fugit ==> execution timed out after " + PREEMPTIVE_TIMEOUT.toMillis() + " ms");
		assertMessageStartsWith(error.getCause(), "Execution timed out in ");
		assertStackTraceContains(error.getCause(), "CountDownLatch", "await");
	}

	@Test
	void assertTimeoutPreemptivelyWithMessageSupplierForExecutableThatCompletesBeforeTheTimeout() {
		assertTimeoutPreemptively(ofMillis(500), nix, () -> "Tempus" + " " + "Fugit");
	}

	// --- supplier ------------------------------------------------------------

	@Test
	void assertTimeoutPreemptivelyForSupplierThatCompletesBeforeTheTimeout() {
		changed.get().set(false);
		String result = assertTimeoutPreemptively(ofMillis(500), () -> {
			changed.get().set(true);
			return "Tempus Fugit";
		});
		assertFalse(changed.get().get(), "should have executed in a different thread");
		assertEquals("Tempus Fugit", result);
		assertEquals("Tempus Fugit", assertTimeoutPreemptively(ofMillis(500), () -> "Tempus Fugit", "message"));
		assertEquals("Tempus Fugit", assertTimeoutPreemptively(ofMillis(500), () -> "Tempus Fugit", () -> "message"));
	}

	@Test
	void assertTimeoutPreemptivelyForSupplierThatThrowsAnException() {
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			assertTimeoutPreemptively(ofMillis(500),
				() -> ExceptionUtils.throwAsUncheckedException(new RuntimeException("not this time")));
		});
		assertMessageEquals(exception, "not this time");
	}

	@Test
	void assertTimeoutPreemptivelyForSupplierThatThrowsAnAssertionFailedError() {
		AssertionFailedError exception = assertThrows(AssertionFailedError.class, () -> {
			assertTimeoutPreemptively(ofMillis(500), () -> {
				fail("enigma");
				return "Tempus Fugit";
			});
		});
		assertMessageEquals(exception, "enigma");
	}

	@Test
	void assertTimeoutPreemptivelyForSupplierThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> {
			assertTimeoutPreemptively(PREEMPTIVE_TIMEOUT, () -> {
				waitForInterrupt();
				return "Tempus Fugit";
			});
		});

		assertMessageEquals(error, "execution timed out after " + PREEMPTIVE_TIMEOUT.toMillis() + " ms");
		assertMessageStartsWith(error.getCause(), "Execution timed out in ");
		assertStackTraceContains(error.getCause(), "CountDownLatch", "await");
	}

	@Test
	void assertTimeoutPreemptivelyWithMessageForSupplierThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> {
			assertTimeoutPreemptively(PREEMPTIVE_TIMEOUT, () -> {
				waitForInterrupt();
				return "Tempus Fugit";
			}, "Tempus Fugit");
		});

		assertMessageEquals(error,
			"Tempus Fugit ==> execution timed out after " + PREEMPTIVE_TIMEOUT.toMillis() + " ms");
		assertMessageStartsWith(error.getCause(), "Execution timed out in ");
		assertStackTraceContains(error.getCause(), "CountDownLatch", "await");
	}

	@Test
	void assertTimeoutPreemptivelyWithMessageSupplierForSupplierThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> {
			assertTimeoutPreemptively(PREEMPTIVE_TIMEOUT, () -> {
				waitForInterrupt();
				return "Tempus Fugit";
			}, () -> "Tempus" + " " + "Fugit");
		});

		assertMessageEquals(error,
			"Tempus Fugit ==> execution timed out after " + PREEMPTIVE_TIMEOUT.toMillis() + " ms");
		assertMessageStartsWith(error.getCause(), "Execution timed out in ");
		assertStackTraceContains(error.getCause(), "CountDownLatch", "await");
	}

	@Test
	void assertTimeoutPreemptivelyUsesThreadsWithSpecificNamePrefix() {
		AtomicReference<String> threadName = new AtomicReference<>("");
		assertTimeoutPreemptively(ofMillis(1000), () -> threadName.set(Thread.currentThread().getName()));
		assertThat(threadName.get()) //
				.withFailMessage("Thread name does not match the expected prefix") //
				.startsWith("junit-timeout-thread-");
	}

	private void waitForInterrupt() {
		try {
			assertFalse(Thread.interrupted(), "Already interrupted");
			new CountDownLatch(1).await();
		}
		catch (InterruptedException ignore) {
			// ignore
		}
	}

	/**
	 * Assert the given stack trace elements contain an element with the given class name and method name.
	 */
	private static void assertStackTraceContains(@Nullable Throwable throwable, String className, String methodName) {
		assertThat(throwable).isNotNull();
		assertThat(throwable.getStackTrace()).anySatisfy(element -> {
			assertThat(element.getClassName()).endsWith(className);
			assertThat(element.getMethodName()).isEqualTo(methodName);
		});
	}
}
