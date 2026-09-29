/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.testkit

// tag::user_guide[]
import example.kotlin.ExampleTestCase
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit
import org.junit.platform.testkit.engine.EventConditions.event
import org.junit.platform.testkit.engine.EventConditions.finishedWithFailure
import org.junit.platform.testkit.engine.EventConditions.test
import org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf
import org.junit.platform.testkit.engine.TestExecutionResultConditions.message

class EngineTestKitFailedMethodDemo {
    @Test
    fun verifyJupiterMethodFailed() {
        EngineTestKit
            .engine("junit-jupiter") // <1>
            .selectors(selectClass(ExampleTestCase::class.java)) // <2>
            .execute() // <3>
            .testEvents() // <4>
            .assertThatEvents()
            .haveExactly(
                1, // <5>
                event(
                    test("failingTest"),
                    finishedWithFailure(
                        instanceOf(ArithmeticException::class.java),
                        message { it.endsWith("by zero") }
                    )
                )
            )
    }
}
// end::user_guide[]
