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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.platform.console.output.Theme;

class ThemeTests {

	@Test
	void givenUtf8ShouldReturnUnicode() {
		assertEquals(Theme.UNICODE, Theme.valueOf(StandardCharsets.UTF_8));
	}

	@Test
	void givenAnythingElseShouldReturnAscii() {
		assertAll("All character sets that are not UTF-8 should return Theme.ASCII", () -> {
			assertEquals(Theme.ASCII, Theme.valueOf(StandardCharsets.ISO_8859_1));
			assertEquals(Theme.ASCII, Theme.valueOf(StandardCharsets.US_ASCII));
			assertEquals(Theme.ASCII, Theme.valueOf(StandardCharsets.UTF_16));
		});
	}

}
