/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * @since 1.8
 */
public class DynamicTestsTestCase {

	@TestFactory
	Stream<DynamicTest> dynamicTests() {
		return Stream.of(//
			dynamicTest("Add test", () -> assertEquals(2, Math.addExact(1, 1))),
			dynamicTest("Multiply Test", () -> assertEquals(4, Math.multiplyExact(2, 2)))//
		);
	}

}
