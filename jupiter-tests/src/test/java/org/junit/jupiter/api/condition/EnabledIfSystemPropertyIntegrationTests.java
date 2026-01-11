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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Integration tests for {@link EnabledIfSystemProperty}.
 *
 * @since 5.1
 */
class EnabledIfSystemPropertyIntegrationTests {

	private static final String KEY1 = "EnabledIfSystemPropertyTests.key1";
	private static final String KEY2 = "EnabledIfSystemPropertyTests.key2";
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
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@EnabledIfSystemProperty(named = "  ", matches = ENIGMA)
	void blankNamedAttribute() {
		fail("should be disabled");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@EnabledIfSystemProperty(named = KEY1, matches = "  ")
	void blankMatchesAttribute() {
		fail("should be disabled");
	}

	@Test
	@EnabledIfSystemProperty(named = KEY1, matches = ENIGMA)
	void enabledBecauseSystemPropertyMatchesExactly() {
		assertEquals(ENIGMA, System.getProperty(KEY1));
	}

	@Test
	@EnabledIfSystemProperty(named = KEY1, matches = ENIGMA)
	@EnabledIfSystemProperty(named = KEY2, matches = ENIGMA)
	void enabledBecauseBothSystemPropertiesMatchExactly() {
		assertEquals(ENIGMA, System.getProperty(KEY1));
		assertEquals(ENIGMA, System.getProperty(KEY2));
	}

	@Test
	@EnabledIfSystemProperty(named = KEY1, matches = ".*en.+gma$")
	void enabledBecauseSystemPropertyMatchesPattern() {
		assertEquals(ENIGMA, System.getProperty(KEY1));
	}

	@Test
	@EnabledIfSystemProperty(named = KEY1, matches = BOGUS, disabledReason = "Not bogus")
	void disabledBecauseSystemPropertyDoesNotMatch() {
		fail("should be disabled");
	}

	@Test
	@EnabledIfSystemProperty(named = KEY1, matches = ENIGMA)
	@CustomEnabled
	void disabledBecauseSystemPropertyForComposedAnnotationDoesNotMatch() {
		fail("should be disabled");
	}

	@Test
	@EnabledIfSystemProperty(named = BOGUS, matches = "doesn't matter")
	void disabledBecauseSystemPropertyDoesNotExist() {
		fail("should be disabled");
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@EnabledIfSystemProperty(named = KEY2, matches = BOGUS)
	@interface CustomEnabled {
	}

}
