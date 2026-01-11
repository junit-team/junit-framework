/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.bridge;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.bridge.NumberTestGroup.ByteTestCase;
import org.junit.jupiter.engine.bridge.NumberTestGroup.ShortTestCase;

/**
 * @since 5.0
 */
class BridgeMethodTests extends AbstractJupiterTestEngineTests {

	static List<String> sequence = new ArrayList<>();

	@Test
	void childrenHaveBridgeMethods() throws Exception {
		assertFalse(ChildWithBridgeMethods.class.getMethod("anotherBeforeEach").isBridge());
		assertFalse(ChildWithBridgeMethods.class.getMethod("anotherAfterEach").isBridge());
		assertTrue(ChildWithBridgeMethods.class.getMethod("beforeEach").isBridge());
		assertTrue(ChildWithBridgeMethods.class.getMethod("afterEach").isBridge());

		assertTrue(ByteTestCase.class.getDeclaredMethod("test", Number.class).isBridge());
		assertFalse(ByteTestCase.class.getDeclaredMethod("test", Byte.class).isBridge());

		assertTrue(ShortTestCase.class.getDeclaredMethod("test", Number.class).isBridge());
		assertFalse(ShortTestCase.class.getDeclaredMethod("test", Short.class).isBridge());
	}

	@Test
	void childHasNoBridgeMethods() throws Exception {
		assertFalse(ChildWithoutBridgeMethods.class.getMethod("anotherBeforeEach").isBridge());
		assertFalse(ChildWithoutBridgeMethods.class.getMethod("anotherAfterEach").isBridge());
		assertFalse(ChildWithoutBridgeMethods.class.getMethod("beforeEach").isBridge());
		assertFalse(ChildWithoutBridgeMethods.class.getMethod("afterEach").isBridge());
	}

	@Test
	void compareMethodExecutionSequenceOrder() {
		String withoutBridgeMethods = execute(1, ChildWithoutBridgeMethods.class);
		String withBridgeMethods = execute(1, ChildWithBridgeMethods.class);
		assertEquals(withoutBridgeMethods, withBridgeMethods);
	}

	@TestFactory
	List<DynamicTest> ensureSingleTestMethodsExecute() {
		return Arrays.asList( //
			dynamicTest("Byte", //
				() -> assertEquals("[test(Byte) BEGIN, test(N), test(Byte) END, test(Long) BEGIN, test(Long) END]", //
					execute(2, ByteTestCase.class))),
			dynamicTest("Short", //
				() -> assertEquals("[test(Long) BEGIN, test(Long) END, test(Short) BEGIN, test(N), test(Short) END]", //
					execute(2, ShortTestCase.class))));
	}

	@Test
	void inheritedNonGenericMethodsAreExecuted() {
		String b = execute(4, AbstractNonGenericTests.B.class);
		assertAll("Missing expected test(s) in sequence: " + b, //
			() -> assertTrue(b.contains("A.test(Number)")), //
			() -> assertTrue(b.contains("mA()")), //
			() -> assertTrue(b.contains("mB()")), //
			() -> assertTrue(b.contains("B.test(Byte)")) //
		);
		String c = execute(5, AbstractNonGenericTests.C.class);
		assertAll("Missing expected test(s) in sequence: " + c, //
			() -> assertTrue(c.contains("A.test(Number)")), //
			() -> assertTrue(c.contains("mA()")), //
			() -> assertTrue(c.contains("mB()")), //
			() -> assertTrue(c.contains("mC()")), //
			() -> assertTrue(c.contains("C.test(Byte)")) //
		);
	}

	private String execute(int expectedTestFinishedCount, Class<?> testClass) {
		sequence.clear();
		executeTestsForClass(testClass).testEvents()//
				.assertStatistics(stats -> stats.started(expectedTestFinishedCount));
		return sequence.toString();
	}

}
