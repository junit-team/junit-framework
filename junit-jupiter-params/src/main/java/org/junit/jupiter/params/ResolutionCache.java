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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.support.ParameterDeclaration;

/**
 * @since 5.13
 */
interface ResolutionCache {

	static ResolutionCache enabled() {
		return new Concurrent();
	}

	ResolutionCache DISABLED = (__, resolver) -> resolver.get();

	@Nullable
	Object resolve(ParameterDeclaration declaration, Supplier<?> resolver);

	class Concurrent implements ResolutionCache {

		private final Map<ParameterDeclaration, Object> cache = new ConcurrentHashMap<>();

		@Override
		public @Nullable Object resolve(ParameterDeclaration declaration, Supplier<?> resolver) {
			return cache.computeIfAbsent(declaration, __ -> resolver.get());
		}
	}
}
