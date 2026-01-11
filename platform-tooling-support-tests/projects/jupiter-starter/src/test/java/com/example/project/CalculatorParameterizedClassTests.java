/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package com.example.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ParameterizedClass
@ValueSource(ints = { 1, 2 })
class CalculatorParameterizedClassTests {

	@Parameter
	int i;

	@ParameterizedTest
	@ValueSource(ints = { 1, 2 })
	void parameterizedTest(int j) {
		Calculator calculator = new Calculator();
		assertEquals(i + j, calculator.add(i, j));
	}

	@Nested
	@ParameterizedClass
	@ValueSource(ints = { 1, 2 })
	class Inner {

		final int j;

		Inner(int j) {
			this.j = j;
		}

		@Test
		void regularTest() {
			Calculator calculator = new Calculator();
			assertEquals(i + j, calculator.add(i, j));
		}
	}
}
