/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExceptionAssertionDemo {

	// @formatter:off
	// tag::user_guide[]
	@Test
	void testExpectedExceptionIsThrown() {
		// The following assertion succeeds because the code under assertion
		// throws the expected IllegalArgumentException.
		// The assertion also returns the thrown exception which can be used for
		// further assertions like asserting the exception message.
		IllegalArgumentException exception =
			assertThrows(IllegalArgumentException.class, () -> {
				throw new IllegalArgumentException("expected message");
			});
		assertEquals("expected message", exception.getMessage());

		// The following assertion also succeeds because the code under assertion
		// throws IllegalArgumentException which is a subclass of RuntimeException.
		assertThrows(RuntimeException.class, () -> {
			throw new IllegalArgumentException("expected message");
		});
	}
	// end::user_guide[]
	// @formatter:on

}
