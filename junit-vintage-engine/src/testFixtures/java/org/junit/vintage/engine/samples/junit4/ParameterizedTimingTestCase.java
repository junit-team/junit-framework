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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.AfterParam;
import org.junit.runners.Parameterized.BeforeParam;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @since 5.9
 */
@RunWith(Parameterized.class)
public class ParameterizedTimingTestCase {

	public static Map<String, Instant> EVENTS = new LinkedHashMap<>();

	@BeforeClass
	public static void beforeClass() throws Exception {
		EVENTS.clear();
	}

	@BeforeParam
	public static void beforeParam(String param) throws Exception {
		EVENTS.put("beforeParam(" + param + ")", Instant.now());
		Thread.sleep(100);
	}

	@AfterParam
	public static void afterParam(String param) throws Exception {
		Thread.sleep(100);
		System.out.println("ParameterizedTimingTestCase.afterParam");
		EVENTS.put("afterParam(" + param + ")", Instant.now());
	}

	@Parameters(name = "{0}")
	public static Iterable<String> parameters() {
		return List.of("foo", "bar");
	}

	@Parameter
	public String value;

	@Test
	public void test() {
		assertEquals("foo", value);
	}

}
