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

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects a directory so that
 * {@link org.junit.platform.engine.TestEngine TestEngines}
 * can discover tests or containers based on directories in the
 * file system.
 *
 * @since 1.0
 * @see DiscoverySelectors#selectDirectory(String)
 * @see DiscoverySelectors#selectDirectory(File)
 * @see FileSelector
 * @see #getDirectory()
 * @see #getPath()
 * @see #getRawPath()
 */
@API(status = STABLE, since = "1.0")
public final class DirectorySelector implements DiscoverySelector {

	private final String path;

	DirectorySelector(String path) {
		this.path = path;
	}

	/**
	 * Get the selected directory as a {@link java.io.File}.
	 *
	 * @see #getPath()
	 * @see #getRawPath()
	 */
	public File getDirectory() {
		return new File(this.path);
	}

	/**
	 * Get the selected directory as a {@link java.nio.file.Path} using the
	 * {@linkplain FileSystems#getDefault default} {@link FileSystem}.
	 *
	 * @see #getDirectory()
	 * @see #getRawPath()
	 */
	public Path getPath() {
		return Path.of(this.path);
	}

	/**
	 * Get the selected directory as a <em>raw path</em>.
	 *
	 * @see #getDirectory()
	 * @see #getPath()
	 */
	public String getRawPath() {
		return this.path;
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
		DirectorySelector that = (DirectorySelector) o;
		return Objects.equals(this.path, that.path);
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("path", this.path).toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, this.path));
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for
	 * {@link DirectorySelector DirectorySelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "directory";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<DirectorySelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(DiscoverySelectors.selectDirectory(identifier.getValue()));
		}

	}

}
