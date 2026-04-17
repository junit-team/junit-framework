/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class OneMillionTests {

	@TestFactory
	Stream<DynamicTest> tests() {
		return IntStream.range(0, 1_000_000) //
				.mapToObj(i -> dynamicTest("test " + i, () -> {
					assertTrue(i + 1 < 1_000_000); // fail last test
				}));
	}

}
