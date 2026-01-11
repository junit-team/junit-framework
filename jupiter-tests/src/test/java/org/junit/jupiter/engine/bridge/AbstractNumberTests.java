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
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @since 5.0
 */
@ExtendWith(NumberResolver.class)
abstract class AbstractNumberTests<N extends Number> {

	@Test
	void test(N number) {
		BridgeMethodTests.sequence.add("test(N)");
		assertNotNull(number);
		assertEquals(123, number.intValue());
	}

}
