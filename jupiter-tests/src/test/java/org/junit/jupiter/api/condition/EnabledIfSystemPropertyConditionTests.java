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

import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotBlankFor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * Unit tests for {@link EnabledIfSystemPropertyCondition}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link EnabledIfSystemPropertyIntegrationTests}.
 *
 * @since 5.1
 */
class EnabledIfSystemPropertyConditionTests extends AbstractExecutionConditionTests {

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return new EnabledIfSystemPropertyCondition();
	}

	@Override
	protected Class<?> getTestClass() {
		return EnabledIfSystemPropertyIntegrationTests.class;
	}

	@BeforeAll
	static void setSystemProperties() {
		EnabledIfSystemPropertyIntegrationTests.setSystemProperties();
	}

	@AfterAll
	static void clearSystemProperties() {
		EnabledIfSystemPropertyIntegrationTests.clearSystemProperties();
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("No @EnabledIfSystemProperty conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#blankNamedAttribute()
	 */
	@Test
	void blankNamedAttribute() {
		assertPreconditionViolationNotBlankFor("The 'named' attribute", this::evaluateCondition);
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#blankMatchesAttribute()
	 */
	@Test
	void blankMatchesAttribute() {
		assertPreconditionViolationNotBlankFor("The 'matches' attribute", this::evaluateCondition);
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#enabledBecauseSystemPropertyMatchesExactly()
	 */
	@Test
	void enabledBecauseSystemPropertyMatchesExactly() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("No @EnabledIfSystemProperty conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#enabledBecauseBothSystemPropertiesMatchExactly()
	 */
	@Test
	void enabledBecauseBothSystemPropertiesMatchExactly() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("No @EnabledIfSystemProperty conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#enabledBecauseSystemPropertyMatchesPattern()
	 */
	@Test
	void enabledBecauseSystemPropertyMatchesPattern() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("No @EnabledIfSystemProperty conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#disabledBecauseSystemPropertyDoesNotMatch()
	 */
	@Test
	void disabledBecauseSystemPropertyDoesNotMatch() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("does not match regular expression");
		assertCustomDisabledReasonIs("Not bogus");
	}

	@Test
	void disabledBecauseSystemPropertyForComposedAnnotationDoesNotMatch() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("does not match regular expression");
	}

	/**
	 * @see EnabledIfSystemPropertyIntegrationTests#disabledBecauseSystemPropertyDoesNotExist()
	 */
	@Test
	void disabledBecauseSystemPropertyDoesNotExist() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("does not exist");
	}

}
