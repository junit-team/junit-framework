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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.time.Duration;
import java.util.Arrays;

import example.util.StringUtils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ParameterizedClassDemo {

	@Nested
	// tag::first_example[]
	@ParameterizedClass
	@ValueSource(strings = { "racecar", "radar", "able was I ere I saw elba" })
	class PalindromeTests {

		@Parameter
		String candidate;

		@Test
		void palindrome() {
			assertTrue(StringUtils.isPalindrome(candidate));
		}

		@Test
		void reversePalindrome() {
			String reverseCandidate = new StringBuilder(candidate).reverse().toString();
			assertTrue(StringUtils.isPalindrome(reverseCandidate));
		}
	}
	// end::first_example[]

	@Nested
	class ConstructorInjection {
		@Nested
		// tag::constructor_injection[]
		@ParameterizedClass
		@CsvSource({ "apple, 23", "banana, 42" })
		class FruitTests {

			final String fruit;
			final int quantity;

			FruitTests(String fruit, int quantity) {
				this.fruit = fruit;
				this.quantity = quantity;
			}

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
		// end::constructor_injection[]
	}

	@Nested
	class FieldInjection {
		@Nested
		// tag::field_injection[]
		@ParameterizedClass
		@CsvSource({ "apple, 23", "banana, 42" })
		class FruitTests {

			@Parameter(0)
			String fruit;

			@Parameter(1)
			int quantity;

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
		// end::field_injection[]
	}

	@Nested
	// tag::nested[]
	@Execution(SAME_THREAD)
	@ParameterizedClass
	@ValueSource(strings = { "apple", "banana" })
	class FruitTests {

		@Parameter
		String fruit;

		@Nested
		@ParameterizedClass
		@ValueSource(ints = { 23, 42 })
		class QuantityTests {

			@Parameter
			int quantity;

			@ParameterizedTest
			@ValueSource(strings = { "PT1H", "PT2H" })
			void test(Duration duration) {
				assertFruit(fruit);
				assertQuantity(quantity);
				assertFalse(duration.isNegative());
			}
		}
	}
	// end::nested[]

	static void assertFruit(String fruit) {
		assertTrue(Arrays.asList("apple", "banana", "cherry", "dewberry").contains(fruit),
			() -> "not a fruit: " + fruit);
	}

	static void assertQuantity(int quantity) {
		assertTrue(quantity > 0);
	}
}
