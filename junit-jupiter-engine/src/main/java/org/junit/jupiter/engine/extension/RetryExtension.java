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
import org.junit.jupiter.api.RetryInfo;
import org.junit.jupiter.api.RetryTest;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * {@code RetryExtension} implements the following extension APIs to support
 * retry attempts of a {@link RetryTest @RetryTest} method.
 *
 * <ul>
 * <li>{@link ParameterResolver} to resolve {@link RetryInfo} arguments</li>
 * <li>{@link TestWatcher} to track successes and failures across attempts</li>
 * <li>{@link ExecutionCondition} to skip remaining attempts after a success</li>
 * <li>{@link BeforeTestExecutionCallback} to apply delay between retry attempts</li>
 * </ul>
 *
 * @since 6.1
 */
class RetryExtension implements ParameterResolver, TestWatcher, ExecutionCondition, BeforeTestExecutionCallback {

	private final DefaultRetryInfo retryInfo;

	RetryExtension(DefaultRetryInfo retryInfo) {
		this.retryInfo = retryInfo;
	}

	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return (parameterContext.getParameter().getType() == RetryInfo.class);
	}

	@Override
	public RetryInfo resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return this.retryInfo;
	}

	@Override
	public void testSuccessful(ExtensionContext context) {
		this.retryInfo.succeeded().set(true);
	}

	@Override
	public void testFailed(ExtensionContext context, @Nullable Throwable cause) {
		this.retryInfo.failureCount().incrementAndGet();
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		if (this.retryInfo.succeeded().get()) {
			return disabled("A previous attempt already succeeded");
		}
		return enabled("No successful attempt yet");
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		long delayMs = this.retryInfo.getDelayMs();
		if (this.retryInfo.currentAttempt() > 1 && delayMs > 0) {
			Thread.sleep(delayMs);
		}
	}

}
