/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DisabledIfSystemProperty}.
 *
 * @since 5.1
 */
class DisabledIfSystemPropertyIntegrationTests {

	private static final String KEY1 = "DisabledIfSystemPropertyTests.key1";
	private static final String KEY2 = "DisabledIfSystemPropertyTests.key2";
	private static final String ENIGMA = "enigma";
	private static final String BOGUS = "bogus";

	@BeforeAll
	static void setSystemProperties() {
		System.setProperty(KEY1, ENIGMA);
		System.setProperty(KEY2, ENIGMA);
	}

	@AfterAll
	static void clearSystemProperties() {
		System.clearProperty(KEY1);
		System.clearProperty(KEY2);
	}

	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		// no-op
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledIfSystemProperty(named = "  ", matches = ENIGMA)
	void blankNamedAttribute() {
		fail("should be disabled");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledIfSystemProperty(named = KEY1, matches = "  ")
	void blankMatchesAttribute() {
		fail("should be disabled");
	}

	@Test
	@DisabledIfSystemProperty(named = KEY1, matches = ENIGMA, disabledReason = "That's an enigma")
	void disabledBecauseSystemPropertyMatchesExactly() {
		fail("should be disabled");
	}

	@Test
	@DisabledIfSystemProperty(named = KEY1, matches = BOGUS)
	@CustomDisabled
	void disabledBecauseSystemPropertyForComposedAnnotationMatchesExactly() {
		fail("should be disabled");
	}

	@Test
	@DisabledIfSystemProperty(named = KEY1, matches = ".*e.+gma$")
	void disabledBecauseSystemPropertyMatchesPattern() {
		fail("should be disabled");
	}

	@Test
	@DisabledIfSystemProperty(named = KEY1, matches = BOGUS)
	void enabledBecauseSystemPropertyDoesNotMatch() {
		assertNotEquals(BOGUS, System.getProperty(KEY1));
	}

	@Test
	@DisabledIfSystemProperty(named = BOGUS, matches = "doesn't matter")
	void enabledBecauseSystemPropertyDoesNotExist() {
		assertNull(System.getProperty(BOGUS));
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@DisabledIfSystemProperty(named = KEY2, matches = ENIGMA)
	@interface CustomDisabled {
	}

}
