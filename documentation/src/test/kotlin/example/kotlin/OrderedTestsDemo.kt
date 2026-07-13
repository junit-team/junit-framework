/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

// tag::user_guide[]
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(OrderAnnotation::class)
class OrderedTestsDemo {
    @Test
    @Order(1)
    fun nullValues() {
        // perform assertions against null values
    }

    @Test
    @Order(2)
    fun emptyValues() {
        // perform assertions against empty values
    }

    @Test
    @Order(3)
    fun validValues() {
        // perform assertions against valid values
    }
}
// end::user_guide[]
