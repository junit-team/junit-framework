/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.testinterface;

import static example.util.StringUtils.isPalindrome;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

// @formatter:off
// tag::user_guide[]
interface TestInterfaceDynamicTestsDemo {

	@TestFactory
	default Stream<DynamicTest> dynamicTestsForPalindromes() {
		return Stream.of("racecar", "radar", "mom", "dad")
			.map(text -> dynamicTest(text, () -> assertTrue(isPalindrome(text))));
	}

}
// end::user_guide[]
// @formatter:on
