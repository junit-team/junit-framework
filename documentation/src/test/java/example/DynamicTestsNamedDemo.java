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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Named.named;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.NamedExecutable;
import org.junit.jupiter.api.TestFactory;

public class DynamicTestsNamedDemo {

	@TestFactory
	Stream<DynamicTest> dynamicTestsFromStreamFactoryMethodWithNames() {
		// Stream of palindromes to check
		// end::user_guide[]
		// @formatter:off
		// tag::user_guide[]
		var inputStream = Stream.of(
			named("racecar is a palindrome", "racecar"),
			named("radar is also a palindrome", "radar"),
			named("mom also seems to be a palindrome", "mom"),
			named("dad is yet another palindrome", "dad")
		);
		// end::user_guide[]
		// @formatter:on
		// tag::user_guide[]

		// Returns a stream of dynamic tests.
		return DynamicTest.stream(inputStream, text -> assertTrue(isPalindrome(text)));
	}

	@TestFactory
	Stream<DynamicTest> dynamicTestsFromStreamFactoryMethodWithNamedExecutables() {
		// Stream of palindromes to check
		// end::user_guide[]
		// @formatter:off
		// tag::user_guide[]
		var inputStream = Stream.of("racecar", "radar", "mom", "dad")
				.map(PalindromeNamedExecutable::new);
		// end::user_guide[]
		// @formatter:on
		// tag::user_guide[]

		// Returns a stream of dynamic tests based on NamedExecutables.
		return DynamicTest.stream(inputStream);
	}

	record PalindromeNamedExecutable(String text) implements NamedExecutable {

		@Override
		public String getName() {
			return "'%s' is a palindrome".formatted(text);
		}

		@Override
		public void execute() {
			assertTrue(isPalindrome(text));
		}
	}
}
// end::user_guide[]
