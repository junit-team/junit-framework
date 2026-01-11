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

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.test;

import example.ExampleTestCase;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

class EngineTestKitSkippedMethodDemo {

	@Test
	void verifyJupiterMethodWasSkipped() {
		String methodName = "skippedTest";

		Events testEvents = EngineTestKit // <5>
			.engine("junit-jupiter") // <1>
			.selectors(selectMethod(ExampleTestCase.class, methodName)) // <2>
			.execute() // <3>
			.testEvents(); // <4>

		testEvents.assertStatistics(stats -> stats.skipped(1)); // <6>

		testEvents.assertThatEvents() // <7>
			.haveExactly(1, event(test(methodName),
				skippedWithReason("for demonstration purposes")));
	}

}
// end::user_guide[]
// @formatter:on
