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

// @formatter:off
// tag::user_guide[]
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;

import example.domain.Person;
import example.util.Calculator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class AssertionsDemo {

	private final Calculator calculator = new Calculator();

	private final Person person = new Person("Jane", "Doe");

	@Test
	void standardAssertions() {
		assertEquals(2, calculator.add(1, 1));
		assertEquals(4, calculator.multiply(2, 2),
				"The optional failure message is now the last parameter");

		// Lazily evaluates generateFailureMessage('a','b').
		assertTrue('a' < 'b', () -> generateFailureMessage('a','b'));
	}

	@Test
	void groupedAssertions() {
		// In a grouped assertion all assertions are executed, and all
		// failures will be reported together.
		assertAll("person",
			() -> assertEquals("Jane", person.getFirstName()),
			() -> assertEquals("Doe", person.getLastName())
		);
	}

	@Test
	void dependentAssertions() {
		// Within a code block, if an assertion fails the
		// subsequent code in the same block will be skipped.
		assertAll("properties",
			() -> {
				String firstName = person.getFirstName();
				assertNotNull(firstName);

				// Executed only if the previous assertion is valid.
				assertAll("first name",
					() -> assertTrue(firstName.startsWith("J")),
					() -> assertTrue(firstName.endsWith("e"))
				);
			},
			() -> {
				// Grouped assertion, so processed independently
				// of results of first name assertions.
				String lastName = person.getLastName();
				assertNotNull(lastName);

				// Executed only if the previous assertion is valid.
				assertAll("last name",
					() -> assertTrue(lastName.startsWith("D")),
					() -> assertTrue(lastName.endsWith("e"))
				);
			}
		);
	}

	// end::user_guide[]
	@extensions.DisabledOnOpenJ9
	// tag::user_guide[]
	@Test
	void exceptionTesting() {
		Exception exception = assertThrows(ArithmeticException.class, () ->
			calculator.divide(1, 0));
		assertEquals("/ by zero", exception.getMessage());
	}

	// end::user_guide[]
	@Tag("timeout")
	// tag::user_guide[]
	@Test
	void timeoutNotExceeded() {
		// The following assertion succeeds.
		assertTimeout(ofMinutes(2), () -> {
			// Perform task that takes less than 2 minutes.
		});
	}

	// end::user_guide[]
	@Tag("timeout")
	// tag::user_guide[]
	@Test
	void timeoutNotExceededWithResult() {
		// The following assertion succeeds, and returns the supplied object.
		String actualResult = assertTimeout(ofMinutes(2), () -> {
			return "a result";
		});
		assertEquals("a result", actualResult);
	}

	// end::user_guide[]
	@Tag("timeout")
	// tag::user_guide[]
	@Test
	void timeoutNotExceededWithMethod() {
		// The following assertion invokes a method reference and returns an object.
		String actualGreeting = assertTimeout(ofMinutes(2), AssertionsDemo::greeting);
		assertEquals("Hello, World!", actualGreeting);
	}

	// end::user_guide[]
	@Tag("timeout")
	@extensions.ExpectToFail
	// tag::user_guide[]
	@Test
	void timeoutExceeded() {
		// The following assertion fails with an error message similar to:
		// execution exceeded timeout of 10 ms by 91 ms
		assertTimeout(ofMillis(10), () -> {
			// Simulate task that takes more than 10 ms.
			Thread.sleep(100);
		});
	}

	// end::user_guide[]
	@Tag("timeout")
	@extensions.ExpectToFail
	// tag::user_guide[]
	@Test
	void timeoutExceededWithPreemptiveTermination() {
		// The following assertion fails with an error message similar to:
		// execution timed out after 10 ms
		assertTimeoutPreemptively(ofMillis(10), () -> {
			// Simulate task that takes more than 10 ms.
			new CountDownLatch(1).await();
		});
	}

	private static String greeting() {
		return "Hello, World!";
	}

	private static String generateFailureMessage(char a, char b) {
		return "Assertion messages can be lazily evaluated -- "
				+ "to avoid constructing complex messages unnecessarily." + (a < b);
	}
}
// end::user_guide[]
// @formatter:on
