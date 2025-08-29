/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.io;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.annotation.Contract;

/**
 * Default implementation of {@link Resource}.
 *
 * @since 6.0
 */
record DefaultResource(String name, URI uri) implements Resource {

	DefaultResource {
		checkNotNull(name, "name");
		checkNotNull(uri, "uri");
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public URI getUri() {
		return this.uri;
	}

	// Cannot use Preconditions due to package cycle
	@Contract("null, _ -> fail; !null, _ -> param1")
	private static <T> void checkNotNull(@Nullable T input, String title) {
		if (input == null) {
			throw new PreconditionViolationException(title + " must not be null");
		}
	}

}
