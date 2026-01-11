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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @since 5.13
 */
class ClassTemplateConstructorParameterResolver extends ParameterizedInvocationParameterResolver {

	private final Class<?> classTemplateClass;

	ClassTemplateConstructorParameterResolver(ParameterizedClassContext classContext, EvaluatedArgumentSet arguments,
			int invocationIndex, ResolutionCache resolutionCache) {
		super(classContext.getResolverFacade(), arguments, invocationIndex, resolutionCache);
		this.classTemplateClass = classContext.getAnnotatedElement();
	}

	@Override
	protected boolean isSupportedOnConstructorOrMethod(Executable declaringExecutable,
			ExtensionContext extensionContext) {
		return declaringExecutable instanceof Constructor //
				&& this.classTemplateClass.equals(declaringExecutable.getDeclaringClass());
	}

}
