/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.testkit;

// @formatter:off
// tag::user_guide[]

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.io.StringWriter;
import java.io.Writer;

import example.ExampleTestCase;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.opentest4j.TestAbortedException;

class EngineTestKitAllEventsDemo {

	@Test
	void verifyAllJupiterEvents() {
		Writer writer = // create a java.io.Writer for debug output
		// end::user_guide[]
				// For the demo, we are swallowing the debug output.
				new StringWriter();
		// tag::user_guide[]

		EngineTestKit.engine("junit-jupiter") // <1>
			.selectors(selectClass(ExampleTestCase.class)) // <2>
			.execute() // <3>
			.allEvents() // <4>
			.debug(writer) // <5>
			.assertEventsMatchExactly( // <6>
				event(engine(), started()),
				event(container(ExampleTestCase.class), started()),
				event(test("skippedTest"), skippedWithReason("for demonstration purposes")),
				event(test("succeedingTest"), started()),
				event(test("succeedingTest"), finishedSuccessfully()),
				event(test("abortedTest"), started()),
				event(test("abortedTest"),
					abortedWithReason(instanceOf(TestAbortedException.class),
						message(m -> m.contains("abc does not contain Z")))),
				event(test("failingTest"), started()),
				event(test("failingTest"), finishedWithFailure(
					instanceOf(ArithmeticException.class), message(it -> it.endsWith("by zero")))),
				event(container(ExampleTestCase.class), finishedSuccessfully()),
				event(engine(), finishedSuccessfully()));
	}

}
// end::user_guide[]
// @formatter:on
