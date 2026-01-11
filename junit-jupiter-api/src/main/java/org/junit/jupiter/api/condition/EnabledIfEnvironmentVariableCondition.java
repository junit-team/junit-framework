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

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@link ExecutionCondition} for {@link EnabledIfEnvironmentVariable @EnabledIfEnvironmentVariable}.
 *
 * @since 5.1
 * @see EnabledIfEnvironmentVariable
 */
class EnabledIfEnvironmentVariableCondition
		extends AbstractRepeatableAnnotationCondition<EnabledIfEnvironmentVariable> {

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled(
		"No @EnabledIfEnvironmentVariable conditions resulting in 'disabled' execution encountered");

	EnabledIfEnvironmentVariableCondition() {
		super(EnabledIfEnvironmentVariable.class);
	}

	@Override
	protected ConditionEvaluationResult getNoDisabledConditionsEncounteredResult() {
		return ENABLED;
	}

	@Override
	protected ConditionEvaluationResult evaluate(EnabledIfEnvironmentVariable annotation) {

		String name = annotation.named().strip();
		String regex = annotation.matches();
		Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
		Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);
		String actual = getEnvironmentVariable(name);

		// Nothing to match against?
		if (actual == null) {
			return disabled("Environment variable [%s] does not exist".formatted(name), annotation.disabledReason());
		}
		if (actual.matches(regex)) {
			return enabled("Environment variable [%s] with value [%s] matches regular expression [%s]".formatted(name,
				actual, regex));
		}
		return disabled("Environment variable [%s] with value [%s] does not match regular expression [%s]".formatted(
			name, actual, regex), annotation.disabledReason());
	}

	/**
	 * Get the value of the named environment variable.
	 *
	 * <p>The default implementation delegates to
	 * {@link System#getenv(String)}. Can be overridden in a subclass for
	 * testing purposes.
	 */
	protected @Nullable String getEnvironmentVariable(String name) {
		return System.getenv(name);
	}

}
