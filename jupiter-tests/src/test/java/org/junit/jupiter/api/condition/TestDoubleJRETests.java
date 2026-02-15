/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.JRE.JAVA_23;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JRE} using {@link TestDoubleJRE}
 *
 * @since 6.1
 */
public class TestDoubleJRETests {

	@Test
	@EnabledForJreRange(min = JAVA_23) // "23" because "22" is the maximum available in TestDoubleJRE
	@SuppressWarnings("deprecation")
	void currentJreIsOtherForUnsupportedJre() {
		assertEquals(TestDoubleJRE.OTHER, TestDoubleJRE.currentJre());
		assertTrue(TestDoubleJRE.OTHER.isCurrentVersion());
		assertTrue(TestDoubleJRE.isCurrentVersion(TestDoubleJRE.OTHER.version()));
	}

}
