/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import java.io.Serial;
import java.net.URI;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of {@link UriSource}.
 *
 * @since 1.3
 */
record DefaultUriSource(URI uri) implements UriSource {

	@Serial
	private static final long serialVersionUID = 1L;

	DefaultUriSource {
		Preconditions.notNull(uri, "URI must not be null");
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("uri", this.uri).toString();
	}

}
