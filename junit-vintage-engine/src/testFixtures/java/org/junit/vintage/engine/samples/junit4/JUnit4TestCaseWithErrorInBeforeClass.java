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

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @since 4.12
 */
public class JUnit4TestCaseWithErrorInBeforeClass {

	@BeforeClass
	public static void failingBeforeClass() {
		fail("something went wrong");
	}

	@Test
	public void test() {
		fail("this should never be called");
	}

}
