/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;
import org.junit.platform.console.options.ConsoleUtils;

class ConsoleUtilsTests {

	@Test
	void consoleCharsetReportedByConsoleUtilsIsEitherNativeCharsetOrDefaultCharset() {
		var console = System.console();
		var expected = console != null ? console.charset() : Charset.defaultCharset();
		assertEquals(expected, ConsoleUtils.charset());
	}

}
