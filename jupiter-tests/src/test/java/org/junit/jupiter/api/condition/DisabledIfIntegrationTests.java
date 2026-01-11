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
 * Integration tests for {@link DisabledIf}.
 *
 * @since 5.7
 */
public class DisabledIfIntegrationTests {

	@Test
	@Disabled("Only used in a unit test via reflection")
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@DisabledIf(value = "staticMethodThatReturnsTrue", disabledReason = "Disabled for some reason")
	void disabledBecauseStaticConditionMethodReturnsTrue() {
		fail("Should be disabled");
	}

	@Test
	@DisabledIf("staticMethodThatReturnsFalse")
	void enabledBecauseStaticConditionMethodReturnsFalse() {
	}

	@Test
	@DisabledIf("methodThatReturnsTrue")
	void disabledBecauseConditionMethodReturnsTrue() {
		fail("Should be disabled");
	}

	@Test
	@DisabledIf("methodThatReturnsFalse")
	void enabledBecauseConditionMethodReturnsFalse() {
	}

	@Test
	@DisabledIf("org.junit.jupiter.api.condition.StaticConditionMethods#returnsTrue")
	void disabledBecauseStaticExternalConditionMethodReturnsTrue() {
		fail("Should be disabled");
	}

	@Test
	@DisabledIf("org.junit.jupiter.api.condition.StaticConditionMethods#returnsFalse")
	void enabledBecauseStaticExternalConditionMethodReturnsFalse() {
	}

	@Nested
	@DisabledIf("org.junit.jupiter.api.condition.StaticConditionMethods#returnsTrue")
	class ConditionallyDisabledClass {

		@Test
		void disabledBecauseConditionMethodReturnsTrue() {
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
