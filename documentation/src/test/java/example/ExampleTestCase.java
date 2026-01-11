/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

// tag::user_guide[]

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import example.util.Calculator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class ExampleTestCase {

	private final Calculator calculator = new Calculator();

	@Test
	@Disabled("for demonstration purposes")
	@Order(1)
	void skippedTest() {
		// skipped ...
	}

	@Test
	@Order(2)
	void succeedingTest() {
		assertEquals(42, calculator.multiply(6, 7));
	}

	@Test
	@Order(3)
	void abortedTest() {
		assumeTrue("abc".contains("Z"), "abc does not contain Z");
		// aborted ...
	}

	@Test
	@Order(4)
	void failingTest() {
		// The following throws an ArithmeticException: "/ by zero"
		calculator.divide(1, 0);
	}

}
// end::user_guide[]
