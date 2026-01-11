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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DisabledIfEnvironmentVariable}.
 *
 * @since 5.1
 */
@Disabled("Disabled since the required environment variables are not set")
// Tests (except those with intentional configuration errors) will pass if you set
// the following environment variables:
// DisabledIfEnvironmentVariableTests.key1 = enigma
// DisabledIfEnvironmentVariableTests.key2 = enigma
class DisabledIfEnvironmentVariableIntegrationTests {

	static final String KEY1 = "DisabledIfEnvironmentVariableTests.key1";
	static final String KEY2 = "DisabledIfEnvironmentVariableTests.key2";
	static final String ENIGMA = "enigma";
	static final String BOGUS = "bogus";

	@Test
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@DisabledIfEnvironmentVariable(named = "  ", matches = ENIGMA)
	void blankNamedAttribute() {
	}

	@Test
	@DisabledIfEnvironmentVariable(named = KEY1, matches = "  ")
	void blankMatchesAttribute() {
	}

	@Test
	@DisabledIfEnvironmentVariable(named = KEY1, matches = ENIGMA, disabledReason = "That's an enigma")
	void disabledBecauseEnvironmentVariableMatchesExactly() {
		fail("should be disabled");
	}

	@Test
	@DisabledIfEnvironmentVariable(named = KEY1, matches = BOGUS)
	@CustomDisabled
	void disabledBecauseEnvironmentVariableForComposedAnnotationMatchesExactly() {
		fail("should be disabled");
	}

	@Test
	@DisabledIfEnvironmentVariable(named = KEY1, matches = ".*e.+gma$")
	void disabledBecauseEnvironmentVariableMatchesPattern() {
		fail("should be disabled");
	}

	@Test
	@DisabledIfEnvironmentVariable(named = KEY1, matches = BOGUS)
	void enabledBecauseEnvironmentVariableDoesNotMatch() {
		assertNotEquals(BOGUS, System.getenv(KEY1));
	}

	@Test
	@DisabledIfEnvironmentVariable(named = BOGUS, matches = "doesn't matter")
	void enabledBecauseEnvironmentVariableDoesNotExist() {
		assertNull(System.getenv(BOGUS));
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@DisabledIfEnvironmentVariable(named = KEY2, matches = ENIGMA)
	@interface CustomDisabled {
	}

}
