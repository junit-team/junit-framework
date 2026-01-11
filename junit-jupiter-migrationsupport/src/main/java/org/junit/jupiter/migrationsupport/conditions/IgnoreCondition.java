/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.conditions;

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.reflect.AnnotatedElement;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;

/**
 * {@link ExecutionCondition} that supports JUnit 4's {@link Ignore @Ignore}
 * annotation.
 *
 * @since 5.4
 * @see org.junit.Ignore @Ignore
 * @see org.junit.jupiter.api.Disabled @Disabled
 * @see #evaluateExecutionCondition(ExtensionContext)
 * @see org.junit.jupiter.migrationsupport.EnableJUnit4MigrationSupport
 * @deprecated Please use {@link org.junit.jupiter.api.Disabled @Disabled} instead.
 */
@API(status = DEPRECATED, since = "6.0")
@Deprecated(since = "6.0", forRemoval = true)
public class IgnoreCondition implements ExecutionCondition {

	private static final ConditionEvaluationResult ENABLED = //
		ConditionEvaluationResult.enabled("@org.junit.Ignore is not present");

	public IgnoreCondition() {
	}

	/**
	 * Containers/tests are disabled if {@link Ignore @Ignore} is present on
	 * the test class or method.
	 */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		AnnotatedElement element = context.getElement().orElse(null);
		return findAnnotation(element, Ignore.class) //
				.map(annotation -> toResult(element, annotation)) //
				.orElse(ENABLED);
	}

	private ConditionEvaluationResult toResult(@Nullable AnnotatedElement element, Ignore annotation) {
		String value = annotation.value();
		String reason = StringUtils.isNotBlank(value) ? value : element + " is disabled via @org.junit.Ignore";
		return ConditionEvaluationResult.disabled(reason);
	}

}
