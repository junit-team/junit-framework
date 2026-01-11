/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example

// tag::user_guide[]

import example.domain.Person
import example.util.Calculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertTimeout
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

class KotlinAssertionsDemo {
    private val person = Person("Jane", "Doe")
    private val people = setOf(person, Person("John", "Doe"))

    @Test
    fun `exception absence testing`() {
        val calculator = Calculator()
        val result =
            assertDoesNotThrow("Should not throw an exception") {
                calculator.divide(0, 1)
            }
        assertEquals(0, result)
    }

    // end::user_guide[]
    @extensions.DisabledOnOpenJ9
    // tag::user_guide[]
    @Test
    fun `expected exception testing`() {
        val calculator = Calculator()
        val exception =
            assertThrows<ArithmeticException> ("Should throw an exception") {
                calculator.divide(1, 0)
            }
        assertEquals("/ by zero", exception.message)
    }

    @Test
    fun `grouped assertions`() {
        assertAll(
            "Person properties",
            { assertEquals("Jane", person.firstName) },
            { assertEquals("Doe", person.lastName) }
        )
    }

    @Test
    fun `grouped assertions from a stream`() {
        assertAll(
            "People with first name starting with J",
            people
                .stream()
                .map {
                    // This mapping returns Stream<() -> Unit>
                    { assertTrue(it.firstName.startsWith("J")) }
                }
        )
    }

    @Test
    fun `grouped assertions from a collection`() {
        assertAll(
            "People with last name of Doe",
            people.map { { assertEquals("Doe", it.lastName) } }
        )
    }

    // end::user_guide[]
    @Tag("timeout")
    // tag::user_guide[]
    @Test
    fun `timeout not exceeded testing`() {
        val fibonacciCalculator = FibonacciCalculator()
        val result =
            assertTimeout(Duration.ofMillis(1000)) {
                fibonacciCalculator.fib(14)
            }
        assertEquals(377, result)
    }

    // end::user_guide[]
    @Tag("timeout")
    @extensions.ExpectToFail
    // tag::user_guide[]
    @Test
    fun `timeout exceeded with preemptive termination`() {
        // The following assertion fails with an error message similar to:
        // execution timed out after 10 ms
        assertTimeoutPreemptively(Duration.ofMillis(10)) {
            // Simulate task that takes more than 10 ms.
            Thread.sleep(100)
        }
    }

    @Test
    fun `assertNotNull with a smart cast`() {
        val nullablePerson: Person? = person

        assertNotNull(nullablePerson)

        // The compiler smart casts nullablePerson to a non-nullable object.
        // The safe call operator (?.) isn't required.
        assertEquals(person.firstName, nullablePerson.firstName)
        assertEquals(person.lastName, nullablePerson.lastName)
    }

    @Test
    fun `assertInstanceOf with a smart cast`() {
        val maybePerson: Any = person

        assertInstanceOf<Person>(maybePerson)

        // The compiler smart casts maybePerson to a Person object,
        // allowing to access the Person properties.
        assertEquals(person.firstName, maybePerson.firstName)
        assertEquals(person.lastName, maybePerson.lastName)
    }
}
// end::user_guide[]
