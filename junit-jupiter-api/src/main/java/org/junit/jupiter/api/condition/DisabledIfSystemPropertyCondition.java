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

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@link ExecutionCondition} for {@link DisabledIfSystemProperty @DisabledIfSystemProperty}.
 *
 * @since 5.1
 * @see DisabledIfSystemProperty
 */
class DisabledIfSystemPropertyCondition extends AbstractRepeatableAnnotationCondition<DisabledIfSystemProperty> {

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled(
		"No @DisabledIfSystemProperty conditions resulting in 'disabled' execution encountered");

	DisabledIfSystemPropertyCondition() {
		super(DisabledIfSystemProperty.class);
	}

	@Override
	protected ConditionEvaluationResult getNoDisabledConditionsEncounteredResult() {
		return ENABLED;
	}

	@Override
	protected ConditionEvaluationResult evaluate(DisabledIfSystemProperty annotation) {
		String name = annotation.named().strip();
		String regex = annotation.matches();
		Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
		Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);
		String actual = System.getProperty(name);

		// Nothing to match against?
		if (actual == null) {
			return enabled("System property [%s] does not exist".formatted(name));
		}

		if (actual.matches(regex)) {
			return disabled(
				"System property [%s] with value [%s] matches regular expression [%s]".formatted(name, actual, regex),
				annotation.disabledReason());
		}
		// else
		return enabled("System property [%s] with value [%s] does not match regular expression [%s]".formatted(name,
			actual, regex));
	}

}
