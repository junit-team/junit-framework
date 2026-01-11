/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static java.util.Objects.requireNonNull;

import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @since 5.13
 */
abstract class AbstractParameterizedClassInvocationLifecycleMethodInvoker implements ParameterResolver {

	private final ParameterizedClassContext declarationContext;
	private final EvaluatedArgumentSet arguments;
	private final int invocationIndex;
	private final ResolutionCache resolutionCache;
	private final ArgumentSetLifecycleMethod lifecycleMethod;

	AbstractParameterizedClassInvocationLifecycleMethodInvoker(ParameterizedClassContext declarationContext,
			EvaluatedArgumentSet arguments, int invocationIndex, ResolutionCache resolutionCache,
			ArgumentSetLifecycleMethod lifecycleMethod) {
		this.declarationContext = declarationContext;
		this.arguments = arguments;
		this.invocationIndex = invocationIndex;
		this.resolutionCache = resolutionCache;
		this.lifecycleMethod = lifecycleMethod;
	}

	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return parameterContext.getDeclaringExecutable().equals(this.lifecycleMethod.method) //
				&& this.lifecycleMethod.parameterResolver.supports(parameterContext);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return requireNonNull(this.lifecycleMethod.parameterResolver //
				.resolve(parameterContext, extensionContext, this.arguments, this.invocationIndex,
					this.resolutionCache));
	}

	protected void invoke(ExtensionContext context) {
		if (isCorrectTestClass(context)) {
			ExecutableInvoker executableInvoker = context.getExecutableInvoker();
			Object testInstance = context.getTestInstance().orElse(null);
			executableInvoker.invoke(this.lifecycleMethod.method, testInstance);
		}
	}

	private boolean isCorrectTestClass(ExtensionContext context) {
		return this.declarationContext.getAnnotatedElement().equals(context.getTestClass().orElse(null));
	}

}
