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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * @since 5.0
 */
interface NumberTestGroup {

	class ByteTestCase extends AbstractNumberTests<Byte> {

		@Test
		@Override
		void test(Byte number) {
			BridgeMethodTests.sequence.add("test(Byte) BEGIN");
			super.test(number);
			BridgeMethodTests.sequence.add("test(Byte) END");
		}

		@Test
		void test(Long number) {
			BridgeMethodTests.sequence.add("test(Long) BEGIN");
			assertNotNull(number);
			assertEquals(123, number.intValue());
			BridgeMethodTests.sequence.add("test(Long) END");
		}
	}

	class ShortTestCase extends AbstractNumberTests<Short> {

		@Test
		@Override
		void test(Short number) {
			BridgeMethodTests.sequence.add("test(Short) BEGIN");
			super.test(number);
			BridgeMethodTests.sequence.add("test(Short) END");
		}

		@Test
		void test(Long number) {
			BridgeMethodTests.sequence.add("test(Long) BEGIN");
			assertNotNull(number);
			assertEquals(123, number.intValue());
			BridgeMethodTests.sequence.add("test(Long) END");
		}
	}

}
