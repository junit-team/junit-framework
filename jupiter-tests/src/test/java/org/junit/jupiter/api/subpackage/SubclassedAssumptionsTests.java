/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.subpackage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

/**
 * Tests which verify that {@link Assumptions} can be subclassed.
 *
 * @since 5.3
 */
class SubclassedAssumptionsTests extends Assumptions {

	@Test
	void assumeTrueWithBooleanTrue() {
		String foo = null;
		try {
			assumeTrue(true);
			foo = "foo";
		}
		finally {
			assertEquals("foo", foo);
		}
	}

	@Test
	void assumeFalseWithBooleanTrue() {
		TestAbortedException exception = assertThrows(TestAbortedException.class, () -> assumeFalse(true));
		assertEquals("Assumption failed: assumption is not false", exception.getMessage());
	}

}
