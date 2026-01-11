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

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.13
 */
class ArgumentSetLifecycleMethod {

	final Method method;
	final ParameterResolver parameterResolver;

	ArgumentSetLifecycleMethod(Method method) {
		this(method, ParameterResolver.DISABLED);
	}

	ArgumentSetLifecycleMethod(Method method, ParameterResolver parameterResolver) {
		this.method = Preconditions.notNull(method, "method must not be null");
		this.parameterResolver = Preconditions.notNull(parameterResolver, "parameterResolver must not be null");
	}

	interface ParameterResolver {

		ParameterResolver DISABLED = new ParameterResolver() {
			@Override
			public boolean supports(ParameterContext parameterContext) {
				return false;
			}

			@Override
			public Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext,
					EvaluatedArgumentSet arguments, int invocationIndex, ResolutionCache resolutionCache) {
				throw new JUnitException("Parameter resolution is disabled");
			}
		};

		boolean supports(ParameterContext parameterContext);

		@Nullable
		Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex, ResolutionCache resolutionCache);

	}
}
