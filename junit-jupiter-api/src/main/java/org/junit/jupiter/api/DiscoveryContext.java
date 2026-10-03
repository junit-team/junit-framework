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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;

/**
 * {@code DiscoveryContext} encapsulates the <em>context</em> available to SPIs
 * during test discovery.
 *
 * @since 6.2
 */
@API(status = MAINTAINED, since = "6.2")
public interface DiscoveryContext {

	/**
	 * Get the configuration parameter stored under the specified {@code key}.
	 *
	 * <p>If no such key is present in the {@code ConfigurationParameters} for
	 * the JUnit Platform, an attempt will be made to look up the value as a
	 * JVM system property. If no such system property exists, an attempt will
	 * be made to look up the value in the JUnit Platform properties file.
	 *
	 * @param key the key to look up; never {@code null} or blank
	 * @return an {@code Optional} containing the value; never {@code null}
	 * but potentially empty
	 *
	 * @see System#getProperty(String)
	 * @see org.junit.platform.engine.ConfigurationParameters
	 */
	Optional<String> getConfigurationParameter(String key);

	/**
	 * Get an accessor to session-scoped store for the supplied namespace parts.
	 *
	 * <p>The store can be used to store information that should be reused in
	 * subsequent executions. For example, when using the Jupiter engine in
	 * multiple Suite classes.
	 *
	 * @param namespaceParts the parts to use for the namespace; must not be
	 * {@code null} or empty
	 * @return an accessor to the session-scoped store for the supplied
	 * namespace; never {@code null}
	 * @see org.junit.jupiter.api.extension.ExtensionContext.Namespace
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default StoreAccessor getStoreAccessor(Object... namespaceParts) {
		throw new JUnitException(
			"ExtensionContext.Store not available; probably due to unaligned versions of the junit-jupiter-api and junit-jupiter-engine jars on the classpath/module path.");
	}

	/**
	 * Provides access to the values in a store.
	 *
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	interface StoreAccessor {

		/**
		 * Get the value that is stored under the supplied {@code key}.
		 *
		 * @param key the key; never {@code null}
		 * @return the value; potentially {@code null}
		 * @see org.junit.jupiter.api.extension.ExtensionContext.Store#get(Object)
		 */
		@Nullable
		Object get(Object key);

		/**
		 * Store a {@code value} for later retrieval under the supplied {@code key}.
		 *
		 * @param key the key under which the value should be stored; never
		 * {@code null}
		 * @param value the value to store; may be {@code null}
		 * @see org.junit.jupiter.api.extension.ExtensionContext.Store#put(Object, Object)
		 */
		void put(Object key, @Nullable Object value);

	}

}
