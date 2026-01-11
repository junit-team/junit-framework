/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test case used in {@link JUnit4ParameterizedTests}.
 *
 * @since 4.12
 */
@RunWith(Parameterized.class)
public class JUnit4ParameterizedTestCase {

	@Parameters
	public static Object[] data() {
		return new Object[] { 1, 2, 3 };
	}

	public JUnit4ParameterizedTestCase(int i) {
	}

	@Test
	public void test1() {
		fail("this test should fail");
	}

	@Test
	public void endingIn_test1() {
		fail("this test should fail");
	}

	@Test
	public void test1_atTheBeginning() {
		fail("this test should fail");
	}

	@Test
	public void test2() {
		/* always succeeds */
	}

}
