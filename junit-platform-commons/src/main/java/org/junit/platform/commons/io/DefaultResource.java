/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.io;

import java.net.URI;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.annotation.Contract;

/**
 * Default implementation of {@link Resource}.
 *
 * @since 1.14
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof org.junit.platform.commons.io.Resource that) {
			return this.name.equals(that.getName()) //
					&& this.uri.equals(that.getUri());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, uri);
	}

	// Cannot use Preconditions due to package cycle
	@Contract("null, _ -> fail; !null, _ -> param1")
	private static <T> void checkNotNull(@Nullable T input, String title) {
		if (input == null) {
			throw new PreconditionViolationException(title + " must not be null");
		}
	}

}
