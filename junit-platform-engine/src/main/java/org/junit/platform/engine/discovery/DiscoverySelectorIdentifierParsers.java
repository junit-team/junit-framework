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

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * Utility class for parsing {@link DiscoverySelectorIdentifier
 * DiscoverySelectorIdentifiers}.
 *
 * @since 1.11
 */
class DiscoverySelectorIdentifierParsers {

	static Stream<? extends DiscoverySelector> parseAll(String... identifiers) {
		Preconditions.notNull(identifiers, "identifiers must not be null");
		return Stream.of(identifiers) //
				.map(DiscoverySelectorIdentifierParsers::parse) //
				.flatMap(Optional::stream);
	}

	static Stream<? extends DiscoverySelector> parseAll(Collection<DiscoverySelectorIdentifier> identifiers) {
		Preconditions.notNull(identifiers, "identifiers must not be null");
		return identifiers.stream() //
				.map(DiscoverySelectorIdentifierParsers::parse) //
				.flatMap(Optional::stream);
	}

	static Optional<? extends DiscoverySelector> parse(String identifier) {
		Preconditions.notNull(identifier, "identifier must not be null");
		return parse(DiscoverySelectorIdentifier.parse(identifier));
	}

	static Optional<? extends DiscoverySelector> parse(DiscoverySelectorIdentifier identifier) {
		Preconditions.notNull(identifier, "identifier must not be null");
		DiscoverySelectorIdentifierParser parser = Preconditions.notNull(
			Singleton.INSTANCE.parsersByPrefix.get(identifier.getPrefix()),
			"No parser for prefix: " + identifier.getPrefix());

		return parser.parse(identifier, DiscoverySelectorIdentifierParsers::parse);
	}

	private enum Singleton {

		INSTANCE;

		private final Map<String, DiscoverySelectorIdentifierParser> parsersByPrefix;

		Singleton() {
			Map<String, DiscoverySelectorIdentifierParser> parsersByPrefix = new HashMap<>();
			Iterable<DiscoverySelectorIdentifierParser> loadedParsers = ServiceLoader.load(
				DiscoverySelectorIdentifierParser.class, ClassLoaderUtils.getDefaultClassLoader());
			for (DiscoverySelectorIdentifierParser parser : loadedParsers) {
				DiscoverySelectorIdentifierParser previous = parsersByPrefix.put(parser.getPrefix(), parser);
				Preconditions.condition(previous == null,
					() -> "Duplicate parser for prefix: [%s]; candidate a: [%s]; candidate b: [%s]".formatted(
						parser.getPrefix(), requireNonNull(previous).getClass().getName(),
						parser.getClass().getName()));
			}
			this.parsersByPrefix = unmodifiableMap(parsersByPrefix);
		}

	}

	private DiscoverySelectorIdentifierParsers() {
	}

}
