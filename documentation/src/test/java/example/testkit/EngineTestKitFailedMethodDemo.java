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
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import example.ExampleTestCase;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

class EngineTestKitFailedMethodDemo {

	@Test
	void verifyJupiterMethodFailed() {
		EngineTestKit.engine("junit-jupiter") // <1>
			.selectors(selectClass(ExampleTestCase.class)) // <2>
			.execute() // <3>
			.testEvents() // <4>
			.assertThatEvents().haveExactly(1, // <5>
				event(test("failingTest"),
					finishedWithFailure(
						instanceOf(ArithmeticException.class), message(it -> it.endsWith("by zero")))));
	}

}
// end::user_guide[]
// @formatter:on
