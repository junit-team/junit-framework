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

import java.io.Serial;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ResourceUtils;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestSource;

/**
 * <em>Classpath resource</em> based {@link org.junit.platform.engine.TestSource}
 * with an optional {@linkplain FilePosition position}.
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.ClasspathResourceSelector
 */
@API(status = STABLE, since = "1.0")
public final class ClasspathResourceSource implements TestSource {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * {@link URI} {@linkplain URI#getScheme() scheme} for classpath
	 * resources: {@value}
	 *
	 * @since 1.3
	 */
	public static final String CLASSPATH_SCHEME = "classpath";

	/**
	 * Create a new {@code ClasspathResourceSource} using the supplied classpath
	 * resource name.
	 *
	 * <p>The name of a <em>classpath resource</em> must follow the semantics
	 * for resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * <p>If the supplied classpath resource name is prefixed with a slash
	 * ({@code /}), the slash will be removed.
	 *
	 * @param classpathResourceName the name of the classpath resource; never
	 * {@code null} or blank
	 * @see ClassLoader#getResource(String)
	 * @see ClassLoader#getResourceAsStream(String)
	 * @see ClassLoader#getResources(String)
	 */
	public static ClasspathResourceSource from(String classpathResourceName) {
		return new ClasspathResourceSource(classpathResourceName);
	}

	/**
	 * Create a new {@code ClasspathResourceSource} using the supplied classpath
	 * resource name and {@link FilePosition}.
	 *
	 * <p>The name of a <em>classpath resource</em> must follow the semantics
	 * for resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * <p>If the supplied classpath resource name is prefixed with a slash
	 * ({@code /}), the slash will be removed.
	 *
	 * @param classpathResourceName the name of the classpath resource; never
	 * {@code null} or blank
	 * @param filePosition the position in the classpath resource; may be {@code null}
	 */
	public static ClasspathResourceSource from(String classpathResourceName, @Nullable FilePosition filePosition) {
		return new ClasspathResourceSource(classpathResourceName, filePosition);
	}

	/**
	 * Create a new {@code ClasspathResourceSource} from the supplied {@link URI}.
	 *
	 * <p>The {@link URI#getPath() path} component of the {@code URI} (excluding
	 * the query) will be used as the classpath resource name. The
	 * {@linkplain URI#getQuery() query} component of the {@code URI}, if present,
	 * will be used to retrieve the {@link FilePosition} via
	 * {@link FilePosition#fromQuery(String)}.
	 *
	 * @param uri the {@code URI} for the classpath resource; never {@code null}
	 * @return a new {@code ClasspathResourceSource}; never {@code null}
	 * @throws PreconditionViolationException if the supplied {@code URI} is
	 * {@code null} or if the scheme of the supplied {@code URI} is not equal
	 * to the {@link #CLASSPATH_SCHEME}
	 * @since 1.3
	 * @see #CLASSPATH_SCHEME
	 */
	@API(status = STABLE, since = "1.3")
	public static ClasspathResourceSource from(URI uri) {
		Preconditions.notNull(uri, "URI must not be null");
		Preconditions.condition(CLASSPATH_SCHEME.equals(uri.getScheme()),
			() -> "URI [" + uri + "] must have [" + CLASSPATH_SCHEME + "] scheme");

		String classpathResource = ResourceUtils.stripQueryComponent(uri).getPath();
		FilePosition filePosition = FilePosition.fromQuery(uri.getQuery()).orElse(null);
		return ClasspathResourceSource.from(classpathResource, filePosition);
	}

	private final String classpathResourceName;

	private final @Nullable FilePosition filePosition;

	private ClasspathResourceSource(String classpathResourceName) {
		this(classpathResourceName, null);
	}

	private ClasspathResourceSource(String classpathResourceName, @Nullable FilePosition filePosition) {
		Preconditions.notBlank(classpathResourceName, "Classpath resource name must not be null or blank");
		boolean startsWithSlash = classpathResourceName.startsWith("/");
		this.classpathResourceName = (startsWithSlash ? classpathResourceName.substring(1) : classpathResourceName);
		this.filePosition = filePosition;
	}

	/**
	 * Get the name of the source <em>classpath resource</em>.
	 *
	 * <p>The name of a <em>classpath resource</em> follows the semantics for
	 * resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * @see ClassLoader#getResource(String)
	 * @see ClassLoader#getResourceAsStream(String)
	 * @see ClassLoader#getResources(String)
	 */
	public String getClasspathResourceName() {
		return this.classpathResourceName;
	}

	/**
	 * Get the {@link FilePosition}, if available.
	 */
	public Optional<FilePosition> getPosition() {
		return Optional.ofNullable(this.filePosition);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClasspathResourceSource that = (ClasspathResourceSource) o;
		return Objects.equals(this.classpathResourceName, that.classpathResourceName)
				&& Objects.equals(this.filePosition, that.filePosition);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.classpathResourceName, this.filePosition);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("classpathResourceName", this.classpathResourceName)
				.append("filePosition", this.filePosition)
				.toString();
		// @formatter:on
	}

}
