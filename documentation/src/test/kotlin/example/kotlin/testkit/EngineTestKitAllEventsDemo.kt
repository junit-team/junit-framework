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
import org.junit.platform.testkit.engine.EventConditions.abortedWithReason
import org.junit.platform.testkit.engine.EventConditions.container
import org.junit.platform.testkit.engine.EventConditions.engine
import org.junit.platform.testkit.engine.EventConditions.event
import org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully
import org.junit.platform.testkit.engine.EventConditions.finishedWithFailure
import org.junit.platform.testkit.engine.EventConditions.skippedWithReason
import org.junit.platform.testkit.engine.EventConditions.started
import org.junit.platform.testkit.engine.EventConditions.test
import org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf
import org.junit.platform.testkit.engine.TestExecutionResultConditions.message
import org.opentest4j.TestAbortedException
import java.io.StringWriter
import java.io.Writer

class EngineTestKitAllEventsDemo {
    @Test
    fun verifyAllJupiterEvents() {
        val writer: Writer = // create a java.io.Writer for debug output
            // end::user_guide[]
            // For the demo, we are swallowing the debug output.
            StringWriter()
        // tag::user_guide[]

        EngineTestKit
            .engine("junit-jupiter") // <1>
            .selectors(selectClass(ExampleTestCase::class.java)) // <2>
            .execute() // <3>
            .allEvents() // <4>
            .debug(writer) // <5>
            .assertEventsMatchExactly( // <6>
                event(engine(), started()),
                event(container(ExampleTestCase::class.java), started()),
                event(test("skippedTest"), skippedWithReason("for demonstration purposes")),
                event(test("succeedingTest"), started()),
                event(test("succeedingTest"), finishedSuccessfully()),
                event(test("abortedTest"), started()),
                event(
                    test("abortedTest"),
                    abortedWithReason(
                        instanceOf(TestAbortedException::class.java),
                        message { it.contains("abc does not contain Z") }
                    )
                ),
                event(test("failingTest"), started()),
                event(
                    test("failingTest"),
                    finishedWithFailure(
                        instanceOf(ArithmeticException::class.java),
                        message { it.endsWith("by zero") }
                    )
                ),
                event(container(ExampleTestCase::class.java), finishedSuccessfully()),
                event(engine(), finishedSuccessfully())
            )
    }
}
// end::user_guide[]
