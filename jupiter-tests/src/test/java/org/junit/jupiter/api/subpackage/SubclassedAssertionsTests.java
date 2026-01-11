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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Tests which verify that {@link Assertions} can be subclassed.
 *
 * @since 5.3
 */
class SubclassedAssertionsTests extends Assertions {

	@Test
	void assertTrueWithBooleanTrue() {
		assertTrue(true);
		assertTrue(true, "test");
		assertTrue(true, () -> "test");
	}

	@Test
	void assertFalseWithBooleanTrue() {
		AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> assertFalse(true));
		assertEquals("expected: <false> but was: <true>", error.getMessage());
	}

}
