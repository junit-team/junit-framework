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

// tag::user_guide[]
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import example.ExampleTestCase;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineDiscoveryResults;
import org.junit.platform.testkit.engine.EngineTestKit;

class EngineTestKitDiscoveryDemo {

	@Test
	void verifyJupiterDiscovery() {
		EngineDiscoveryResults results = EngineTestKit.engine("junit-jupiter") // <1>
				.selectors(selectClass(ExampleTestCase.class)) // <2>
				.discover(); // <3>

		assertEquals("JUnit Jupiter", results.getEngineDescriptor().getDisplayName()); // <4>
		assertEquals(emptyList(), results.getDiscoveryIssues()); // <5>
	}

}
// end::user_guide[]
