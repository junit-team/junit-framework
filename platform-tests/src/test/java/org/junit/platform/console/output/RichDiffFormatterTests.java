/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class RichDiffFormatterTests {

	@Test
	void test() {
		var expected = """
				{
				  "speaker": "world"
				  "message": "hello"
				}
				""";
		var actuall = """
				{
				  "speaker": "you"
				  "message": "hello"
				}
				""";

		var assertionFailed = assertThrows(AssertionFailedError.class, () -> assertEquals(expected, actuall));

		var formatter = new RichDiffFormatter();

		String message = formatter.format(assertionFailed);

		assertEquals("""

				""", message);

	}

}
