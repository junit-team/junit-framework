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

import example.ExampleTestCase;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

class EngineTestKitStatisticsDemo {

	@Test
	void verifyJupiterContainerStats() {
		EngineTestKit
			.engine("junit-jupiter") // <1>
			.selectors(selectClass(ExampleTestCase.class)) // <2>
			.execute() // <3>
			.containerEvents() // <4>
			.assertStatistics(stats -> stats.started(2).succeeded(2)); // <5>
	}

	@Test
	void verifyJupiterTestStats() {
		EngineTestKit
			.engine("junit-jupiter") // <1>
			.selectors(selectClass(ExampleTestCase.class)) // <2>
			.execute() // <3>
			.testEvents() // <6>
			.assertStatistics(stats ->
				stats.skipped(1).started(3).succeeded(1).aborted(1).failed(1)); // <7>
	}

}
// end::user_guide[]
// @formatter:on
