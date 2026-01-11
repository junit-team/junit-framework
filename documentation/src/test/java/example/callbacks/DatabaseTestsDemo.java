/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.callbacks;

// tag::user_guide[]

import static example.callbacks.Logger.afterEachMethod;
import static example.callbacks.Logger.beforeAllMethod;
import static example.callbacks.Logger.beforeEachMethod;
import static example.callbacks.Logger.testMethod;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Extension of {@link AbstractDatabaseTests} that inserts test data
 * into the database (after the database connection has been opened)
 * and deletes test data (before the database connection is closed).
 */
@ExtendWith({ Extension1.class, Extension2.class })
class DatabaseTestsDemo extends AbstractDatabaseTests {

	@BeforeAll
	static void beforeAll() {
		beforeAllMethod(DatabaseTestsDemo.class.getSimpleName() + ".beforeAll()");
	}

	@BeforeEach
	void insertTestDataIntoDatabase() {
		beforeEachMethod(getClass().getSimpleName() + ".insertTestDataIntoDatabase()");
	}

	@Test
	void testDatabaseFunctionality() {
		testMethod(getClass().getSimpleName() + ".testDatabaseFunctionality()");
	}

	@AfterEach
	void deleteTestDataFromDatabase() {
		afterEachMethod(getClass().getSimpleName() + ".deleteTestDataFromDatabase()");
	}

	@AfterAll
	static void afterAll() {
		beforeAllMethod(DatabaseTestsDemo.class.getSimpleName() + ".afterAll()");
	}

}
// end::user_guide[]
