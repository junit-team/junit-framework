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

	static Long getSeed(Function<String, Optional<String>> configurationParameterLookup, Logger logger) {
		return getCustomSeed(configurationParameterLookup, logger).orElse(DEFAULT_SEED);
	}

	private static Optional<Long> getCustomSeed(Function<String, Optional<String>> configurationParameterLookup,
			Logger logger) {
		return configurationParameterLookup.apply(RANDOM_SEED_PROPERTY_NAME).map(configurationParameter -> {
			try {
				logger.config(() -> "Using custom seed for configuration parameter [%s] with value [%s].".formatted(
					RANDOM_SEED_PROPERTY_NAME, configurationParameter));
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
