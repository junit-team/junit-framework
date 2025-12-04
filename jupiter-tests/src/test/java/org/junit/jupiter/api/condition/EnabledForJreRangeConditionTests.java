/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava17;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava18;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava19;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava20;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava21;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava22;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava23;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava24;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava25;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava26;
import static org.junit.jupiter.api.condition.JavaVersionPredicates.onKnownVersion;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * Unit tests for {@link EnabledForJreRange @EnabledForJreRange}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link EnabledForJreRangeIntegrationTests}.
 *
 * @since 5.6
 */
class EnabledForJreRangeConditionTests extends AbstractExecutionConditionTests {

	private static final String JAVA_VERSION = System.getProperty("java.version");

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return new EnabledForJreRangeCondition();
	}

	@Override
	protected Class<?> getTestClass() {
		return EnabledForJreRangeIntegrationTests.class;
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("@EnabledForJreRange is not present");
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#defaultValues()
	 */
	@Test
	void defaultValues() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessage(
					"You must declare a non-default value for the minimum or maximum value in @EnabledForJreRange");
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#effectiveJreDefaultValues()
	 */
	@Test
	void effectiveJreDefaultValues() {
		defaultValues();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#effectiveVersionDefaultValues()
	 */
	@Test
	void effectiveVersionDefaultValues() {
		defaultValues();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#min17()
	 */
	@Test
	void min17() {
		defaultValues();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion17()
	 */
	@Test
	void minVersion17() {
		defaultValues();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#maxOther()
	 */
	@Test
	void maxOther() {
		defaultValues();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#maxVersionMaxInteger()
	 */
	@Test
	void maxVersionMaxInteger() {
		defaultValues();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion7()
	 */
	@Test
	void minVersion7() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessage("@EnabledForJreRange's minVersion [7] must be greater than or equal to 8");
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#maxVersion16()
	 */
	@Test
	void maxVersion16() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessage(
					"@EnabledForJreRange's minimum value [17] must be less than or equal to its maximum value [16]");
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minAndMinVersion()
	 */
	@Test
	void minAndMinVersion() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessage(
					"@EnabledForJreRange's minimum value must be configured with either a JRE enum constant or numeric version, but not both");
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#maxAndMaxVersion()
	 */
	@Test
	void maxAndMaxVersion() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessage(
					"@EnabledForJreRange's maximum value must be configured with either a JRE enum constant or numeric version, but not both");
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minGreaterThanMax()
	 */
	@Test
	void minGreaterThanMax() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessage(
					"@EnabledForJreRange's minimum value [21] must be less than or equal to its maximum value [17]");
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minGreaterThanMaxVersion()
	 */
	@Test
	void minGreaterThanMaxVersion() {
		minGreaterThanMax();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersionGreaterThanMaxVersion()
	 */
	@Test
	void minVersionGreaterThanMaxVersion() {
		minGreaterThanMax();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersionGreaterThanMax()
	 */
	@Test
	void minVersionGreaterThanMax() {
		minGreaterThanMax();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#min20()
	 */
	@Test
	void min20() {
		evaluateCondition();
		assertEnabledOnCurrentJreIf(
			onJava20() || onJava21() || onJava22() || onJava23() || onJava24() || onJava25() || onJava26());
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion20()
	 */
	@Test
	void minVersion20() {
		min20();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#max21()
	 */
	@Test
	void max21() {
		evaluateCondition();
		assertEnabledOnCurrentJreIf(onJava17() || onJava18() || onJava19() || onJava20() || onJava21());
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#maxVersion21()
	 */
	@Test
	void maxVersion21() {
		max21();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#min17Max21()
	 */
	@Test
	void min17Max21() {
		max21();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#min17Max17()
	 */
	@Test
	void min17Max17() {
		evaluateCondition();
		assertEnabledOnCurrentJreIf(onJava17());
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#min17MaxVersion17()
	 */
	@Test
	void min17MaxVersion17() {
		min17Max17();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion17Max17()
	 */
	@Test
	void minVersion17Max17() {
		min17Max17();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion17MaxVersion17()
	 */
	@Test
	void minVersion17MaxVersion17() {
		min17Max17();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#min20Max21()
	 */
	@Test
	void min20Max21() {
		evaluateCondition();
		assertEnabledOnCurrentJreIf(onJava20() || onJava21());
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#min20MaxVersion21()
	 */
	@Test
	void min20MaxVersion21() {
		min20Max21();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion20Max21()
	 */
	@Test
	void minVersion20Max21() {
		min20Max21();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion20MaxVersion21()
	 */
	@Test
	void minVersion20MaxVersion21() {
		min20Max21();
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minVersion21MaxVersionMaxInteger()
	 */
	@Test
	void minVersion21MaxVersionMaxInteger() {
		evaluateCondition();
		assertEnabledOnCurrentJreIf(onJava21() || onJava22() || onJava23() || onJava24() || onJava25() || onJava26());
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minOtherMaxOther()
	 */
	@Test
	void minOtherMaxOther() {
		evaluateCondition();
		assertEnabledOnCurrentJreIf(!onKnownVersion());
	}

	/**
	 * @see EnabledForJreRangeIntegrationTests#minMaxIntegerMaxMaxInteger()
	 */
	@Test
	void minMaxIntegerMaxMaxInteger() {
		minOtherMaxOther();
	}

	private void assertEnabledOnCurrentJreIf(boolean condition) {
		if (condition) {
			assertEnabled();
			assertReasonContains("Enabled on JRE version: " + JAVA_VERSION);
		}
		else {
			assertDisabled();
			assertReasonContains("Disabled on JRE version: " + JAVA_VERSION);
		}
	}

}
