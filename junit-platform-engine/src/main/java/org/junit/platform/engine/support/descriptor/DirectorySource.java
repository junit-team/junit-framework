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

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.net.URI;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Directory based {@link org.junit.platform.engine.TestSource}.
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.DirectorySelector
 */
@API(status = STABLE, since = "1.0")
public final class DirectorySource implements FileSystemSource {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@code DirectorySource} using the supplied
	 * {@linkplain File directory}.
	 *
	 * @param directory the source directory; must not be {@code null}
	 */
	public static DirectorySource from(File directory) {
		return new DirectorySource(directory);
	}

	private final File directory;

	private DirectorySource(File directory) {
		Preconditions.notNull(directory, "directory must not be null");
		try {
			this.directory = directory.getCanonicalFile();
		}
		catch (IOException ex) {
			throw new JUnitException("Failed to retrieve canonical path for directory: " + directory, ex);
		}
	}

	/**
	 * Get the {@link URI} for the source {@linkplain #getFile directory}.
	 *
	 * @return the source {@code URI}; never {@code null}
	 */
	@Override
	public URI getUri() {
		return getFile().toURI();
	}

	/**
	 * Get the source {@linkplain File directory}.
	 *
	 * @return the source directory; never {@code null}
	 */
	@Override
	public File getFile() {
		return this.directory;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DirectorySource that = (DirectorySource) o;
		return this.directory.equals(that.directory);
	}

	@Override
	public int hashCode() {
		return this.directory.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("directory", this.directory).toString();
	}

}
