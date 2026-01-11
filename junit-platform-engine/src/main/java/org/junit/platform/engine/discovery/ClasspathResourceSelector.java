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

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.support.ResourceSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects the name of a <em>classpath resource</em>
 * so that {@link org.junit.platform.engine.TestEngine TestEngines} can load resources
 * from the classpath &mdash; for example, to load XML or JSON files from the classpath,
 * potentially within JARs.
 *
 * <p>Since {@linkplain org.junit.platform.engine.TestEngine engines} are not
 * expected to modify the classpath, the classpath resource represented by this
 * selector must be on the classpath of the
 * {@linkplain Thread#getContextClassLoader() context class loader} of the
 * {@linkplain Thread thread} that uses it.
 *
 * <p>Note: Since Java 9, all resources are on the module path. Either in
 * named or unnamed modules. These resources are also considered to be
 * classpath resources.
 *
 * @since 1.0
 * @see DiscoverySelectors#selectClasspathResource(String)
 * @see ClasspathRootSelector
 * @see #getClasspathResourceName()
 */
@API(status = STABLE, since = "1.0")
public final class ClasspathResourceSelector implements DiscoverySelector {

	private final String classpathResourceName;

	private final @Nullable FilePosition position;

	private @Nullable Set<Resource> resources;

	ClasspathResourceSelector(String classpathResourceName, @Nullable FilePosition position) {
		boolean startsWithSlash = classpathResourceName.startsWith("/");
		this.classpathResourceName = (startsWithSlash ? classpathResourceName.substring(1) : classpathResourceName);
		Preconditions.notBlank(this.classpathResourceName,
			"classpath resource name must not be blank after removing leading slash");
		this.position = position;
	}

	ClasspathResourceSelector(Set<? extends Resource> resources) {
		this(resources.iterator().next().getName(), null);
		this.resources = unmodifiableSet(new LinkedHashSet<>(resources));
	}

	/**
	 * Get the name of the selected classpath resource.
	 *
	 * <p>The name of a <em>classpath resource</em> must follow the semantics
	 * for resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * @see ClassLoader#getResource(String)
	 * @see ClassLoader#getResourceAsStream(String)
	 * @see ClassLoader#getResources(String)
	 */
	public String getClasspathResourceName() {
		return this.classpathResourceName;
	}

	/**
	 * Get the selected {@link Resource resources}.
	 *
	 * <p>If the {@link Resource resources} were not provided, but only their name,
	 * this method attempts to lazily load the {@link Resource resources} based on
	 * their name and throws a {@link PreconditionViolationException} if the
	 * resource cannot be loaded.
	 *
	 * @since 1.12
	 * @deprecated Please use {{@link #getResources()}} instead.
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public Set<org.junit.platform.commons.support.Resource> getClasspathResources() {
		return getResources().stream() //
				.map(org.junit.platform.commons.support.Resource::of) //
				.collect(toCollection(LinkedHashSet::new));
	}

	/**
	 * Get the selected {@link Resource resources}.
	 *
	 * <p>If the {@link Resource resources} were not provided, but only their name,
	 * this method attempts to lazily load the {@link Resource resources} based on
	 * their name and throws a {@link PreconditionViolationException} if the
	 * resource cannot be loaded.
	 *
	 * @since 1.14
	 */
	@API(status = MAINTAINED, since = "1.14")
	public Set<Resource> getResources() {
		if (this.resources == null) {
			Try<Set<Resource>> tryToGetResource = ResourceSupport.tryToGetResources(this.classpathResourceName);
			Set<Resource> classpathResources = tryToGetResource.getNonNullOrThrow( //
				cause -> new PreconditionViolationException( //
					"Could not load resource(s) with name: " + this.classpathResourceName, cause));
			if (classpathResources.isEmpty()) {
				throw new PreconditionViolationException(
					"Could not find any resource(s) with name: " + this.classpathResourceName);
			}
			this.resources = unmodifiableSet(classpathResources);
		}
		return this.resources;
	}

	/**
	 * Get the selected {@code FilePosition} within the classpath resource.
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
		ClasspathResourceSelector that = (ClasspathResourceSelector) o;
		return Objects.equals(this.classpathResourceName, that.classpathResourceName)
				&& Objects.equals(this.position, that.position);
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return Objects.hash(this.classpathResourceName, this.position);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("classpathResourceName", this.classpathResourceName)
				.append("position", this.position)
				.toString();
		// @formatter:on
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		if (this.position == null) {
			return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, this.classpathResourceName));
		}
		else {
			return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX,
				"%s?%s".formatted(this.classpathResourceName, this.position.toQueryPart())));
		}
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for
	 * {@link ClasspathResourceSelector ClasspathResourceSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "resource";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<ClasspathResourceSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(StringUtils.splitIntoTwo('?', identifier.getValue()).map( //
				DiscoverySelectors::selectClasspathResource, //
				(resourceName, query) -> {
					FilePosition position = FilePosition.fromQuery(query).orElse(null);
					return DiscoverySelectors.selectClasspathResource(resourceName, position);
				} //
			));
		}

	}

}
