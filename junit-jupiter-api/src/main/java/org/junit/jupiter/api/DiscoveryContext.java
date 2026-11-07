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
import org.junit.jupiter.api.store.Namespace;
import org.junit.jupiter.api.store.Store;
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
	 * Get an accessor to session-scoped store for the supplied namespace.
	 *
	 * <p>The store can be used to store information that should be reused in
	 * subsequent executions. For example, when using the Jupiter engine in
	 * multiple Suite classes.
	 *
	 * @param namespace the desired namespace; must not be {@code null} or
	 * empty
	 * @return an accessor to the session-scoped store for the supplied
	 * namespace; never {@code null}
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	default Store getStore(Namespace namespace) {
		throw new JUnitException(
			"Store not available; probably due to unaligned versions of the junit-jupiter-api and junit-jupiter-engine jars on the classpath/module path.");
	}

}
