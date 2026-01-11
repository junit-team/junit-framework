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

import java.util.Arrays;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@link ExecutionCondition} for {@link EnabledOnOs @EnabledOnOs}.
 *
 * @since 5.1
 * @see EnabledOnOs
 */
class EnabledOnOsCondition extends AbstractOsBasedExecutionCondition<EnabledOnOs> {

	EnabledOnOsCondition() {
		super(EnabledOnOs.class);
	}

	@Override
	ConditionEvaluationResult evaluateExecutionCondition(EnabledOnOs annotation) {
		boolean osSpecified = annotation.value().length > 0;
		boolean archSpecified = annotation.architectures().length > 0;
		Preconditions.condition(osSpecified || archSpecified,
			"You must declare at least one OS or architecture in @EnabledOnOs");

		boolean enabled = isEnabledBasedOnOs(annotation) && isEnabledBasedOnArchitecture(annotation);
		String reason = createReason(enabled, osSpecified, archSpecified);

		return enabled ? ConditionEvaluationResult.enabled(reason)
				: ConditionEvaluationResult.disabled(reason, annotation.disabledReason());
	}

	private boolean isEnabledBasedOnOs(EnabledOnOs annotation) {
		OS[] operatingSystems = annotation.value();
		if (operatingSystems.length == 0) {
			return true;
		}
		return Arrays.stream(operatingSystems).anyMatch(OS::isCurrentOs);
	}

	private boolean isEnabledBasedOnArchitecture(EnabledOnOs annotation) {
		String[] architectures = annotation.architectures();
		if (architectures.length == 0) {
			return true;
		}
		return Arrays.stream(architectures).anyMatch(CURRENT_ARCHITECTURE::equalsIgnoreCase);
	}

}
