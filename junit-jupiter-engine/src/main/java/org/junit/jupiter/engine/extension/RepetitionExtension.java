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

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * {@code RepetitionExtension} implements the following extension APIs to support
 * repetitions of a {@link RepeatedTest @RepeatedTest} method.
 *
 * <ul>
 * <li>{@link ParameterResolver} to resolve {@link RepetitionInfo} arguments</li>
 * <li>{@link TestWatcher} to track the {@linkplain RepetitionInfo#getFailureCount()
 * failure count}</li>
 * <li>{@link ExecutionCondition} to disable the repetition if the
 * {@linkplain RepetitionInfo#getFailureThreshold() failure threshold} has been
 * exceeded</li>
 * </ul>
 *
 * @since 5.0
 */
class RepetitionExtension implements ParameterResolver, TestWatcher, ExecutionCondition {

	private final DefaultRepetitionInfo repetitionInfo;

	RepetitionExtension(DefaultRepetitionInfo repetitionInfo) {
		this.repetitionInfo = repetitionInfo;
	}

	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return (parameterContext.getParameter().getType() == RepetitionInfo.class);
	}

	@Override
	public RepetitionInfo resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return this.repetitionInfo;
	}

	@Override
	public void testFailed(ExtensionContext context, @Nullable Throwable cause) {
		this.repetitionInfo.failureCount().incrementAndGet();
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		int failureThreshold = this.repetitionInfo.getFailureThreshold();
		if (this.repetitionInfo.getFailureCount() >= failureThreshold) {
			return disabled("Failure threshold [" + failureThreshold + "] exceeded");
		}
		return enabled("Failure threshold not exceeded");
	}

}
