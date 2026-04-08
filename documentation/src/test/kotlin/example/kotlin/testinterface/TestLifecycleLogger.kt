/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.testinterface

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.util.logging.Logger

// tag::user_guide[]
@TestInstance(Lifecycle.PER_CLASS)
interface TestLifecycleLogger {
    @BeforeAll
    fun beforeAllTests() {
        logger.info("Before all tests")
    }

    @AfterAll
    fun afterAllTests() {
        logger.info("After all tests")
    }

    @BeforeEach
    fun beforeEachTest(testInfo: TestInfo) {
        logger.info { "About to execute [${testInfo.displayName}]" }
    }

    @AfterEach
    fun afterEachTest(testInfo: TestInfo) {
        logger.info { "Finished executing [${testInfo.displayName}]" }
    }

    companion object {
        private val logger: Logger = Logger.getLogger(TestLifecycleLogger::class.java.name)
    }
}
// end::user_guide[]
