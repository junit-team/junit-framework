/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

class MediaTypeTests {

	@Test
	void parse() {
		MediaType mediaType = MediaType.parse("text/plain");
		assertEquals("text/plain", mediaType.toString());
	}

	@Test
	void create() {
		MediaType mediaType = MediaType.create("application", "json");
		assertEquals("application/json", mediaType.toString());
	}

	@Test
	void createWithCharset() {
		MediaType mediaType = MediaType.create("application", "json", StandardCharsets.UTF_8);
		assertEquals("application/json; charset=UTF-8", mediaType.toString());
	}

	@Test
	void parseWithInvalidMediaType() {
		var exception = assertThrows(PreconditionViolationException.class, () -> MediaType.parse("invalid"));
		assertEquals("Invalid media type: 'invalid'", exception.getMessage());
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void parseWithNullMediaType() {
		var exception = assertThrows(PreconditionViolationException.class, () -> MediaType.parse(null));
		assertEquals("value must not be null", exception.getMessage());
	}

	@Test
	void equals() {
		MediaType mediaType1 = MediaType.TEXT_PLAIN;
		MediaType mediaType2 = MediaType.parse("text/plain");
		MediaType mediaType3 = MediaType.parse("application/json");

		assertEqualsAndHashCode(mediaType1, mediaType2, mediaType3);
	}
}
