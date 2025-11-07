/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit.assertion;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.testkit.ExecutionResults;

/**
 * Entry point to all JUnit Pioneer assertions.
 */
public class JUnitJupiterAssert {

	private JUnitJupiterAssert() {
		// private constructor to prevent instantiation
	}

	public static ExecutionResultAssert assertThat(ExecutionResults actual) {
		return new JUnitJupiterExecutionResultAssert(actual);
	}

	public static JUnitJupiterPathAssert assertThat(Path actual) {
		return new JUnitJupiterPathAssert(actual);
	}

	/**
	 * Make an assertion on a {@link Properties} instance.
	 *
	 * @param actual The {@link Properties} instance the assertion is made with respect to
	 * @return Assertion instance
	 */
	public static PropertiesAssert assertThat(Properties actual) {
		return new PropertiesAssert(actual);
	}

}
