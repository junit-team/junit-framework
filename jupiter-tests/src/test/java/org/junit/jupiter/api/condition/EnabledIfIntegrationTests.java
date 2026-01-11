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

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link EnabledIf}.
 *
 * @since 5.7
 */
public class EnabledIfIntegrationTests {

	@Test
	@Disabled("Only used in a unit test via reflection")
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@EnabledIf("staticMethodThatReturnsTrue")
	void enabledBecauseStaticConditionMethodReturnsTrue() {
	}

	@Test
	@EnabledIf(value = "staticMethodThatReturnsFalse", disabledReason = "Disabled for some reason")
	void disabledBecauseStaticConditionMethodReturnsFalse() {
		fail("Should be disabled");
	}

	@Test
	@EnabledIf("methodThatReturnsTrue")
	void enabledBecauseConditionMethodReturnsTrue() {
	}

	@Test
	@EnabledIf("methodThatReturnsFalse")
	void disabledBecauseConditionMethodReturnsFalse() {
		fail("Should be disabled");
	}

	@Test
	@EnabledIf("org.junit.jupiter.api.condition.StaticConditionMethods#returnsTrue")
	void enabledBecauseStaticExternalConditionMethodReturnsTrue() {
	}

	@Test
	@EnabledIf("org.junit.jupiter.api.condition.StaticConditionMethods#returnsFalse")
	void disabledBecauseStaticExternalConditionMethodReturnsFalse() {
		fail("Should be disabled");
	}

	@Nested
	@EnabledIf("org.junit.jupiter.api.condition.StaticConditionMethods#returnsFalse")
	class ConditionallyDisabledClass {

		@Test
		void disabledBecauseConditionMethodReturnsFalse() {
			fail("Should be disabled");
		}

	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private static boolean staticMethodThatReturnsTrue() {
		return true;
	}

	@SuppressWarnings("unused")
	private static boolean staticMethodThatReturnsFalse() {
		return false;
	}

	@SuppressWarnings("unused")
	private boolean methodThatReturnsTrue() {
		return true;
	}

	@SuppressWarnings("unused")
	private boolean methodThatReturnsFalse() {
		return false;
	}

}
