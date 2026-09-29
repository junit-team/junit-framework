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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Example of "broken" lifecycle method configuration.
 *
 * Test data is inserted before the database connection has been opened.
 *
 * Database connection is closed before deleting test data.
 */
@ExtendWith(Extension1::class, Extension2::class)
class BrokenLifecycleMethodConfigDemo {
    @BeforeEach
    fun connectToDatabase() {
        beforeEachMethod("${javaClass.simpleName}.connectToDatabase()")
    }

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

    @AfterEach
    fun disconnectFromDatabase() {
        afterEachMethod("${javaClass.simpleName}.disconnectFromDatabase()")
    }
}
// end::user_guide[]
