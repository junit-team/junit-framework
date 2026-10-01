/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.logging.Logger;

/**
 * Shared utility methods for ordering test classes and test methods randomly.
 *
 * @since 5.11
 * @see ClassOrderer.Random
 * @see MethodOrderer.Random
 */
class RandomOrdererUtils {

	static final String RANDOM_SEED_PROPERTY_NAME = "junit.jupiter.execution.order.random.seed";

	static final long DEFAULT_SEED = System.nanoTime();

	static long getSeed(Function<String, Optional<String>> configurationParameterLookup, Function<ExtensionContext.Namespace, ExtensionContext.Store> storeLookup, Logger logger) {
		var seed = getCustomSeed(configurationParameterLookup, logger).orElse(DEFAULT_SEED);
		var store = storeLookup.apply(ExtensionContext.Namespace.create(RandomOrdererUtils.class.getName()));
		var key = String.valueOf(seed);
		if (store.get(key) == null) {
			logger.config(() -> "Using seed [%d] for ordering classes/methods. To reuse the same seed in a subsequent execution, please pass '%s=%1$d' as configuration parameter".formatted(
					seed, RANDOM_SEED_PROPERTY_NAME));
			store.put(key, true);
		}
		return seed;
	}

	private static Optional<Long> getCustomSeed(Function<String, Optional<String>> configurationParameterLookup, Logger logger) {
		return configurationParameterLookup.apply(RANDOM_SEED_PROPERTY_NAME).map(configurationParameter -> {
			try {
				return Long.valueOf(configurationParameter);
			}
			catch (NumberFormatException ex) {
				logger.warn(ex, () -> """
						Failed to convert configuration parameter [%s] with value [%s] to a long. \
						Using default seed [%s] as fallback.""".formatted(RANDOM_SEED_PROPERTY_NAME,
					configurationParameter, DEFAULT_SEED));
				return null;
			}
		});
	}

	private RandomOrdererUtils() {
	}
}
