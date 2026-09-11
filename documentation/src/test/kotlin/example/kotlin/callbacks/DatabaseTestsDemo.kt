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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Extension of [AbstractDatabaseTests] that inserts test data
 * into the database (after the database connection has been opened)
 * and deletes test data (before the database connection is closed).
 */
@ExtendWith(Extension1::class, Extension2::class)
class DatabaseTestsDemo : AbstractDatabaseTests() {
    @BeforeEach
    fun insertTestDataIntoDatabase() {
        beforeEachMethod("${javaClass.simpleName}.insertTestDataIntoDatabase()")
    }

    @Test
    fun testDatabaseFunctionality() {
        testMethod("${javaClass.simpleName}.testDatabaseFunctionality()")
    }

    @AfterEach
    fun deleteTestDataFromDatabase() {
        afterEachMethod("${javaClass.simpleName}.deleteTestDataFromDatabase()")
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            beforeAllMethod("${DatabaseTestsDemo::class.simpleName}.beforeAll()")
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            afterAllMethod("${DatabaseTestsDemo::class.simpleName}.afterAll()")
        }
    }
}
// end::user_guide[]
