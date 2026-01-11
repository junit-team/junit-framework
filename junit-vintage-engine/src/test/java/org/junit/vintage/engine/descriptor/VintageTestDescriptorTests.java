/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.UniqueId;
import org.junit.runner.Description;
import org.junit.vintage.engine.samples.junit4.ConcreteJUnit4TestCase;

class VintageTestDescriptorTests {

	private static final UniqueId uniqueId = UniqueId.forEngine("vintage");

	@Test
	void legacyReportingNameUsesClassName() {
		var description = Description.createSuiteDescription(ConcreteJUnit4TestCase.class);
		var testDescriptor = new VintageTestDescriptor(uniqueId, description, null);

		assertEquals("org.junit.vintage.engine.samples.junit4.ConcreteJUnit4TestCase",
			testDescriptor.getLegacyReportingName());
	}

	@Test
	void legacyReportingNameUsesMethodName() {
		var description = Description.createTestDescription(ConcreteJUnit4TestCase.class, "legacyTest");
		var testDescriptor = new VintageTestDescriptor(uniqueId, description, null);

		assertEquals("legacyTest", testDescriptor.getLegacyReportingName());
	}

	@Test
	void legacyReportingNameFallbackToDisplayName() {
		var suiteName = "Legacy Suite";
		var description = Description.createSuiteDescription(suiteName);
		var testDescriptor = new VintageTestDescriptor(uniqueId, description, null);

		assertEquals(testDescriptor.getDisplayName(), testDescriptor.getLegacyReportingName());
		assertEquals(suiteName, testDescriptor.getLegacyReportingName());
	}

}
