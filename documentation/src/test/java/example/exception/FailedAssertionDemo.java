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

import example.util.Calculator;

import extensions.ExpectToFail;

import org.junit.jupiter.api.Test;

class FailedAssertionDemo {

	// tag::user_guide[]
	private final Calculator calculator = new Calculator();

	// end::user_guide[]

	@ExpectToFail
	// tag::user_guide[]
	@Test
	void failsDueToUncaughtAssertionError() {
		// The following incorrect assertion will cause a test failure.
		// The expected value should be 2 instead of 99.
		assertEquals(99, calculator.add(1, 1));
	}
	// end::user_guide[]

}
