/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension.sub;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Intentionally in a subpackage in order to properly test deactivation
 * of conditions based on patterns. In other words, we do not want this
 * condition declared in the same package as the
 * {@link org.junit.jupiter.engine.extension.DisabledCondition}
 *
 * @since 5.0
 */
public class SystemPropertyCondition implements ExecutionCondition {

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith(SystemPropertyCondition.class)
	public @interface SystemProperty {

		String key();

		String value();
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<SystemProperty> optional = findAnnotation(context.getElement(), SystemProperty.class);

		if (optional.isPresent()) {
			SystemProperty systemProperty = optional.get();
			String key = systemProperty.key();
			String expected = systemProperty.value();
			String actual = System.getProperty(key);

			if (!Objects.equals(expected, actual)) {
				return ConditionEvaluationResult.disabled(
					"System property [%s] has a value of [%s] instead of [%s]".formatted(key, actual, expected));
			}
		}

		return ConditionEvaluationResult.enabled("@SystemProperty is not present");
	}

}
