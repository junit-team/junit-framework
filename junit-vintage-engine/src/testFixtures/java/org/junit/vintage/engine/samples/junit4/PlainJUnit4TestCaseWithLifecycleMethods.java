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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PlainJUnit4TestCaseWithLifecycleMethods {

	public static final List<String> EVENTS = new ArrayList<>();

	@BeforeClass
	public static void beforeClass() {
		EVENTS.add("beforeClass");
	}

	@Before
	public void before() {
		EVENTS.add("before");
	}

	@Test
	public void failingTest() {
		EVENTS.add("failingTest");
		fail();
	}

	@Test
	@Ignore("skipped")
	public void skippedTest() {
		EVENTS.add("this should never ever be executed because the test is skipped");
	}

	@Test
	public void succeedingTest() {
		EVENTS.add("succeedingTest");
	}

	@After
	public void after() {
		EVENTS.add("after");
	}

	@AfterClass
	public static void afterClass() {
		EVENTS.add("afterClass");
	}
}
