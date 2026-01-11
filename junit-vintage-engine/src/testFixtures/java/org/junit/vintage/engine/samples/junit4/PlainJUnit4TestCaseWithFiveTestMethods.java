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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.vintage.engine.samples.junit4.Categories.Failing;
import org.junit.vintage.engine.samples.junit4.Categories.Plain;
import org.junit.vintage.engine.samples.junit4.Categories.Skipped;
import org.junit.vintage.engine.samples.junit4.Categories.SkippedWithReason;

/**
 * @since 4.12
 */
@FixMethodOrder(NAME_ASCENDING)
@Category(Plain.class)
public class PlainJUnit4TestCaseWithFiveTestMethods {

	@Test
	public void abortedTest() {
		assumeFalse("this test should be aborted", true);
	}

	@Test
	@Category(Failing.class)
	public void failingTest() {
		fail("this test should fail");
	}

	@Test
	@Ignore
	@Category(Skipped.class)
	public void ignoredTest1_withoutReason() {
		fail("this should never be called");
	}

	@Test
	@Ignore("a custom reason")
	@Category(SkippedWithReason.class)
	public void ignoredTest2_withReason() {
		fail("this should never be called");
	}

	@Test
	public void successfulTest() {
		assertEquals(3, 1 + 2);
	}

}
