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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.CsvSource;

public class ParameterizedRecordDemo {

	// tag::example[]
	@ParameterizedClass
	@CsvSource({ "apple, 23", "banana, 42" })
	record FruitTests(String fruit, int quantity) {

		@Test
		void test() {
			assertFruit(fruit);
			assertQuantity(quantity);
		}

		@Test
		void anotherTest() {
			// ...
		}
	}
	// end::example[]

	static void assertFruit(String fruit) {
		assertTrue(Arrays.asList("apple", "banana", "cherry", "dewberry").contains(fruit));
	}

	static void assertQuantity(int quantity) {
		assertTrue(quantity >= 0);
	}
}
