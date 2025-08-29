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

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * {@code ResourceSupport} provides static utility methods for common tasks
 * dealing with resources; for example, scanning for resources on the class path
 * or module path.
 *
 * <p>{@link org.junit.platform.engine.TestEngine TestEngine} and extension
 * authors are encouraged to use these supported methods in order to align with
 * the behavior of the JUnit Platform.
 *
 * @since 6.0
 * @see AnnotationSupport
 * @see ClassSupport
 * @see ModifierSupport
 * @see ReflectionSupport
 */
@API(status = MAINTAINED, since = "6.0")
public class ResourceSupport {

	/**
	 * Try to get the {@linkplain Resource resources} for the supplied classpath
	 * resource name.
	 *
	 * <p>The name of a <em>classpath resource</em> must follow the semantics
	 * for resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * <p>If the supplied classpath resource name is prefixed with a slash
	 * ({@code /}), the slash will be removed.
	 *
	 * @param classpathResourceName the name of the resource to load; never
	 * {@code null} or blank
	 * @return a successful {@code Try} containing the set of loaded resources
	 * (potentially empty) or a failed {@code Try} containing the exception in
	 * case a failure occurred while trying to list resources; never
	 * {@code null}
	 * @see #tryToGetResources(String, ClassLoader)
	 * @see ReflectionSupport#tryToLoadClass(String)
	 */
	public static Try<Set<Resource>> tryToGetResources(String classpathResourceName) {
		return ReflectionUtils.tryToGetResources(classpathResourceName);
	}

	/**
	 * Try to load the {@linkplain Resource resources} for the supplied classpath
	 * resource name, using the supplied {@link ClassLoader}.
	 *
	 * <p>The name of a <em>classpath resource</em> must follow the semantics
	 * for resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * <p>If the supplied classpath resource name is prefixed with a slash
	 * ({@code /}), the slash will be removed.
	 *
	 * @param classpathResourceName the name of the resource to load; never
	 * {@code null} or blank
	 * @param classLoader the {@code ClassLoader} to use; never {@code null}
	 * @return a successful {@code Try} containing the set of loaded resources
	 * (potentially empty) or a failed {@code Try} containing the exception in
	 * case a failure occurred while trying to list resources; never
	 * {@code null}
	 * @see #tryToGetResources(String)
	 * @see ReflectionSupport#tryToLoadClass(String, ClassLoader)
	 */
	public static Try<Set<Resource>> tryToGetResources(String classpathResourceName, ClassLoader classLoader) {
		return ReflectionUtils.tryToGetResources(classpathResourceName, classLoader);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied classpath {@code root}
	 * that match the specified {@code resourceFilter}.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning with the root of the classpath.
	 *
	 * @param root the URI for the classpath root in which to scan; never
	 * {@code null}
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return an immutable list of all such resources found; never {@code null}
	 * but potentially empty
	 * @see #findAllResourcesInPackage(String, ResourceFilter)
	 * @see #findAllResourcesInModule(String, ResourceFilter)
	 * @see ReflectionSupport#findAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 */
	public static List<Resource> findAllResourcesInClasspathRoot(URI root, ResourceFilter resourceFilter) {
		return ReflectionUtils.findAllResourcesInClasspathRoot(root, resourceFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied classpath {@code root}
	 * that match the specified {@code resourceFilter}.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning with the root of the classpath.
	 *
	 * @param root the URI for the classpath root in which to scan; never
	 * {@code null}
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return a stream of all such classes found; never {@code null}
	 * but potentially empty
	 * @see #streamAllResourcesInPackage(String, ResourceFilter)
	 * @see #streamAllResourcesInModule(String, ResourceFilter)
	 * @see ReflectionSupport#streamAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 */
	public static Stream<Resource> streamAllResourcesInClasspathRoot(URI root, ResourceFilter resourceFilter) {
		return ReflectionUtils.streamAllResourcesInClasspathRoot(root, resourceFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code basePackageName}
	 * that match the specified {@code resourceFilter}.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning within the supplied base package. The resulting list may include
	 * identically named resources from different classpath roots.
	 *
	 * @param basePackageName the name of the base package in which to start
	 * scanning; must not be {@code null} and must be valid in terms of Java
	 * syntax
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @see #findAllResourcesInClasspathRoot(URI, ResourceFilter)
	 * @see #findAllResourcesInModule(String, ResourceFilter)
	 * @see ReflectionSupport#findAllClassesInPackage(String, Predicate, Predicate)
	 */
	public static List<Resource> findAllResourcesInPackage(String basePackageName, ResourceFilter resourceFilter) {
		return ReflectionUtils.findAllResourcesInPackage(basePackageName, resourceFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code basePackageName}
	 * that match the specified {@code resourceFilter}.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning within the supplied base package. The resulting stream may
	 * include identically named resources from different classpath roots.
	 *
	 * @param basePackageName the name of the base package in which to start
	 * scanning; must not be {@code null} and must be valid in terms of Java
	 * syntax
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return a stream of all such resources found; never {@code null}
	 * but potentially empty
	 * @see #streamAllResourcesInClasspathRoot(URI, ResourceFilter)
	 * @see #streamAllResourcesInModule(String, ResourceFilter)
	 * @see ReflectionSupport#streamAllClassesInPackage(String, Predicate, Predicate)
	 */
	public static Stream<Resource> streamAllResourcesInPackage(String basePackageName, ResourceFilter resourceFilter) {
		return ReflectionUtils.streamAllResourcesInPackage(basePackageName, resourceFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code moduleName}
	 * that match the specified {@code resourceFilter}.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param moduleName the name of the module to scan; never {@code null} or
	 * <em>empty</em>
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return an immutable list of all such resources found; never {@code null}
	 * but potentially empty
	 * @see #findAllResourcesInClasspathRoot(URI, ResourceFilter)
	 * @see #findAllResourcesInPackage(String, ResourceFilter)
	 * @see ReflectionSupport#findAllClassesInModule(String, Predicate, Predicate)
	 */
	public static List<Resource> findAllResourcesInModule(String moduleName, ResourceFilter resourceFilter) {
		return ReflectionUtils.findAllResourcesInModule(moduleName, resourceFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code moduleName}
	 * that match the specified {@code resourceFilter}.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param moduleName the name of the module to scan; never {@code null} or
	 * <em>empty</em>
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return a stream of all such resources found; never {@code null}
	 * but potentially empty
	 * @see #streamAllResourcesInClasspathRoot(URI, ResourceFilter)
	 * @see #streamAllResourcesInPackage(String, ResourceFilter)
	 * @see ReflectionSupport#streamAllClassesInModule(String, Predicate, Predicate)
	 */
	public static Stream<Resource> streamAllResourcesInModule(String moduleName, ResourceFilter resourceFilter) {
		return ReflectionUtils.streamAllResourcesInModule(moduleName, resourceFilter);
	}

	private ResourceSupport() {
	}

}
