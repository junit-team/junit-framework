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
 * Unit tests for {@link DisabledIfSystemPropertyCondition}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link DisabledIfSystemPropertyIntegrationTests}.
 *
 * @since 5.1
 */
class DisabledIfSystemPropertyConditionTests extends AbstractExecutionConditionTests {

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return new DisabledIfSystemPropertyCondition();
	}

	@Override
	protected Class<?> getTestClass() {
		return DisabledIfSystemPropertyIntegrationTests.class;
	}

	@BeforeAll
	static void setSystemProperties() {
		DisabledIfSystemPropertyIntegrationTests.setSystemProperties();
	}

	@AfterAll
	static void clearSystemProperties() {
		DisabledIfSystemPropertyIntegrationTests.clearSystemProperties();
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("No @DisabledIfSystemProperty conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#blankNamedAttribute()
	 */
	@Test
	void blankNamedAttribute() {
		assertPreconditionViolationNotBlankFor("The 'named' attribute", this::evaluateCondition);
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#blankMatchesAttribute()
	 */
	@Test
	void blankMatchesAttribute() {
		assertPreconditionViolationNotBlankFor("The 'matches' attribute", this::evaluateCondition);
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#disabledBecauseSystemPropertyMatchesExactly()
	 */
	@Test
	void disabledBecauseSystemPropertyMatchesExactly() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
		assertCustomDisabledReasonIs("That's an enigma");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#disabledBecauseSystemPropertyForComposedAnnotationMatchesExactly()
	 */
	@Test
	void disabledBecauseSystemPropertyForComposedAnnotationMatchesExactly() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#disabledBecauseSystemPropertyMatchesPattern()
	 */
	@Test
	void disabledBecauseSystemPropertyMatchesPattern() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#enabledBecauseSystemPropertyDoesNotMatch()
	 */
	@Test
	void enabledBecauseSystemPropertyDoesNotMatch() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("No @DisabledIfSystemProperty conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#enabledBecauseSystemPropertyDoesNotExist()
	 */
	@Test
	void enabledBecauseSystemPropertyDoesNotExist() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("No @DisabledIfSystemProperty conditions resulting in 'disabled' execution encountered");
	}

}
