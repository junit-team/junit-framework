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
import static example.util.StringUtils.isPalindrome;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import example.util.Calculator;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.parallel.Execution;

// end::user_guide[]
// @formatter:off
// tag::user_guide[]
class DynamicTestsDemo {

	private final Calculator calculator = new Calculator();

	// This method will not be executed but produce a warning
	@TestFactory
	// end::user_guide[]
	@Tag("exclude")
	DynamicTest dummy() { return dynamicTest("dummy", () -> {}); }
	// tag::user_guide[]
	List<String> dynamicTestsWithInvalidReturnType() {
		return Arrays.asList("Hello");
	}

	@TestFactory
	Collection<DynamicTest> dynamicTestsFromCollection() {
		return Arrays.asList(
			dynamicTest("1st dynamic test", () -> assertTrue(isPalindrome("madam"))),
			dynamicTest("2nd dynamic test", () -> assertEquals(4, calculator.multiply(2, 2)))
		);
	}

	@TestFactory
	Iterable<DynamicTest> dynamicTestsFromIterable() {
		return Arrays.asList(
			dynamicTest("3rd dynamic test", () -> assertTrue(isPalindrome("madam"))),
			dynamicTest("4th dynamic test", () -> assertEquals(4, calculator.multiply(2, 2)))
		);
	}

	@TestFactory
	Iterator<DynamicTest> dynamicTestsFromIterator() {
		return Arrays.asList(
			dynamicTest("5th dynamic test", () -> assertTrue(isPalindrome("madam"))),
			dynamicTest("6th dynamic test", () -> assertEquals(4, calculator.multiply(2, 2)))
		).iterator();
	}

	@TestFactory
	DynamicTest[] dynamicTestsFromArray() {
		return new DynamicTest[] {
			dynamicTest("7th dynamic test", () -> assertTrue(isPalindrome("madam"))),
			dynamicTest("8th dynamic test", () -> assertEquals(4, calculator.multiply(2, 2)))
		};
	}

	@TestFactory
	Stream<DynamicTest> dynamicTestsFromStream() {
		return Stream.of("racecar", "radar", "mom", "dad")
			.map(text -> dynamicTest(text, () -> assertTrue(isPalindrome(text))));
	}

	@TestFactory
	Stream<DynamicTest> dynamicTestsFromIntStream() {
		// Generates tests for the first 10 even integers.
		return IntStream.iterate(0, n -> n + 2).limit(10)
			.mapToObj(n -> dynamicTest("test" + n, () -> assertEquals(0, n % 2)));
	}

	@TestFactory
	Stream<DynamicTest> generateRandomNumberOfTests() {

		// Generates random positive integers between 0 and 100 until
		// a number evenly divisible by 7 is encountered.
		Iterator<Integer> inputGenerator = new Iterator<>() {

			Random random = new Random();
			// end::user_guide[]
			{
				// Use fixed seed to always produce the same number of tests for execution on the CI server
				random = new Random(23);
			}
			// tag::user_guide[]
			int current;

			@Override
			public boolean hasNext() {
				current = random.nextInt(100);
				return current % 7 != 0;
			}

			@Override
			public Integer next() {
				return current;
			}
		};

		// Generates display names like: input:5, input:37, input:85, etc.
		Function<Integer, String> displayNameGenerator = (input) -> "input:" + input;

		// Executes tests based on the current input value.
		ThrowingConsumer<Integer> testExecutor = (input) -> assertTrue(input % 7 != 0);

		// Returns a stream of dynamic tests.
		return DynamicTest.stream(inputGenerator, displayNameGenerator, testExecutor);
	}

	@TestFactory
	Stream<DynamicTest> dynamicTestsFromStreamFactoryMethod() {
		// Stream of palindromes to check
		Stream<String> inputStream = Stream.of("racecar", "radar", "mom", "dad");

		// Generates display names like: racecar is a palindrome
		Function<String, String> displayNameGenerator = text -> text + " is a palindrome";

		// Executes tests based on the current input value.
		ThrowingConsumer<String> testExecutor = text -> assertTrue(isPalindrome(text));

		// Returns a stream of dynamic tests.
		return DynamicTest.stream(inputStream, displayNameGenerator, testExecutor);
	}

	@TestFactory
	Stream<DynamicNode> dynamicTestsWithContainers() {
		return Stream.of("A", "B", "C")
			.map(input -> dynamicContainer("Container " + input, Stream.of(
				dynamicTest("not null", () -> assertNotNull(input)),
				dynamicContainer("properties", Stream.of(
					dynamicTest("length > 0", () -> assertTrue(input.length() > 0)),
					dynamicTest("not empty", () -> assertFalse(input.isEmpty()))
				))
			)));
	}

	// end::user_guide[]
	// tag::execution_mode[]
	@TestFactory
	@Execution(CONCURRENT) // <1>
	Stream<DynamicNode> dynamicTestsWithConfiguredExecutionMode() {
		return Stream.of("A", "B", "C")
				.map(input ->
					dynamicContainer(outer -> outer
						.displayName("Container " + input)
						.children(
							dynamicTest(config -> config
								.displayName("not null")
								.executionMode(SAME_THREAD) // <2>
								.executable(() -> assertNotNull(input))
							),
							dynamicContainer(inner -> inner
								.displayName("properties")
								.executionMode(CONCURRENT) // <3>
								.childExecutionMode(SAME_THREAD) // <4>
								.children(
									dynamicTest(config -> config
										.displayName("length > 0")
										.executionMode(CONCURRENT) // <5>
										.executable(() -> assertTrue(input.length() > 0))
									),
									dynamicTest(config -> config
										.displayName("not empty")
										.executable(() -> assertFalse(input.isEmpty()))
									)
								)
							)
						)
					)
				);
	}
	// end::execution_mode[]

	// tag::user_guide[]
	@TestFactory
	DynamicNode dynamicNodeSingleTest() {
		return dynamicTest("'pop' is a palindrome", () -> assertTrue(isPalindrome("pop")));
	}

	@TestFactory
	DynamicNode dynamicNodeSingleContainer() {
		return dynamicContainer("palindromes",
			Stream.of("racecar", "radar", "mom", "dad")
				.map(text -> dynamicTest(text, () -> assertTrue(isPalindrome(text)))
		));
	}
}
// end::user_guide[]
