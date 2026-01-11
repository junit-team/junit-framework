/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.engine.discovery.DiscoverySelectorIdentifierParser;

/**
 * A selector defines what a {@link TestEngine} can use to discover tests
 * &mdash; for example, the name of a Java class, the path to a file or
 * directory, etc.
 *
 * @since 1.0
 * @see EngineDiscoveryRequest
 * @see org.junit.platform.engine.discovery.DiscoverySelectors
 */
@API(status = STABLE, since = "1.0")
public interface DiscoverySelector {

	/**
	 * Return the {@linkplain DiscoverySelectorIdentifier identifier} of this
	 * selector.
	 *
	 * <p>The returned identifier must be parsable by a corresponding
	 * {@link DiscoverySelectorIdentifierParser}.
	 *
	 * <p>The default implementation returns {@link Optional#empty()}. Can be
	 * overridden by concrete implementations.
	 *
	 * @return an {@link Optional} containing the identifier of this selector;
	 * never {@code null} but potentially empty if the selector does not support
	 * identifiers
	 * @since 1.11
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	default Optional<DiscoverySelectorIdentifier> toIdentifier() {
		return Optional.empty();
	}

}
