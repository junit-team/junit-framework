/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution.injection.sample;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Example {@link ParameterResolver} that always resolves a {@link Long}
 * parameter to {@code 42}.
 *
 * <p>This resolver also <em>attempts</em> to support generic parameter type
 * declarations if the generic type is defined at the class level in a
 * superclass or interface (extended or implemented by the test class) and
 * is assignable from {@link Long}.
 *
 * @since 5.0
 */
public class LongParameterResolver implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		// Exact match?
		if (parameterContext.getParameter().getType() == Long.class) {
			return true;
		}

		Type typeInMethod = parameterContext.getParameter().getParameterizedType();

		// Type variables in parameterized class
		for (TypeVariable<?> typeVariable : parameterContext.getDeclaringExecutable().getDeclaringClass().getTypeParameters()) {
			boolean namesMatch = typeInMethod.getTypeName().equals(typeVariable.getName());
			boolean typesAreCompatible = typeVariable.getBounds().length == 1 && //
					typeVariable.getBounds()[0] instanceof Class<?> clazz && //
					clazz.isAssignableFrom(Long.class);

			if (namesMatch && typesAreCompatible) {
				return true;
			}
		}

		return false;

	}

	@Override
	public Long resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return 42L;
	}

}
