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

import example.kotlin.extensions.Database
import example.kotlin.extensions.DatabaseExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith

// tag::user_guide[]
@ExtendWith(DatabaseExtension::class)
@TestInstance(Lifecycle.PER_CLASS) // also works with the default PER_METHOD lifecycle
class DatabaseDemo(
    private val database: Database
) {
    private val greeting = checkNotNull(database.find("greeting")) // <1>

    @Test
    fun dataIsAvailable() {
        assertEquals("Hello", greeting)
        assertEquals("DatabaseDemo", database.find("testClass")) // <2>
    }
}
// end::user_guide[]
