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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Example {@link ParameterResolver} that resolves {@code Map<String, String>} types.
 *
 * @since 5.0
 */
public class MapOfStringsParameterResolver implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Type type = parameterContext.getParameter().getParameterizedType();
		if (!(type instanceof ParameterizedType parameterizedType)) {
			return false;
		}
		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
		if (actualTypeArguments.length != 2) {
			return false;
		}
		return actualTypeArguments[0] == String.class && actualTypeArguments[1] == String.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Map<String, String> map = new TreeMap<>();
		map.put("key", "value");
		return map;
	}

}
