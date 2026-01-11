/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Integration tests for dynamic tests.
 *
 * @since 5.5
 */
class DynamicTestIntegrationTests {

	private static final int TEN_MB = 10 * 1024 * 1024;

	/**
	 * Without the fix in {@code DynamicTestTestDescriptor}, setting the
	 * {@code -mx200m} VM argument will cause an {@link OutOfMemoryError} before
	 * the 200 limit is reached.
	 *
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/1865">Issue 1865</a>
	 */
	@TestFactory
	Stream<DynamicTest> generateDynamicTestsThatReferenceLargeAmountsOfMemory() {
		return Stream.generate(() -> new byte[TEN_MB])//
				// The lambda Executable in the following line *must* reference
				// the `bytes` array in order to hold onto the allocated memory.
				.map(bytes -> dynamicTest("test", () -> assertNotNull(bytes)))//
				.limit(200);
	}

}
