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
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for {@link AssertTimeout}.
 *
 * @since 5.0
 */
class AssertTimeoutAssertionsTests {

	private static final ThreadLocal<AtomicBoolean> changed = ThreadLocal.withInitial(() -> new AtomicBoolean(false));

	private final Executable nix = () -> {
	};

	// --- executable ----------------------------------------------------------

	@Test
	void assertTimeoutForExecutableThatCompletesBeforeTheTimeout() {
		changed.get().set(false);
		assertTimeout(ofMillis(500), () -> changed.get().set(true));
		assertTrue(changed.get().get(), "should have executed in the same thread");
		assertTimeout(ofMillis(500), nix, "message");
		assertTimeout(ofMillis(500), nix, () -> "message");
	}

	@Test
	void assertTimeoutForExecutableThatThrowsAnException() {
		RuntimeException exception = assertThrows(RuntimeException.class, () -> assertTimeout(ofMillis(500), () -> {
			throw new RuntimeException("not this time");
		}));
		assertMessageEquals(exception, "not this time");
	}

	@Test
	void assertTimeoutForExecutableThatThrowsAnAssertionFailedError() {
		AssertionFailedError exception = assertThrows(AssertionFailedError.class,
			() -> assertTimeout(ofMillis(500), () -> fail("enigma")));
		assertMessageEquals(exception, "enigma");
	}

	@Test
	void assertTimeoutForExecutableThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class,
			() -> assertTimeout(ofMillis(10), this::nap));
		assertMessageStartsWith(error, "execution exceeded timeout of 10 ms by");
	}

	@Test
	void assertTimeoutWithMessageForExecutableThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class,
			() -> assertTimeout(ofMillis(10), this::nap, "Tempus Fugit"));
		assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by");
	}

	@Test
	void assertTimeoutWithMessageSupplierForExecutableThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class,
			() -> assertTimeout(ofMillis(10), this::nap, () -> "Tempus" + " " + "Fugit"));
		assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by");
	}

	// --- supplier ------------------------------------------------------------

	@Test
	void assertTimeoutForSupplierThatCompletesBeforeTheTimeout() {
		changed.get().set(false);
		String result = assertTimeout(ofMillis(500), () -> {
			changed.get().set(true);
			return "Tempus Fugit";
		});
		assertTrue(changed.get().get(), "should have executed in the same thread");
		assertEquals("Tempus Fugit", result);
		assertEquals("Tempus Fugit", assertTimeout(ofMillis(500), () -> "Tempus Fugit", "message"));
		assertEquals("Tempus Fugit", assertTimeout(ofMillis(500), () -> "Tempus Fugit", () -> "message"));
	}

	@Test
	void assertTimeoutForSupplierThatThrowsAnException() {
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			assertTimeout(ofMillis(500),
				() -> ExceptionUtils.throwAsUncheckedException(new RuntimeException("not this time")));
		});
		assertMessageEquals(exception, "not this time");
	}

	@Test
	void assertTimeoutForSupplierThatThrowsAnAssertionFailedError() {
		AssertionFailedError exception = assertThrows(AssertionFailedError.class, () -> {
			assertTimeout(ofMillis(500), () -> fail("enigma"));
		});
		assertMessageEquals(exception, "enigma");
	}

	@Test
	void assertTimeoutForSupplierThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> {
			assertTimeout(ofMillis(10), () -> {
				nap();
				return "Tempus Fugit";
			});
		});
		assertMessageStartsWith(error, "execution exceeded timeout of 10 ms by");
	}

	@Test
	void assertTimeoutWithMessageForSupplierThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> {
			assertTimeout(ofMillis(10), () -> {
				nap();
				return "Tempus Fugit";
			}, "Tempus Fugit");
		});
		assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by");
	}

	@Test
	void assertTimeoutWithMessageSupplierForSupplierThatCompletesAfterTheTimeout() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> {
			assertTimeout(ofMillis(10), () -> {
				nap();
				return "Tempus Fugit";
			}, () -> "Tempus" + " " + "Fugit");
		});
		assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by");
	}

	/**
	 * Take a nap for 100 milliseconds.
	 */
	private void nap() throws InterruptedException {
		long start = System.nanoTime();
		// workaround for imprecise clocks (yes, Windows, I'm talking about you)
		do {
			Thread.sleep(100);
		} while (System.nanoTime() - start < 100_000_000L);
	}

}
