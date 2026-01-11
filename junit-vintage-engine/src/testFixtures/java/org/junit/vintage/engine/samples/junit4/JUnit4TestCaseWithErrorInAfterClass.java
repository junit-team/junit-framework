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
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

/**
 * @since 4.12
 */
@FixMethodOrder(NAME_ASCENDING)
public class JUnit4TestCaseWithErrorInAfterClass {

	@AfterClass
	public static void failingAfterClass() {
		fail("error in @AfterClass");
	}

	@Test
	public void failingTest() {
		fail("expected to fail");
	}

	@Test
	public void succeedingTest() {
		// no-op
	}

}
