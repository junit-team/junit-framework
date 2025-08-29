/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import java.net.URI;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of {@link Resource}.
 *
 * @since 1.11
 */
@SuppressWarnings("removal")
record DefaultResource(String name, URI uri) implements Resource {

	public DefaultResource {
		Preconditions.notNull(name, "name must not be null");
		Preconditions.notNull(uri, "uri must not be null");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("name", name) //
				.append("uri", uri) //
				.toString();
	}

}
