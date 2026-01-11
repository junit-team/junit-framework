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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link EnabledIfEnvironmentVariable}.
 *
 * @since 5.1
 */
@Disabled("Disabled since the required environment variables are not set")
// Tests (except those with intentional configuration errors) will pass if you set
// the following environment variables:
// EnabledIfEnvironmentVariableTests.key1 = enigma
// EnabledIfEnvironmentVariableTests.key2 = enigma
class EnabledIfEnvironmentVariableIntegrationTests {

	static final String KEY1 = "EnabledIfEnvironmentVariableTests.key1";
	static final String KEY2 = "EnabledIfEnvironmentVariableTests.key2";
	static final String ENIGMA = "enigma";
	static final String PUZZLE = "puzzle";
	static final String BOGUS = "bogus";

	@Test
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "  ", matches = ENIGMA)
	void blankNamedAttribute() {
	}

	@Test
	@EnabledIfEnvironmentVariable(named = KEY1, matches = "  ")
	void blankMatchesAttribute() {
	}

	@Test
	@EnabledIfEnvironmentVariable(named = KEY1, matches = ENIGMA)
	void enabledBecauseEnvironmentVariableMatchesExactly() {
		assertEquals(ENIGMA, System.getenv(KEY1));
	}

	@Test
	@EnabledIfEnvironmentVariable(named = KEY1, matches = ENIGMA)
	@EnabledIfEnvironmentVariable(named = KEY2, matches = ENIGMA)
	void enabledBecauseBothEnvironmentVariablesMatchExactly() {
		assertEquals(ENIGMA, System.getenv(KEY1));
		assertEquals(ENIGMA, System.getenv(KEY2));
	}

	@Test
	@EnabledIfEnvironmentVariable(named = KEY1, matches = ".*e.+ma$")
	void enabledBecauseEnvironmentVariableMatchesPattern() {
		assertEquals(ENIGMA, System.getenv(KEY1));
	}

	@Test
	@EnabledIfEnvironmentVariable(named = KEY1, matches = BOGUS, disabledReason = "Not bogus")
	void disabledBecauseEnvironmentVariableDoesNotMatch() {
		fail("should be disabled");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = KEY1, matches = ENIGMA)
	@CustomEnabled
	void disabledBecauseEnvironmentVariableForComposedAnnotationDoesNotMatch() {
		fail("should be disabled");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = BOGUS, matches = "doesn't matter")
	void disabledBecauseEnvironmentVariableDoesNotExist() {
		fail("should be disabled");
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@EnabledIfEnvironmentVariable(named = KEY2, matches = PUZZLE)
	@interface CustomEnabled {
	}

}
