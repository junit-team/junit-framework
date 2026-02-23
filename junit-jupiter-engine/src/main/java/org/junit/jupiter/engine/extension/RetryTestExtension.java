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
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.RetryTest;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code TestTemplateInvocationContextProvider} that supports the
 * {@link RetryTest @RetryTest} annotation.
 *
 * @since 6.1
 */
class RetryTestExtension implements TestTemplateInvocationContextProvider {

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return isAnnotated(context.getTestMethod(), RetryTest.class);
	}

	@Override
	public Stream<RetryTestInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		Method testMethod = context.getRequiredTestMethod();
		String displayName = context.getDisplayName();
		RetryTest retryTest = findAnnotation(testMethod, RetryTest.class).get();
		int maxAttempts = maxAttempts(retryTest, testMethod);
		long delayMs = delayMs(retryTest, testMethod);
		AtomicInteger failureCount = new AtomicInteger();
		AtomicBoolean succeeded = new AtomicBoolean(false);
		RetryTestDisplayNameFormatter formatter = displayNameFormatter(retryTest, testMethod, displayName);

		// @formatter:off
		return IntStream
				.rangeClosed(1, maxAttempts)
				.mapToObj(attempt -> new DefaultRetryInfo(attempt, maxAttempts, delayMs, failureCount, succeeded))
				.map(retryInfo -> new RetryTestInvocationContext(retryInfo, formatter));
		// @formatter:on
	}

	private int maxAttempts(RetryTest retryTest, Method method) {
		int maxAttempts = retryTest.maxAttempts();
		Preconditions.condition(maxAttempts > 0,
			() -> "Configuration error: @RetryTest on method [%s] must be declared with a positive 'maxAttempts'.".formatted(
				method));
		return maxAttempts;
	}

	private long delayMs(RetryTest retryTest, Method method) {
		long delayMs = retryTest.delayMs();
		Preconditions.condition(delayMs >= 0,
			() -> "Configuration error: @RetryTest on method [%s] must be declared with a non-negative 'delayMs'.".formatted(
				method));
		return delayMs;
	}

	private RetryTestDisplayNameFormatter displayNameFormatter(RetryTest retryTest, Method method,
			String displayName) {
		String pattern = Preconditions.notBlank(retryTest.name().strip(),
			() -> "Configuration error: @RetryTest on method [%s] must be declared with a non-empty name.".formatted(
				method));
		return new RetryTestDisplayNameFormatter(pattern, displayName);
	}

}
