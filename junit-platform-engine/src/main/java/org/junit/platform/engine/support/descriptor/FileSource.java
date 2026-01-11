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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * File based {@link org.junit.platform.engine.TestSource} with an optional
 * {@linkplain FilePosition position}.
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.FileSelector
 */
@API(status = STABLE, since = "1.0")
public final class FileSource implements FileSystemSource {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@code FileSource} using the supplied {@link File file}.
	 *
	 * @param file the source file; must not be {@code null}
	 */
	public static FileSource from(File file) {
		return new FileSource(file);
	}

	/**
	 * Create a new {@code FileSource} using the supplied {@link File file} and
	 * {@link FilePosition filePosition}.
	 *
	 * @param file the source file; must not be {@code null}
	 * @param filePosition the position in the source file; may be {@code null}
	 * @see #withPosition(FilePosition)
	 */
	public static FileSource from(File file, @Nullable FilePosition filePosition) {
		return new FileSource(file, filePosition);
	}

	private final File file;

	private final @Nullable FilePosition filePosition;

	private FileSource(File file) {
		this(file, null);
	}

	private FileSource(File file, @Nullable FilePosition filePosition) {
		Preconditions.notNull(file, "file must not be null");
		try {
			this.file = file.getCanonicalFile();
		}
		catch (IOException ex) {
			throw new JUnitException("Failed to retrieve canonical path for file: " + file, ex);
		}
		this.filePosition = filePosition;
	}

	private FileSource(FileSource fileSource, @Nullable FilePosition filePosition) {
		this.file = fileSource.file;
		this.filePosition = filePosition;
	}

	/**
	 * Get the {@link URI} for the source {@linkplain #getFile file}.
	 *
	 * @return the source {@code URI}; never {@code null}
	 */
	@Override
	public URI getUri() {
		return getFile().toURI();
	}

	/**
	 * Get the source {@linkplain File file}.
	 *
	 * @return the source file; never {@code null}
	 */
	@Override
	public File getFile() {
		return this.file;
	}

	/**
	 * Get the {@link FilePosition}, if available.
	 */
	public Optional<FilePosition> getPosition() {
		return Optional.ofNullable(this.filePosition);
	}

	/**
	 * {@return a {@code FileSource} based on this instance but with the
	 * supplied {@link FilePosition}}
	 *
	 * <p>If the supplied {@code FilePosition}
	 * {@linkplain Objects#equals(Object, Object) equals} the existing one, this
	 * method returns {@code this}. Otherwise, a new instance is created and
	 * returned.
	 *
	 * <p>Calling this method rather than creating a new {@code FileSource} via
	 * {@link #from(File, FilePosition)} avoids the overhead of redundant
	 * canonical path resolution.
	 *
	 * @param filePosition the position in the source file; may be {@code null}
	 * @since 1.14
	 */
	@API(status = EXPERIMENTAL, since = "1.14")
	public FileSource withPosition(@Nullable FilePosition filePosition) {
		if (Objects.equals(this.filePosition, filePosition)) {
			return this;
		}
		return new FileSource(this, filePosition);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FileSource that = (FileSource) o;
		return Objects.equals(this.file, that.file) //
				&& Objects.equals(this.filePosition, that.filePosition);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.file, this.filePosition);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("file", this.file)
				.append("filePosition", this.filePosition)
				.toString();
		// @formatter:on
	}

}
