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

import static org.apiguardian.api.API.Status.STABLE;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apiguardian.api.API;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ResourceUtils;
import org.junit.platform.engine.TestSource;

/**
 * A {@link TestSource} that can be represented as a {@link URI}.
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.UriSelector
 */
@API(status = STABLE, since = "1.0")
public interface UriSource extends TestSource {

	/**
	 * Get the {@link URI} that represents this source.
	 *
	 * @return the source {@code URI}; never {@code null}
	 */
	URI getUri();

	/**
	 * Create a new {@code UriSource} using the supplied {@code URI}.
	 *
	 * <p>This implementation first attempts to resolve the supplied {@code URI}
	 * to a path-based {@code UriSource} in the local filesystem. If that fails
	 * for any reason, an instance of the default {@code UriSource}
	 * implementation storing the supplied {@code URI} <em>as-is</em> will be
	 * returned.
	 *
	 * @param uri the URI to use as the source; never {@code null}
	 * @return an appropriate {@code UriSource} for the supplied {@code URI}
	 * @since 1.3
	 * @see org.junit.platform.engine.support.descriptor.FileSource
	 * @see org.junit.platform.engine.support.descriptor.DirectorySource
	 */
	static UriSource from(URI uri) {
		Preconditions.notNull(uri, "URI must not be null");

		try {
			URI uriWithoutQuery = ResourceUtils.stripQueryComponent(uri);
			Path path = Path.of(uriWithoutQuery);
			if (Files.isRegularFile(path)) {
				return FileSource.from(path.toFile(), FilePosition.fromQuery(uri.getQuery()).orElse(null));
			}
			if (Files.isDirectory(path)) {
				return DirectorySource.from(path.toFile());
			}
		}
		catch (RuntimeException ex) {
			LoggerFactory.getLogger(UriSource.class).debug(ex,
				() -> "The supplied URI [%s] is not path-based. Falling back to default UriSource implementation.".formatted(
					uri));
		}

		// Store supplied URI as-is
		return new DefaultUriSource(uri);
	}

}
