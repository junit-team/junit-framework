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

import example.util.Calculator;

import extensions.ExpectToFail;

import org.junit.jupiter.api.Test;

class UncaughtExceptionHandlingDemo {

	// tag::user_guide[]
	private final Calculator calculator = new Calculator();

	// end::user_guide[]

	@ExpectToFail
	// tag::user_guide[]
	@Test
	void failsDueToUncaughtException() {
		// The following throws an ArithmeticException due to division by
		// zero, which causes a test failure.
		calculator.divide(1, 0);
	}
	// end::user_guide[]

}
