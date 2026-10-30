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

import org.junit.jupiter.api.store.Namespace;
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

	private static final Namespace NAMESPACE = Namespace.create(RandomOrdererUtils.class);

	static long getSeed(DiscoveryContext context, Logger logger) {
		// Use the session-scoped store to log parse failures and used seeds only once per session.
		// Configured values are stored under String keys, logged seeds under Long keys.
		var store = context.getStore(NAMESPACE);
		long seed = context.getConfigurationParameter(RANDOM_SEED_PROPERTY_NAME) //
				.map(value -> store.computeIfAbsent(value, __ -> parseCustomSeed(value, logger), Long.class)) //
				.orElse(DEFAULT_SEED);
		store.computeIfAbsent(seed, __ -> {
			logger.config(() -> """
					Using seed [%d] for ordering classes/methods. \
					To reuse the same seed in a subsequent execution, \
					pass '%s=%1$d' as configuration parameter.\
					""".formatted(seed, RANDOM_SEED_PROPERTY_NAME));
			return true;
		});
		return seed;
	}

	private static long parseCustomSeed(String configurationParameter, Logger logger) {
		try {
			return Long.parseLong(configurationParameter);
		}
		catch (NumberFormatException ex) {
			logger.warn(ex, () -> """
					Failed to convert configuration parameter [%s] with value [%s] to a long. \
					Using default seed [%s] as fallback.""".formatted(RANDOM_SEED_PROPERTY_NAME, configurationParameter,
				DEFAULT_SEED));
			return DEFAULT_SEED;
		}
	}

	private RandomOrdererUtils() {
	}
}
