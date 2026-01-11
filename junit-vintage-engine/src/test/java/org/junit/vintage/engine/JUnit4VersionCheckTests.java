/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

/**
 * @since 5.4
 */
class JUnit4VersionCheckTests {

	/**
	 * @since 5.7
	 */
	@Test
	void handlesParsingSupportedVersionIdWithStandardVersionFormat() {
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.12"));
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.13"));
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.13.1"));
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.13.2"));
	}

	/**
	 * @since 5.7
	 */
	@Test
	void handlesParsingSupportedVersionIdWithCustomizedVersionFormat() {
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.12-patch_1"));
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.12.0"));
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.12.0.1"));
		assertDoesNotThrow(() -> JUnit4VersionCheck.checkSupported(() -> "4.12.0.patch-042"));
	}

	@Test
	void throwsExceptionForUnsupportedVersion() {
		var exception = assertThrows(JUnitException.class, () -> JUnit4VersionCheck.checkSupported(() -> "4.11"));

		assertEquals("Unsupported version of junit:junit: 4.11. Please upgrade to version 4.12 or later.",
			exception.getMessage());
	}

	@Test
	void handlesErrorsReadingVersion() {
		Error error = new NoClassDefFoundError();

		var exception = assertThrows(JUnitException.class, () -> JUnit4VersionCheck.checkSupported(() -> {
			throw error;
		}));

		assertEquals("Failed to read version of junit:junit", exception.getMessage());
		assertSame(error, exception.getCause());
	}

	@Test
	void handlesErrorsParsingVersion() {
		var exception = assertThrows(JUnitException.class,
			() -> JUnit4VersionCheck.checkSupported(() -> "not a version"));

		assertEquals("Failed to parse version of junit:junit: not a version", exception.getMessage());
	}

	@Test
	@Tag("missing-junit4")
	void handlesMissingJUnit() {
		var exception = assertThrows(JUnitException.class, JUnit4VersionCheck::checkSupported);

		assertEquals("Invalid class/module path: junit-vintage-engine is present but junit:junit is not. "
				+ "Please either remove junit-vintage-engine or add junit:junit, or alternatively use "
				+ "an excludeEngines(\"junit-vintage\") filter.",
			exception.getMessage());
	}

}
