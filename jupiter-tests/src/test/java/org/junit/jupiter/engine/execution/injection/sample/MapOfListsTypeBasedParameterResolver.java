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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

/**
 * @since 5.6
 */
public class MapOfListsTypeBasedParameterResolver extends TypeBasedParameterResolver<Map<String, List<Integer>>> {

	@Override
	public Map<String, List<Integer>> resolveParameter(ParameterContext parameterContext,
			ExtensionContext extensionContext) {

		return Map.of("ids", List.of(1, 42));
	}

}
