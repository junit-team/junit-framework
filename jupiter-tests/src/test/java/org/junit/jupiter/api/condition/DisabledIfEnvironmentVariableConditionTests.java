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

import static org.junit.jupiter.api.condition.DisabledIfEnvironmentVariableIntegrationTests.ENIGMA;
import static org.junit.jupiter.api.condition.DisabledIfEnvironmentVariableIntegrationTests.KEY1;
import static org.junit.jupiter.api.condition.DisabledIfEnvironmentVariableIntegrationTests.KEY2;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotBlankFor;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * Unit tests for {@link DisabledIfEnvironmentVariableCondition}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link DisabledIfEnvironmentVariableIntegrationTests}.
 *
 * @since 5.1
 */
class DisabledIfEnvironmentVariableConditionTests extends AbstractExecutionConditionTests {

	/**
	 * Stubbed subclass of {@link DisabledIfEnvironmentVariableCondition}.
	 */
	private ExecutionCondition condition = new DisabledIfEnvironmentVariableCondition() {

		@Override
		protected @Nullable String getEnvironmentVariable(String name) {
			return KEY1.equals(name) ? ENIGMA : null;
		}
	};

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return this.condition;
	}

	@Override
	protected Class<?> getTestClass() {
		return DisabledIfEnvironmentVariableIntegrationTests.class;
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains(
			"No @DisabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#blankNamedAttribute()
	 */
	@Test
	void blankNamedAttribute() {
		assertPreconditionViolationNotBlankFor("The 'named' attribute", this::evaluateCondition);
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#blankMatchesAttribute()
	 */
	@Test
	void blankMatchesAttribute() {
		assertPreconditionViolationNotBlankFor("The 'matches' attribute", this::evaluateCondition);
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#disabledBecauseEnvironmentVariableMatchesExactly()
	 */
	@Test
	void disabledBecauseEnvironmentVariableMatchesExactly() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
		assertCustomDisabledReasonIs("That's an enigma");
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#disabledBecauseEnvironmentVariableForComposedAnnotationMatchesExactly()
	 */
	@Test
	void disabledBecauseEnvironmentVariableForComposedAnnotationMatchesExactly() {
		this.condition = new DisabledIfEnvironmentVariableCondition() {

			@Override
			protected @Nullable String getEnvironmentVariable(String name) {
				return KEY1.equals(name) || KEY2.equals(name) ? ENIGMA : null;
			}
		};

		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#disabledBecauseEnvironmentVariableMatchesPattern()
	 */
	@Test
	void disabledBecauseEnvironmentVariableMatchesPattern() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#enabledBecauseEnvironmentVariableDoesNotMatch()
	 */
	@Test
	void enabledBecauseEnvironmentVariableDoesNotMatch() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains(
			"No @DisabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see DisabledIfEnvironmentVariableIntegrationTests#enabledBecauseEnvironmentVariableDoesNotExist()
	 */
	@Test
	void enabledBecauseEnvironmentVariableDoesNotExist() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains(
			"No @DisabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");
	}

}
