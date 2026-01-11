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

import java.util.Objects;

import org.apiguardian.api.API;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;

/**
 * Identifier for a {@link DiscoverySelector} with a specific prefix.
 *
 * <p>The {@linkplain #toString() string representation} of an identifier is
 * intended to be human-readable and is formatted as {@code prefix:value}.
 *
 * @since 1.11
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#parse(String)
 * @see org.junit.platform.engine.discovery.DiscoverySelectorIdentifierParser
 */
@API(status = MAINTAINED, since = "1.13.3")
public final class DiscoverySelectorIdentifier {

	private final String prefix;
	private final String value;

	/**
	 * Create a new {@code DiscoverySelectorIdentifier} with the supplied prefix and
	 * value.
	 *
	 * @param prefix the prefix; never {@code null} or blank
	 * @param value the value; never {@code null} or blank
	 */
	public static DiscoverySelectorIdentifier create(String prefix, String value) {
		return new DiscoverySelectorIdentifier(prefix, value);
	}

	/**
	 * Parse the supplied string representation of a
	 * {@link DiscoverySelectorIdentifier} in the format {@code prefix:value}.
	 *
	 * @param string the string representation of a {@code DiscoverySelectorIdentifier}
	 * @return the parsed {@code DiscoverySelectorIdentifier}
	 * @throws PreconditionViolationException if the supplied string does not
	 * conform to the expected format
	 */
	public static DiscoverySelectorIdentifier parse(String string) {
		return StringUtils.splitIntoTwo(':', string).mapTwo( //
			() -> new PreconditionViolationException("Identifier string must be 'prefix:value', but was " + string),
			DiscoverySelectorIdentifier::new //
		);
	}

	private DiscoverySelectorIdentifier(String prefix, String value) {
		this.prefix = Preconditions.notBlank(prefix, "prefix must not be blank");
		this.value = Preconditions.notBlank(value, "value must not be blank");
	}

	/**
	 * Get the prefix of this identifier.
	 *
	 * @return the prefix; never {@code null} or blank
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Get the value of this identifier.
	 *
	 * @return the value; never {@code null} or blank
	 */
	public String getValue() {
		return this.value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DiscoverySelectorIdentifier that = (DiscoverySelectorIdentifier) o;
		return Objects.equals(this.prefix, that.prefix) && Objects.equals(this.value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.prefix, this.value);
	}

	/**
	 * Get the string representation of this identifier in the format
	 * {@code prefix:value}.
	 */
	@Override
	public String toString() {
		return "%s:%s".formatted(this.prefix, this.value);
	}

}
