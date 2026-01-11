/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.reflect.AnnotatedElement;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;

/**
 * {@link ExecutionCondition} that supports the {@code @Disabled} annotation.
 *
 * @since 5.0
 * @see Disabled
 * @see #evaluateExecutionCondition(ExtensionContext)
 */
class DisabledCondition implements ExecutionCondition {

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled(
		"@Disabled is not present");

	/**
	 * Containers/tests are disabled if {@code @Disabled} is present on the test
	 * class or method.
	 */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		AnnotatedElement element = context.getElement().orElse(null);
		return findAnnotation(element, Disabled.class) //
				.map(annotation -> toResult(element, annotation)) //
				.orElse(ENABLED);
	}

	private ConditionEvaluationResult toResult(@Nullable AnnotatedElement element, Disabled annotation) {
		String value = annotation.value();
		String reason = StringUtils.isNotBlank(value) ? value : element + " is @Disabled";
		return ConditionEvaluationResult.disabled(reason);
	}

}
