/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;
import org.junit.platform.engine.UniqueId;

/**
 * A {@link DiscoverySelector} that selects a {@link UniqueId} so that
 * {@link org.junit.platform.engine.TestEngine TestEngines} can discover
 * tests or containers based on unique IDs.
 *
 * @since 1.0
 * @see DiscoverySelectors#selectUniqueId(String)
 * @see DiscoverySelectors#selectUniqueId(UniqueId)
 */
@API(status = STABLE, since = "1.0")
public final class UniqueIdSelector implements DiscoverySelector {

	private final UniqueId uniqueId;

	UniqueIdSelector(UniqueId uniqueId) {
		this.uniqueId = uniqueId;
	}

	/**
	 * Get the selected {@link UniqueId}.
	 */
	public UniqueId getUniqueId() {
		return this.uniqueId;
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UniqueIdSelector that = (UniqueIdSelector) o;
		return Objects.equals(this.uniqueId, that.uniqueId);
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return this.uniqueId.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("uniqueId", this.uniqueId).toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, this.uniqueId.toString()));
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for
	 * {@link UniqueIdSelector UniqueIdSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "uid";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<UniqueIdSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(DiscoverySelectors.selectUniqueId(identifier.getValue()));
		}

	}

}
