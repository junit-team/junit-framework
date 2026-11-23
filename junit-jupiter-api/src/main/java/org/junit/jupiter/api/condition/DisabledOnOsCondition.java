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
import org.junit.platform.commons.util.StringUtils;

/**
 * {@link ExecutionCondition} for {@link DisabledOnOs @DisabledOnOs}.
 *
 * @since 5.1
 * @see DisabledOnOs
 */
class DisabledOnOsCondition extends AbstractOsBasedExecutionCondition<DisabledOnOs> {

	DisabledOnOsCondition() {
		super(DisabledOnOs.class);
	}

	@Override
	ConditionEvaluationResult evaluateExecutionCondition(DisabledOnOs annotation) {
		boolean osValuesSpecified = annotation.value().length > 0;
		boolean osNameSpecified = StringUtils.isNotBlank(annotation.name());
		boolean archSpecified = annotation.architectures().length > 0;
		Preconditions.condition(osValuesSpecified || osNameSpecified || archSpecified,
			"You must declare at least one OS or architecture in @DisabledOnOs");
		Preconditions.condition(!(osValuesSpecified && osNameSpecified),
			"@DisabledOnOs must be configured with either OS enum constants or an OS name, but not both");

		boolean osSpecified = osValuesSpecified || osNameSpecified;
		boolean enabled = isEnabledBasedOnOs(annotation) || isEnabledBasedOnArchitecture(annotation);
		String reason = createReason(enabled, osSpecified, archSpecified);

		return enabled ? ConditionEvaluationResult.enabled(reason)
				: ConditionEvaluationResult.disabled(reason, annotation.disabledReason());
	}

	private boolean isEnabledBasedOnOs(DisabledOnOs annotation) {
		OS[] operatingSystems = annotation.value();
		if (operatingSystems.length > 0) {
			return Arrays.stream(operatingSystems).noneMatch(OS::isCurrentOs);
		}
		String name = annotation.name();
		if (StringUtils.isNotBlank(name)) {
			return !isCurrentOsName(name);
		}
		return false;
	}

	private boolean isEnabledBasedOnArchitecture(DisabledOnOs annotation) {
		String[] architectures = annotation.architectures();
		if (architectures.length == 0) {
			return false;
		}
		return Arrays.stream(architectures).noneMatch(CURRENT_ARCHITECTURE::equalsIgnoreCase);
	}

}
