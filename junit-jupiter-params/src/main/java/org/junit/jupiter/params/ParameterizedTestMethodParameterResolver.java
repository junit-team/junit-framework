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
import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @since 5.0
 */
class ParameterizedTestMethodParameterResolver extends ParameterizedInvocationParameterResolver {

	private final Method testTemplateMethod;

	ParameterizedTestMethodParameterResolver(ParameterizedTestContext methodContext, EvaluatedArgumentSet arguments,
			int invocationIndex) {
		super(methodContext.getResolverFacade(), arguments, invocationIndex, ResolutionCache.DISABLED);
		this.testTemplateMethod = methodContext.getAnnotatedElement();
	}

	@Override
	protected boolean isSupportedOnConstructorOrMethod(Executable declaringExecutable,
			ExtensionContext extensionContext) {
		return this.testTemplateMethod.equals(declaringExecutable);
	}

}
