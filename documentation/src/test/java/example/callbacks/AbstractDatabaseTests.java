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

import static example.callbacks.Logger.afterAllMethod;
import static example.callbacks.Logger.afterEachMethod;
import static example.callbacks.Logger.beforeAllMethod;
import static example.callbacks.Logger.beforeEachMethod;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Abstract base class for tests that use the database.
 */
abstract class AbstractDatabaseTests {

	@BeforeAll
	static void createDatabase() {
		beforeAllMethod(AbstractDatabaseTests.class.getSimpleName() + ".createDatabase()");
	}

	@BeforeEach
	void connectToDatabase() {
		beforeEachMethod(AbstractDatabaseTests.class.getSimpleName() + ".connectToDatabase()");
	}

	@AfterEach
	void disconnectFromDatabase() {
		afterEachMethod(AbstractDatabaseTests.class.getSimpleName() + ".disconnectFromDatabase()");
	}

	@AfterAll
	static void destroyDatabase() {
		afterAllMethod(AbstractDatabaseTests.class.getSimpleName() + ".destroyDatabase()");
	}

}
// end::user_guide[]
