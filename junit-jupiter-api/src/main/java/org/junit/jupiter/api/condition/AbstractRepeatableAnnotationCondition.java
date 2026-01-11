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

import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

/**
 * Abstract base class for {@link ExecutionCondition} implementations that support
 * {@linkplain Repeatable repeatable} annotations.
 *
 * @param <A> the type of repeatable annotation supported by this {@code ExecutionCondition}
 * @since 5.6
 */
abstract class AbstractRepeatableAnnotationCondition<A extends Annotation> implements ExecutionCondition {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Class<A> annotationType;

	AbstractRepeatableAnnotationCondition(Class<A> annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public final ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<AnnotatedElement> optionalElement = context.getElement();
		if (optionalElement.isPresent()) {
			AnnotatedElement annotatedElement = optionalElement.get();
			// @formatter:off
			return findRepeatableAnnotations(annotatedElement, this.annotationType).stream()
					.map(annotation -> {
						ConditionEvaluationResult result = evaluate(annotation);
						logResult(annotation, annotatedElement, result);
						return result;
					})
					.filter(ConditionEvaluationResult::isDisabled)
					.findFirst()
					.orElse(getNoDisabledConditionsEncounteredResult());
			// @formatter:on
		}
		return getNoDisabledConditionsEncounteredResult();
	}

	protected abstract ConditionEvaluationResult evaluate(A annotation);

	protected abstract ConditionEvaluationResult getNoDisabledConditionsEncounteredResult();

	private void logResult(A annotation, AnnotatedElement annotatedElement, ConditionEvaluationResult result) {
		logger.trace(() -> "Evaluation of %s on [%s] resulted in: %s".formatted(annotation, annotatedElement, result));
	}

}
