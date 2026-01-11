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
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;

/**
 * @since 5.7
 */
abstract class MethodBasedCondition<A extends Annotation> implements ExecutionCondition {

	private final Class<A> annotationType;
	private final Function<A, String> methodName;
	private final Function<A, String> customDisabledReason;

	MethodBasedCondition(Class<A> annotationType, Function<A, String> methodName,
			Function<A, String> customDisabledReason) {
		this.annotationType = annotationType;
		this.methodName = methodName;
		this.customDisabledReason = customDisabledReason;
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<A> annotation = findAnnotation(context.getElement(), this.annotationType);
		return annotation //
				.map(this.methodName) //
				.map(methodName -> getConditionMethod(methodName, context)) //
				.map(method -> invokeConditionMethod(method, context)) //
				.map(methodResult -> buildConditionEvaluationResult(methodResult, annotation.get())) //
				.orElseGet(this::enabledByDefault);
	}

	// package-private for testing
	Method getConditionMethod(String fullyQualifiedMethodName, ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		if (!fullyQualifiedMethodName.contains("#")) {
			return findMethod(testClass, fullyQualifiedMethodName);
		}
		String[] methodParts = ReflectionUtils.parseFullyQualifiedMethodName(fullyQualifiedMethodName);
		String className = methodParts[0];
		String methodName = methodParts[1];
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader(testClass);
		Class<?> clazz = ReflectionSupport.tryToLoadClass(className, classLoader).getNonNullOrThrow(
			cause -> new JUnitException("Could not load class [%s]".formatted(className), cause));
		return findMethod(clazz, methodName);
	}

	private Method findMethod(Class<?> clazz, String methodName) {
		return ReflectionSupport.findMethod(clazz, methodName) //
				.orElseGet(() -> ReflectionUtils.getRequiredMethod(clazz, methodName, ExtensionContext.class));
	}

	private boolean invokeConditionMethod(Method method, ExtensionContext context) {
		Preconditions.condition(method.getReturnType() == boolean.class,
			() -> "Method [%s] must return a boolean".formatted(method));
		Preconditions.condition(acceptsExtensionContextOrNoArguments(method),
			() -> "Method [%s] must accept either an ExtensionContext or no arguments".formatted(method));

		Object testInstance = context.getTestInstance().orElse(null);
		return invokeMethod(method, context, testInstance);
	}

	@SuppressWarnings("DataFlowIssue")
	private static boolean invokeMethod(Method method, ExtensionContext context, @Nullable Object testInstance) {
		if (method.getParameterCount() == 0) {
			return (boolean) ReflectionSupport.invokeMethod(method, testInstance);
		}
		return (boolean) ReflectionSupport.invokeMethod(method, testInstance, context);
	}

	private boolean acceptsExtensionContextOrNoArguments(Method method) {
		int parameterCount = method.getParameterCount();
		return parameterCount == 0 || (parameterCount == 1 && method.getParameterTypes()[0] == ExtensionContext.class);
	}

	private ConditionEvaluationResult buildConditionEvaluationResult(boolean methodResult, A annotation) {
		Supplier<String> defaultReason = () -> "@%s(\"%s\") evaluated to %s".formatted(
			this.annotationType.getSimpleName(), this.methodName.apply(annotation), methodResult);
		if (isEnabled(methodResult)) {
			return enabled(defaultReason.get());
		}
		String customReason = this.customDisabledReason.apply(annotation);
		return StringUtils.isNotBlank(customReason) ? disabled(customReason) : disabled(defaultReason.get());
	}

	protected abstract boolean isEnabled(boolean methodResult);

	private ConditionEvaluationResult enabledByDefault() {
		return enabled("@%s is not present".formatted(this.annotationType.getSimpleName()));
	}

}
