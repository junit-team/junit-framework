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
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects a file so that
 * {@link org.junit.platform.engine.TestEngine TestEngines}
 * can discover tests or containers based on files in the
 * file system.
 *
 * @since 1.0
 * @see DiscoverySelectors#selectFile(String)
 * @see DiscoverySelectors#selectFile(File)
 * @see DirectorySelector
 * @see #getFile()
 * @see #getPath()
 * @see #getRawPath()
 */
@API(status = STABLE, since = "1.0")
public final class FileSelector implements DiscoverySelector {

	private final String path;

	private final @Nullable FilePosition position;

	FileSelector(String path, @Nullable FilePosition position) {
		this.path = path;
		this.position = position;
	}

	/**
	 * Get the selected file as a {@link java.io.File}.
	 *
	 * @see #getPath()
	 * @see #getRawPath()
	 */
	public File getFile() {
		return new File(this.path);
	}

	/**
	 * Get the selected file as a {@link java.nio.file.Path} using the
	 * {@linkplain FileSystems#getDefault default} {@link FileSystem}.
	 *
	 * @see #getFile()
	 * @see #getRawPath()
	 */
	public Path getPath() {
		return Path.of(this.path);
	}

	/**
	 * Get the selected file as a <em>raw path</em>.
	 *
	 * @see #getFile()
	 * @see #getPath()
	 */
	public String getRawPath() {
		return this.path;
	}

	/**
	 * Get the selected position within the file as a {@link FilePosition}.
	 */
	public Optional<FilePosition> getPosition() {
		return Optional.ofNullable(this.position);
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
		FileSelector that = (FileSelector) o;
		return Objects.equals(this.path, that.path) && Objects.equals(this.position, that.position);
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return Objects.hash(this.path, this.position);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("path", this.path).append("position", this.position).toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		if (this.position == null) {
			return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, this.path));
		}
		else {
			return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX,
				"%s?%s".formatted(this.path, this.position.toQueryPart())));
		}
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for {@link FileSelector
	 * FileSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "file";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<FileSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(StringUtils.splitIntoTwo('?', identifier.getValue()).map( //
				DiscoverySelectors::selectFile, //
				(path, query) -> {
					FilePosition position = FilePosition.fromQuery(query).orElse(null);
					return DiscoverySelectors.selectFile(path, position);
				} //
			));
		}

	}

}
