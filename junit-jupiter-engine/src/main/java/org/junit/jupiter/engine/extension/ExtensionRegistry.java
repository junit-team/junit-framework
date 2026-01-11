/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.stream.Collectors.toCollection;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.Extension;

/**
 * An {@code ExtensionRegistry} holds all registered extensions (i.e.
 * instances of {@link Extension}) for a given
 * {@link org.junit.platform.engine.support.hierarchical.Node}.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public interface ExtensionRegistry {

	/**
	 * Stream all {@code Extensions} of the specified type that are present
	 * in this registry or one of its ancestors.
	 *
	 * @param extensionType the type of {@link Extension} to stream
	 * @see #getExtensions(Class)
	 */
	<E extends Extension> Stream<E> stream(Class<E> extensionType);

	/**
	 * Get all {@code Extensions} of the specified type that are present
	 * in this registry or one of its ancestors.
	 *
	 * @param extensionType the type of {@link Extension} to get
	 * @see #stream(Class)
	 */
	default <E extends Extension> List<E> getExtensions(Class<E> extensionType) {
		return stream(extensionType).collect(toCollection(ArrayList::new));
	}

}
