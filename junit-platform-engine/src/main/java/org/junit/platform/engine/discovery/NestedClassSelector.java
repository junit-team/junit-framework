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

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects a nested {@link Class}
 * or class name enclosed in other classes so that
 * {@link org.junit.platform.engine.TestEngine TestEngines} can discover
 * tests or containers based on classes.
 *
 * <p>If Java {@link Class} references are provided for the nested class or
 * the enclosing classes, the selector will return those classes and their class
 * names accordingly. If class names are provided, the selector will only attempt
 * to lazily load classes if {@link #getEnclosingClasses()} or
 * {@link #getNestedClass()} is invoked.
 *
 * <p>In this context, Java {@link Class} means anything that can be referenced
 * as a {@link Class} on the JVM &mdash; for example, classes from other JVM
 * languages such Groovy, Scala, etc.
 *
 * @since 1.6
 * @see DiscoverySelectors#selectNestedClass(List, Class)
 * @see DiscoverySelectors#selectNestedClass(List, String)
 * @see org.junit.platform.engine.support.descriptor.ClassSource
 * @see ClassSelector
 */
@API(status = STABLE, since = "1.6")
public final class NestedClassSelector implements DiscoverySelector {

	private final @Nullable ClassLoader classLoader;

	private final List<ClassSelector> enclosingClassSelectors;
	private final ClassSelector nestedClassSelector;

	NestedClassSelector(@Nullable ClassLoader classLoader, List<String> enclosingClassNames, String nestedClassName) {
		this.classLoader = classLoader;
		this.enclosingClassSelectors = enclosingClassNames.stream() //
				.map(className -> new ClassSelector(classLoader, className)) //
				.toList();
		this.nestedClassSelector = new ClassSelector(classLoader, nestedClassName);
	}

	NestedClassSelector(List<Class<?>> enclosingClasses, Class<?> nestedClass) {
		this.classLoader = nestedClass.getClassLoader();
		this.enclosingClassSelectors = enclosingClasses.stream().map(ClassSelector::new).toList();
		this.nestedClassSelector = new ClassSelector(nestedClass);
	}

	/**
	 * Get the {@link ClassLoader} used to load the selected nested class.
	 *
	 * @return the {@code ClassLoader}; potentially {@code null}
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public @Nullable ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Get the names of the classes enclosing the selected nested class.
	 */
	public List<String> getEnclosingClassNames() {
		return this.enclosingClassSelectors.stream().map(ClassSelector::getClassName).toList();
	}

	/**
	 * Get the list of {@link Class} enclosing the selected nested
	 * {@link Class}.
	 *
	 * <p>If the {@link Class} were not provided, but only the name of the
	 * nested class and its enclosing classes, this method attempts to lazily
	 * load the list of enclosing {@link Class} and throws a
	 * {@link PreconditionViolationException} if the classes cannot be loaded.
	 */
	public List<Class<?>> getEnclosingClasses() {
		return this.enclosingClassSelectors.stream().<Class<?>> map(ClassSelector::getJavaClass).toList();
	}

	/**
	 * Get the name of the selected nested class.
	 */
	public String getNestedClassName() {
		return this.nestedClassSelector.getClassName();
	}

	/**
	 * Get the selected nested {@link Class}.
	 *
	 * <p>If the {@link Class} were not provided, but only the name of the
	 * nested class and its enclosing classes, this method attempts to lazily
	 * load the nested {@link Class} and throws a
	 * {@link PreconditionViolationException} if the class cannot be loaded.
	 */
	public Class<?> getNestedClass() {
		return this.nestedClassSelector.getJavaClass();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NestedClassSelector that = (NestedClassSelector) o;
		return this.enclosingClassSelectors.equals(that.enclosingClassSelectors)
				&& this.nestedClassSelector.equals(that.nestedClassSelector);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.enclosingClassSelectors, this.nestedClassSelector);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("enclosingClassNames", getEnclosingClassNames()) //
				.append("nestedClassName", getNestedClassName()) //
				.append("classLoader", getClassLoader()) //
				.toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		String allClassNames = Stream.concat(enclosingClassSelectors.stream(), Stream.of(nestedClassSelector)) //
				.map(ClassSelector::getClassName) //
				.collect(joining("/"));
		return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, allClassNames));
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for
	 * {@link NestedClassSelector NestedClassSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		static final String PREFIX = "nested-class";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<NestedClassSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			List<String> parts = Arrays.asList(identifier.getValue().split("/"));
			return Optional.of(
				DiscoverySelectors.selectNestedClass(parts.subList(0, parts.size() - 1), parts.get(parts.size() - 1)));
		}

	}

}
