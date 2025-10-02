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

import static org.junit.jupiter.api.condition.EnabledIfEnvironmentVariableIntegrationTests.BOGUS;
import static org.junit.jupiter.api.condition.EnabledIfEnvironmentVariableIntegrationTests.ENIGMA;
import static org.junit.jupiter.api.condition.EnabledIfEnvironmentVariableIntegrationTests.KEY1;
import static org.junit.jupiter.api.condition.EnabledIfEnvironmentVariableIntegrationTests.KEY2;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * Unit tests for {@link EnabledIfEnvironmentVariableCondition}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link EnabledIfEnvironmentVariableIntegrationTests}.
 *
 * @since 5.1
 */
class EnabledIfEnvironmentVariableConditionTests extends AbstractExecutionConditionTests {

	/**
	 * Stubbed subclass of {@link EnabledIfEnvironmentVariableCondition}.
	 */
	private ExecutionCondition condition = new EnabledIfEnvironmentVariableCondition() {

		@Override
		protected @Nullable String getEnvironmentVariable(String name) {
			return KEY1.equals(name) ? ENIGMA : null;
		}
	};

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return condition;
	}

	@Override
	protected Class<?> getTestClass() {
		return EnabledIfEnvironmentVariableIntegrationTests.class;
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains(
			"No @EnabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#blankNamedAttribute()
	 */
	@Test
	void blankNamedAttribute() {
		assertPreconditionViolationFor(this::evaluateCondition).withMessageContaining(
			"The 'named' attribute must not be blank");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#blankMatchesAttribute()
	 */
	@Test
	void blankMatchesAttribute() {
		assertPreconditionViolationFor(this::evaluateCondition).withMessageContaining(
			"The 'matches' attribute must not be blank");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#enabledBecauseEnvironmentVariableMatchesExactly()
	 */
	@Test
	void enabledBecauseEnvironmentVariableMatchesExactly() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains(
			"No @EnabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#enabledBecauseBothEnvironmentVariablesMatchExactly()
	 */
	@Test
	void enabledBecauseBothEnvironmentVariablesMatchExactly() {
		this.condition = new EnabledIfEnvironmentVariableCondition() {

			@Override
			protected @Nullable String getEnvironmentVariable(String name) {
				return KEY1.equals(name) || KEY2.equals(name) ? ENIGMA : null;
			}
		};

		evaluateCondition();
		assertEnabled();
		assertReasonContains(
			"No @EnabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#enabledBecauseEnvironmentVariableMatchesPattern()
	 */
	@Test
	void enabledBecauseEnvironmentVariableMatchesPattern() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains(
			"No @EnabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#disabledBecauseEnvironmentVariableDoesNotMatch()
	 */
	@Test
	void disabledBecauseEnvironmentVariableDoesNotMatch() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("does not match regular expression");
		assertCustomDisabledReasonIs("Not bogus");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#disabledBecauseEnvironmentVariableForComposedAnnotationDoesNotMatch()
	 */
	@Test
	void disabledBecauseEnvironmentVariableForComposedAnnotationDoesNotMatch() {
		this.condition = new EnabledIfEnvironmentVariableCondition() {

			@Override
			protected @Nullable String getEnvironmentVariable(String name) {
				return KEY1.equals(name) ? ENIGMA : KEY2.equals(name) ? BOGUS : null;
			}
		};

		evaluateCondition();
		assertDisabled();
		assertReasonContains("does not match regular expression");
	}

	/**
	 * @see EnabledIfEnvironmentVariableIntegrationTests#disabledBecauseEnvironmentVariableDoesNotExist()
	 */
	@Test
	void disabledBecauseEnvironmentVariableDoesNotExist() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("does not exist");
	}

}
