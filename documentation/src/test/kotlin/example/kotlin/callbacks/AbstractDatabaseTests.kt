/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.callbacks

// tag::user_guide[]

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

/**
 * Abstract base class for tests that use the database.
 */
abstract class AbstractDatabaseTests {
    @BeforeEach
    fun connectToDatabase() {
        beforeEachMethod("${AbstractDatabaseTests::class.simpleName}.connectToDatabase()")
    }

    @AfterEach
    fun disconnectFromDatabase() {
        afterEachMethod("${AbstractDatabaseTests::class.simpleName}.disconnectFromDatabase()")
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun createDatabase() {
            beforeAllMethod("${AbstractDatabaseTests::class.simpleName}.createDatabase()")
        }

        @JvmStatic
        @AfterAll
        fun destroyDatabase() {
            afterAllMethod("${AbstractDatabaseTests::class.simpleName}.destroyDatabase()")
        }
    }
}
// end::user_guide[]
