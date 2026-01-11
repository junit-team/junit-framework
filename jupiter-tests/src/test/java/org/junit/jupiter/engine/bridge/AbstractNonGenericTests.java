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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @since 5.0
 */
@ExtendWith(NumberResolver.class)
abstract class AbstractNonGenericTests {

	@Test
	void mA() {
		BridgeMethodTests.sequence.add("mA()");
	}

	@Test
	void test(Number value) {
		BridgeMethodTests.sequence.add("A.test(Number)");
		Assertions.assertEquals(42, value);
	}

	static class B extends AbstractNonGenericTests {

		@Test
		void mB() {
			BridgeMethodTests.sequence.add("mB()");
		}

		@Test
		void test(Byte value) {
			BridgeMethodTests.sequence.add("B.test(Byte)");
			Assertions.assertEquals(123, value.intValue());
		}

	}

	static class C extends B {

		@Test
		void mC() {
			BridgeMethodTests.sequence.add("mC()");
		}

		@Override
		@Test
		void test(Byte value) {
			BridgeMethodTests.sequence.add("C.test(Byte)");
			Assertions.assertEquals(123, value.intValue());
		}

	}

}
