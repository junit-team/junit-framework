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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import java.util.logging.Logger

@Execution(SAME_THREAD)
class RepeatedTestsDemo {
    private val logger = // ...
        // end::user_guide[]
        Logger.getLogger(RepeatedTestsDemo::class.java.name)
    // tag::user_guide[]

    @BeforeEach
    fun beforeEach(
        testInfo: TestInfo,
        repetitionInfo: RepetitionInfo
    ) {
        val currentRepetition = repetitionInfo.currentRepetition
        val totalRepetitions = repetitionInfo.totalRepetitions
        val methodName = testInfo.testMethod.get().name
        logger.info("About to execute repetition $currentRepetition of $totalRepetitions for $methodName")
    }

    @RepeatedTest(10)
    fun repeatedTest() {
        // ...
    }

    @RepeatedTest(5)
    fun repeatedTestWithRepetitionInfo(repetitionInfo: RepetitionInfo) {
        assertEquals(5, repetitionInfo.totalRepetitions)
    }

    // end::user_guide[]
    // Use fully qualified name to avoid having it show up in the imports.
    @org.junit.jupiter.api.Disabled("intentional failures would break the build")
    // tag::user_guide[]
    @RepeatedTest(value = 8, failureThreshold = 2)
    fun repeatedTestWithFailureThreshold(repetitionInfo: RepetitionInfo) {
        // Simulate unexpected failure every second repetition
        if (repetitionInfo.currentRepetition % 2 == 0) {
            fail("Boom!")
        }
    }

    @RepeatedTest(value = 1, name = "{displayName} {currentRepetition}/{totalRepetitions}")
    @DisplayName("Repeat!")
    fun customDisplayName(testInfo: TestInfo) {
        assertEquals("Repeat! 1/1", testInfo.displayName)
    }

    @RepeatedTest(value = 1, name = RepeatedTest.LONG_DISPLAY_NAME)
    @DisplayName("Details...")
    fun customDisplayNameWithLongPattern(testInfo: TestInfo) {
        assertEquals("Details... :: repetition 1 of 1", testInfo.displayName)
    }

    @RepeatedTest(value = 5, name = "Wiederholung {currentRepetition} von {totalRepetitions}")
    fun repeatedTestInGerman() {
        // ...
    }
}
// end::user_guide[]
