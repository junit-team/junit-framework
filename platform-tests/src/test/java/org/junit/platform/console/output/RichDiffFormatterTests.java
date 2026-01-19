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
	void lineAdded() {
		assertRichDiffEquals("""
				{
				  "speaker": "world"
				}
				""", """
				{
				  "speaker": "world"
				  "message": "hello"
				}
				""", """
				expected did not match actual
				+ actual - expected
				  {
				    "speaker": "world"
				+   "message": "hello"
				  }
				""");
	}

	@Test
	void lineRemoved() {
		assertRichDiffEquals("""
				{
				  "speaker": "world"
				  "message": "hello"
				}
				""", """
				{
				  "speaker": "world"
				}
				""", """
				expected did not match actual
				+ actual - expected
				  {
				    "speaker": "world"
				-   "message": "hello"
				  }
				""");
	}

	@Test
	void lineChanged() {
		assertRichDiffEquals("""
				{
				  "speaker": "world"
				  "message": "hello"
				}
				""", """
				{
				  "speaker": "you"
				  "message": "hello"
				}
				""", """
				expected did not match actual
				+ actual - expected
				  {
				+   "speaker": "world"
				-   "speaker": "you"
				    "message": "hello"
				  }
				""");
	}

	private static void assertRichDiffEquals(String expected, String actual, String expectedDiff) {
		var formatter = new RichDiffFormatter();
		var assertionFailed = assertThrows(AssertionFailedError.class, () -> assertEquals(expected, actual));
		assertEquals(expectedDiff, formatter.format(assertionFailed));
	}

}
