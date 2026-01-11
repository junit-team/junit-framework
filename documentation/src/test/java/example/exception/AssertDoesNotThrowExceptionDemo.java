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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class AssertDoesNotThrowExceptionDemo {

	// tag::user_guide[]
	@Test
	void testExceptionIsNotThrown() {
		assertDoesNotThrow(() -> {
			shouldNotThrowException();
		});
	}

	void shouldNotThrowException() {
	}
	// end::user_guide[]

}
