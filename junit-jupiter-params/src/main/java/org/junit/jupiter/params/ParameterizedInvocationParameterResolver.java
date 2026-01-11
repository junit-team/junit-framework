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

import java.lang.reflect.Executable;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @since 5.13
 */
abstract class ParameterizedInvocationParameterResolver implements ParameterResolver {

	private final ResolverFacade resolverFacade;
	private final EvaluatedArgumentSet arguments;
	private final int invocationIndex;
	private final ResolutionCache resolutionCache;

	ParameterizedInvocationParameterResolver(ResolverFacade resolverFacade, EvaluatedArgumentSet arguments,
			int invocationIndex, ResolutionCache resolutionCache) {

		this.resolverFacade = resolverFacade;
		this.arguments = arguments;
		this.invocationIndex = invocationIndex;
		this.resolutionCache = resolutionCache;
	}

	@Override
	public final ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	@Override
	public final boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {

		return isSupportedOnConstructorOrMethod(parameterContext.getDeclaringExecutable(), extensionContext) //
				&& this.resolverFacade.isSupportedParameter(parameterContext, this.arguments);

	}

	@Override
	public final @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {

		return this.resolverFacade.resolve(parameterContext, extensionContext, this.arguments, this.invocationIndex,
			this.resolutionCache);
	}

	protected abstract boolean isSupportedOnConstructorOrMethod(Executable declaringExecutable,
			ExtensionContext extensionContext);

}
