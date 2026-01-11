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
import static example.callbacks.Logger.beforeEachMethod;
import static example.callbacks.Logger.testMethod;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Example of "broken" lifecycle method configuration.
 *
 * <p>Test data is inserted before the database connection has been opened.
 *
 * <p>Database connection is closed before deleting test data.
 */
@ExtendWith({ Extension1.class, Extension2.class })
class BrokenLifecycleMethodConfigDemo {

	@BeforeEach
	void connectToDatabase() {
		beforeEachMethod(getClass().getSimpleName() + ".connectToDatabase()");
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

	@AfterEach
	void disconnectFromDatabase() {
		afterEachMethod(getClass().getSimpleName() + ".disconnectFromDatabase()");
	}

}
// end::user_guide[]
