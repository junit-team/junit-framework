/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.testcases;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @since 1.11
 */
public class StatefulTestCase {

	public static List<String> callSequence = new ArrayList<>();

	@SuppressWarnings("NewClassNamingConvention")
	public static class Test1 {

		@Test
		void statefulTest() {
			callSequence.add("test1");
		}

	}

	@SuppressWarnings("NewClassNamingConvention")
	public static class Test2 {

		@Test
		void statefulTest() {
			callSequence.add("test2");
			fail("This is a failing test");
		}

	}

}
