/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import org.junit.jupiter.api.Test;

public class StdStreamTestCase {

	private static final String STDOUT_DATA = "Writing to STDOUT...";
	private static final String STDERR_DATA = "Writing to STDERR...";

	public static int getStdoutOutputFileSize() {
		return STDOUT_DATA.length();
	}

	public static int getStderrOutputFileSize() {
		return STDERR_DATA.length();
	}

	@Test
	void printTest() {
		System.out.print(STDOUT_DATA);
		System.err.print(STDERR_DATA);
	}

}
