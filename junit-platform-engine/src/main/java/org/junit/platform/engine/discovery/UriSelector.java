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

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects a {@link URI} so that
 * {@link org.junit.platform.engine.TestEngine TestEngines}
 * can discover tests or containers based on URIs.
 *
 * @since 1.0
 * @see DiscoverySelectors#selectUri(String)
 * @see DiscoverySelectors#selectUri(URI)
 * @see FileSelector
 * @see DirectorySelector
 * @see org.junit.platform.engine.support.descriptor.UriSource
 */
@API(status = STABLE, since = "1.0")
public final class UriSelector implements DiscoverySelector {

	private final URI uri;

	UriSelector(URI uri) {
		this.uri = uri;
	}

	/**
	 * Get the selected {@link URI}.
	 */
	public URI getUri() {
		return this.uri;
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
		UriSelector that = (UriSelector) o;
		return Objects.equals(this.uri, that.uri);
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return this.uri.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("uri", this.uri).toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, this.uri.toString()));
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for {@link UriSelector
	 * UriSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "uri";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<UriSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(DiscoverySelectors.selectUri(identifier.getValue()));
		}

	}

}
