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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.JRE.JAVA_17;
import static org.junit.jupiter.api.condition.JRE.JAVA_18;
import static org.junit.jupiter.api.condition.JRE.JAVA_19;
import static org.junit.jupiter.api.condition.JRE.JAVA_21;
import static org.junit.jupiter.api.condition.JRE.OTHER;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava17;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava18;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava19;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onKnownVersion;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onOtherVersion;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DisabledForJreRange @DisabledForJreRange}.
 *
 * @since 5.6
 */
class DisabledForJreRangeIntegrationTests {

	@Test
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange
	void defaultValues() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(min = JAVA_17, max = OTHER)
	void effectiveJreDefaultValues() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(minVersion = 17, maxVersion = Integer.MAX_VALUE)
	void effectiveVersionDefaultValues() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(min = JAVA_17)
	void min17() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(minVersion = 17)
	void minVersion17() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(max = OTHER)
	void maxOther() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(maxVersion = Integer.MAX_VALUE)
	void maxVersionMaxInteger() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(minVersion = 7)
	void minVersion7() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(maxVersion = 16)
	void maxVersion16() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(min = JAVA_18, minVersion = 21)
	void minAndMinVersion() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(max = JAVA_18, maxVersion = 21)
	void maxAndMaxVersion() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(min = JAVA_21, max = JAVA_17)
	void minGreaterThanMax() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(min = JAVA_21, maxVersion = 17)
	void minGreaterThanMaxVersion() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(minVersion = 21, maxVersion = 17)
	void minVersionGreaterThanMaxVersion() {
		fail("should result in a configuration exception");
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledForJreRange(minVersion = 21, max = JAVA_17)
	void minVersionGreaterThanMax() {
		fail("should result in a configuration exception");
	}

	@Test
	@DisabledForJreRange(min = JAVA_18)
	void min18() {
		assertFalse(onJava18() || onJava19());
		assertTrue(onJava17());
	}

	@Test
	@DisabledForJreRange(minVersion = 18)
	void minVersion18() {
		min18();
	}

	@Test
	@DisabledForJreRange(max = JAVA_18)
	void max18() {
		assertFalse(onJava17() || onJava18());
	}

	@Test
	@DisabledForJreRange(maxVersion = 18)
	void maxVersion18() {
		max18();
	}

	@Test
	@DisabledForJreRange(min = JAVA_17, max = JAVA_17)
	void min17Max17() {
		assertFalse(onJava17());
	}

	@Test
	@DisabledForJreRange(minVersion = 17, maxVersion = 17)
	void minVersion17MaxVersion17() {
		min17Max17();
	}

	@Test
	@DisabledForJreRange(min = JAVA_18, max = JAVA_19, disabledReason = "Disabled on Java 18 & 19")
	void min18Max19() {
		assertFalse(onJava18() || onJava19());
	}

	@Test
	@DisabledForJreRange(minVersion = 18, maxVersion = 19, disabledReason = "Disabled on Java 18 & 19")
	void minVersion18MaxVersion19() {
		min18Max19();
	}

	@Test
	@DisabledForJreRange(min = OTHER, max = OTHER)
	void minOtherMaxOther() {
		assertTrue(onKnownVersion() || onOtherVersion());
	}

	@Test
	@DisabledForJreRange(minVersion = Integer.MAX_VALUE, maxVersion = Integer.MAX_VALUE)
	void minMaxIntegerMaxMaxInteger() {
		minOtherMaxOther();
	}

}
