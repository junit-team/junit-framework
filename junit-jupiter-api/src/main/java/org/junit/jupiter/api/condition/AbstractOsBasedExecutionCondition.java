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

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Base class for OS-based {@link ExecutionCondition} implementations.
 *
 * @since 5.9
 */
abstract class AbstractOsBasedExecutionCondition<A extends Annotation> implements ExecutionCondition {

	static final String CURRENT_ARCHITECTURE = System.getProperty("os.arch");
	static final String CURRENT_OS = System.getProperty("os.name");

	private final Class<A> annotationType;

	AbstractOsBasedExecutionCondition(Class<A> annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return findAnnotation(context.getElement(), this.annotationType) //
				.map(this::evaluateExecutionCondition) //
				.orElseGet(this::enabledByDefault);
	}

	abstract ConditionEvaluationResult evaluateExecutionCondition(A annotation);

	String createReason(boolean enabled, boolean osSpecified, boolean archSpecified) {
		StringBuilder reason = new StringBuilder() //
				.append(enabled ? "Enabled" : "Disabled") //
				.append(osSpecified ? " on operating system: " : " on architecture: ");

		if (osSpecified && archSpecified) {
			reason.append("%s (%s)".formatted(CURRENT_OS, CURRENT_ARCHITECTURE));
		}
		else if (osSpecified) {
			reason.append(CURRENT_OS);
		}
		else {
			reason.append(CURRENT_ARCHITECTURE);
		}

		return reason.toString();
	}

	private ConditionEvaluationResult enabledByDefault() {
		String reason = "@%s is not present".formatted(this.annotationType.getSimpleName());
		return enabled(reason);
	}

}
