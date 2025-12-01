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

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * {@code ReflectionSupport} provides static utility methods for common
 * reflection tasks &mdash; for example, scanning for classes in the class-path
 * or module-path, loading classes, finding methods, invoking methods, etc.
 *
 * <p>{@link org.junit.platform.engine.TestEngine TestEngine} and extension
 * authors are encouraged to use these supported methods in order to align with
 * the behavior of the JUnit Platform.
 *
 * @since 1.0
 * @see AnnotationSupport
 * @see ClassSupport
 * @see ModifierSupport
 * @see ResourceSupport
 */
@API(status = MAINTAINED, since = "1.0")
public final class ReflectionSupport {

	private ReflectionSupport() {
		/* no-op */
	}

	/**
	 * Try to load a class by its <em>primitive name</em> or <em>fully qualified
	 * name</em>, using the default {@link ClassLoader}.
	 *
	 * <p>Class names for arrays may be specified using either the JVM's internal
	 * String representation (e.g., {@code [[I} for {@code int[][]},
	 * {@code [Lava.lang.String;} for {@code java.lang.String[]}, etc.) or
	 * <em>source code syntax</em> (e.g., {@code int[][]}, {@code java.lang.String[]},
	 * etc.).
	 *
	 * @param name the name of the class to load; never {@code null} or blank
	 * @return a successful {@code Try} containing the loaded class or a failed
	 * {@code Try} containing the exception if no such class could be loaded;
	 * never {@code null}
	 * @since 1.4
	 * @see #tryToLoadClass(String, ClassLoader)
	 * @see ResourceSupport#tryToGetResources(String)
	 */
	@API(status = MAINTAINED, since = "1.4")
	public static Try<Class<?>> tryToLoadClass(String name) {
		return ReflectionUtils.tryToLoadClass(name);
	}

	/**
	 * Try to load a class by its <em>primitive name</em> or <em>fully qualified
	 * name</em>, using the supplied {@link ClassLoader}.
	 *
	 * <p>See {@link ReflectionSupport#tryToLoadClass(String) tryToLoadClass(String)}
	 * for details on support for class names for arrays.
	 *
	 * @param name the name of the class to load; never {@code null} or blank
	 * @param classLoader the {@code ClassLoader} to use; never {@code null}
	 * @return a successful {@code Try} containing the loaded class or a failed
	 * {@code Try} containing the exception if no such class could be loaded;
	 * never {@code null}
	 * @since 1.10
	 * @see #tryToLoadClass(String)
	 * @see ResourceSupport#tryToGetResources(String, ClassLoader)
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static Try<Class<?>> tryToLoadClass(String name, ClassLoader classLoader) {
		return ReflectionUtils.tryToLoadClass(name, classLoader);
	}

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
	 * @since 1.12
	 * @see #tryToGetResources(String, ClassLoader)
	 * @deprecated Please use {@link ResourceSupport#tryToGetResources(String)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static Try<Set<Resource>> tryToGetResources(String classpathResourceName) {
		return ResourceSupport.tryToGetResources(classpathResourceName) //
				.andThenTry(ReflectionSupport::toSupportResourcesSet);
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
	 * @since 1.12
	 * @see #tryToGetResources(String)
	 * @deprecated Please use {@link ResourceSupport#tryToGetResources(String, ClassLoader)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static Try<Set<Resource>> tryToGetResources(String classpathResourceName, ClassLoader classLoader) {
		return ResourceSupport.tryToGetResources(classpathResourceName, classLoader) //
				.andThenTry(ReflectionSupport::toSupportResourcesSet);
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied classpath {@code root}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning with the root of the classpath.
	 *
	 * @param root the URI for the classpath root in which to scan; never
	 * {@code null}
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @see #findAllClassesInPackage(String, Predicate, Predicate)
	 * @see #findAllClassesInModule(String, Predicate, Predicate)
	 * @see ResourceSupport#findAllResourcesInClasspathRoot(URI, ResourceFilter)
	 */
	public static List<Class<?>> findAllClassesInClasspathRoot(URI root, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.findAllClassesInClasspathRoot(root, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied classpath {@code root}
	 * that match the specified {@code resourceFilter} predicate.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning with the root of the classpath.
	 *
	 * @param root the URI for the classpath root in which to scan; never
	 * {@code null}
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return an immutable list of all such resources found; never {@code null}
	 * but potentially empty
	 * @since 1.11
	 * @see #findAllResourcesInPackage(String, Predicate)
	 * @see #findAllResourcesInModule(String, Predicate)
	 * @deprecated Please use {@link ResourceSupport#findAllResourcesInClasspathRoot(URI, ResourceFilter)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static List<Resource> findAllResourcesInClasspathRoot(URI root, Predicate<Resource> resourceFilter) {
		return toSupportResourcesList(
			ResourceSupport.findAllResourcesInClasspathRoot(root, toResourceFilter(resourceFilter)));
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied classpath {@code root}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning with the root of the classpath.
	 *
	 * @param root the URI for the classpath root in which to scan; never
	 * {@code null}
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return a stream of all such classes found; never {@code null}
	 * but potentially empty
	 * @since 1.10
	 * @see #streamAllClassesInPackage(String, Predicate, Predicate)
	 * @see #streamAllClassesInModule(String, Predicate, Predicate)
	 * @see ResourceSupport#streamAllResourcesInClasspathRoot(URI, ResourceFilter)
	 */
	@API(status = MAINTAINED, since = "1.10")
	public static Stream<Class<?>> streamAllClassesInClasspathRoot(URI root, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.streamAllClassesInClasspathRoot(root, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied classpath {@code root}
	 * that match the specified {@code resourceFilter} predicate.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning with the root of the classpath.
	 *
	 * @param root the URI for the classpath root in which to scan; never
	 * {@code null}
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return a stream of all such classes found; never {@code null}
	 * but potentially empty
	 * @since 1.11
	 * @see #streamAllResourcesInPackage(String, Predicate)
	 * @see #streamAllResourcesInModule(String, Predicate)
	 * @deprecated Please use {@link ResourceSupport#streamAllResourcesInClasspathRoot(URI, ResourceFilter)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static Stream<Resource> streamAllResourcesInClasspathRoot(URI root, Predicate<Resource> resourceFilter) {
		return toSupportResourcesStream(
			ResourceSupport.streamAllResourcesInClasspathRoot(root, toResourceFilter(resourceFilter)));
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied {@code basePackageName}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning within the supplied base package.
	 *
	 * @param basePackageName the name of the base package in which to start
	 * scanning; must not be {@code null} and must be valid in terms of Java
	 * syntax
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @see #findAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 * @see #findAllClassesInModule(String, Predicate, Predicate)
	 * @see ResourceSupport#findAllResourcesInPackage(String, ResourceFilter)
	 */
	public static List<Class<?>> findAllClassesInPackage(String basePackageName, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.findAllClassesInPackage(basePackageName, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code basePackageName}
	 * that match the specified {@code resourceFilter} predicate.
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
	 * @since 1.11
	 * @see #findAllResourcesInClasspathRoot(URI, Predicate)
	 * @see #findAllResourcesInModule(String, Predicate)
	 * @deprecated Please use {@link ResourceSupport#findAllResourcesInPackage(String, ResourceFilter)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static List<Resource> findAllResourcesInPackage(String basePackageName, Predicate<Resource> resourceFilter) {
		return toSupportResourcesList(
			ResourceSupport.findAllResourcesInPackage(basePackageName, toResourceFilter(resourceFilter)));
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied {@code basePackageName}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning within the supplied base package. The resulting stream may
	 * include identically named resources from different classpath roots.
	 *
	 * @param basePackageName the name of the base package in which to start
	 * scanning; must not be {@code null} and must be valid in terms of Java
	 * syntax
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return a stream of all such classes found; never {@code null}
	 * but potentially empty
	 * @since 1.10
	 * @see #streamAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 * @see #streamAllClassesInModule(String, Predicate, Predicate)
	 * @see ResourceSupport#streamAllResourcesInPackage(String, ResourceFilter)
	 */
	@API(status = MAINTAINED, since = "1.10")
	public static Stream<Class<?>> streamAllClassesInPackage(String basePackageName, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.streamAllClassesInPackage(basePackageName, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code basePackageName}
	 * that match the specified {@code resourceFilter} predicate.
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
	 * @since 1.11
	 * @see #streamAllResourcesInClasspathRoot(URI, Predicate)
	 * @see #streamAllResourcesInModule(String, Predicate)
	 * @deprecated Please use {@link ResourceSupport#streamAllResourcesInPackage(String, ResourceFilter)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static Stream<Resource> streamAllResourcesInPackage(String basePackageName,
			Predicate<Resource> resourceFilter) {

		return toSupportResourcesStream(
			ResourceSupport.streamAllResourcesInPackage(basePackageName, toResourceFilter(resourceFilter)));
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied {@code moduleName}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param moduleName the name of the module to scan; never {@code null} or
	 * <em>empty</em>
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @since 1.1.1
	 * @see #findAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 * @see #findAllClassesInPackage(String, Predicate, Predicate)
	 * @see ResourceSupport#findAllResourcesInModule(String, ResourceFilter)
	 */
	@API(status = MAINTAINED, since = "1.1.1")
	public static List<Class<?>> findAllClassesInModule(String moduleName, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.findAllClassesInModule(moduleName, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied {@code module}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param module the module to scan; never {@code null} or <em>unnamed</em>
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @since 6.1
	 * @see #findAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 * @see #findAllClassesInPackage(String, Predicate, Predicate)
	 * @see ResourceSupport#findAllResourcesInModule(String, ResourceFilter)
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	public static List<Class<?>> findAllClassesInModule(Module module, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.findAllClassesInModule(module, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code moduleName}
	 * that match the specified {@code resourceFilter} predicate.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param moduleName the name of the module to scan; never {@code null} or
	 * <em>empty</em>
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return an immutable list of all such resources found; never {@code null}
	 * but potentially empty
	 * @since 1.11
	 * @see #findAllResourcesInClasspathRoot(URI, Predicate)
	 * @see #findAllResourcesInPackage(String, Predicate)
	 * @deprecated Please use {@link ResourceSupport#findAllResourcesInModule(String, ResourceFilter)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static List<Resource> findAllResourcesInModule(String moduleName, Predicate<Resource> resourceFilter) {
		return toSupportResourcesList(
			ResourceSupport.findAllResourcesInModule(moduleName, toResourceFilter(resourceFilter)));
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied {@code moduleName}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param moduleName the name of the module to scan; never {@code null} or
	 * <em>empty</em>
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return a stream of all such classes found; never {@code null}
	 * but potentially empty
	 * @since 1.10
	 * @see #streamAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 * @see #streamAllClassesInPackage(String, Predicate, Predicate)
	 */
	@API(status = MAINTAINED, since = "1.10")
	public static Stream<Class<?>> streamAllClassesInModule(String moduleName, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.streamAllClassesInModule(moduleName, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Resource resources} in the supplied {@code moduleName}
	 * that match the specified {@code resourceFilter} predicate.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param moduleName the name of the module to scan; never {@code null} or
	 * <em>empty</em>
	 * @param resourceFilter the resource type filter; never {@code null}
	 * @return a stream of all such resources found; never {@code null}
	 * but potentially empty
	 * @since 1.11
	 * @see #streamAllResourcesInClasspathRoot(URI, Predicate)
	 * @see #streamAllResourcesInPackage(String, Predicate)
	 * @deprecated Please use {@link ResourceSupport#streamAllResourcesInModule(String, ResourceFilter)} instead
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static Stream<Resource> streamAllResourcesInModule(String moduleName, Predicate<Resource> resourceFilter) {
		return toSupportResourcesStream(
			ResourceSupport.streamAllResourcesInModule(moduleName, toResourceFilter(resourceFilter)));
	}

	/**
	 * Create a new instance of the specified {@link Class} by invoking
	 * the constructor whose argument list matches the types of the supplied
	 * arguments.
	 *
	 * <p>The constructor will be made accessible if necessary, and any checked
	 * exception will be {@linkplain ExceptionUtils#throwAsUncheckedException masked}
	 * as an unchecked exception.
	 *
	 * @param clazz the class to instantiate; never {@code null}
	 * @param args the arguments to pass to the constructor, none of which may
	 * be {@code null}
	 * @return the new instance; never {@code null}
	 * @see ExceptionUtils#throwAsUncheckedException(Throwable)
	 */
	public static <T> T newInstance(Class<T> clazz, Object... args) {
		return ReflectionUtils.newInstance(clazz, args);
	}

	/**
	 * Invoke the supplied method, making it accessible if necessary and
	 * {@linkplain ExceptionUtils#throwAsUncheckedException masking} any
	 * checked exception as an unchecked exception.
	 *
	 * @param method the method to invoke; never {@code null}
	 * @param target the object on which to invoke the method; may be
	 * {@code null} if the method is {@code static}
	 * @param args the arguments to pass to the method; never {@code null}
	 * @return the value returned by the method invocation or {@code null}
	 * if the return type is {@code void}
	 * @see ExceptionUtils#throwAsUncheckedException(Throwable)
	 */
	public static @Nullable Object invokeMethod(Method method, @Nullable Object target, @Nullable Object... args) {
		return ReflectionUtils.invokeMethod(method, target, args);
	}

	/**
	 * Find all distinct {@linkplain Field fields} of the supplied class or
	 * interface that match the specified {@code predicate}.
	 *
	 * <p>Fields declared in the same class or interface will be ordered using
	 * an algorithm that is deterministic but intentionally nonobvious.
	 *
	 * <p>The results will not contain fields that are
	 * {@linkplain Field#isSynthetic() synthetic}.
	 *
	 * @param clazz the class or interface in which to find the fields; never {@code null}
	 * @param predicate the field filter; never {@code null}
	 * @param traversalMode the hierarchy traversal mode; never {@code null}
	 * @return an immutable list of all such fields found; never {@code null}
	 * but potentially empty
	 * @since 1.4
	 */
	@API(status = MAINTAINED, since = "1.4")
	public static List<Field> findFields(Class<?> clazz, Predicate<Field> predicate,
			HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(traversalMode, "HierarchyTraversalMode must not be null");

		return ReflectionUtils.findFields(clazz, predicate,
			ReflectionUtils.HierarchyTraversalMode.valueOf(traversalMode.name()));
	}

	/**
	 * Find all distinct {@linkplain Field fields} of the supplied class or
	 * interface that match the specified {@code predicate}.
	 *
	 * <p>Fields declared in the same class or interface will be ordered using
	 * an algorithm that is deterministic but intentionally nonobvious.
	 *
	 * <p>The results will not contain fields that are
	 * {@linkplain Field#isSynthetic() synthetic}.
	 *
	 * @param clazz the class or interface in which to find the fields; never {@code null}
	 * @param predicate the field filter; never {@code null}
	 * @param traversalMode the hierarchy traversal mode; never {@code null}
	 * @return a stream of all such fields found; never {@code null}
	 * but potentially empty
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.10")
	public static Stream<Field> streamFields(Class<?> clazz, Predicate<Field> predicate,
			HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(traversalMode, "HierarchyTraversalMode must not be null");

		return ReflectionUtils.streamFields(clazz, predicate,
			ReflectionUtils.HierarchyTraversalMode.valueOf(traversalMode.name()));
	}

	/**
	 * Try to read the value of a potentially inaccessible field.
	 *
	 * <p>If an exception occurs while reading the field, a failed {@link Try}
	 * is returned that contains the corresponding exception.
	 *
	 * @param field the field to read; never {@code null}
	 * @param instance the instance from which the value is to be read; may
	 * be {@code null} for a static field
	 * @since 1.4
	 */
	@API(status = MAINTAINED, since = "1.4")
	public static Try<@Nullable Object> tryToReadFieldValue(Field field, @Nullable Object instance) {
		return ReflectionUtils.tryToReadFieldValue(field, instance);
	}

	/**
	 * Find the first {@link Method} of the supplied class or interface that
	 * meets the specified criteria, beginning with the specified class or
	 * interface and traversing up the type hierarchy until such a method is
	 * found or the type hierarchy is exhausted.
	 *
	 * <p>This method uses the {@link ClassLoader} of the supplied {@code clazz}
	 * to load parameter types instead of using the <em>default</em>
	 * {@code ClassLoader}, which allows parameter types to be resolved in different
	 * {@code ClassLoader} arrangements.
	 *
	 * <p>The algorithm does not search for methods in {@link Object}.
	 *
	 * @param clazz the class or interface in which to find the method; never {@code null}
	 * @param methodName the name of the method to find; never {@code null} or empty
	 * @param parameterTypeNames the fully qualified names of the types of parameters
	 * accepted by the method, if any, provided as a comma-separated list
	 * @return an {@code Optional} containing the method found; never {@code null}
	 * but potentially empty if no such method could be found
	 * @see #findMethod(Class, String, Class...)
	 */
	public static Optional<Method> findMethod(Class<?> clazz, String methodName, @Nullable String parameterTypeNames) {
		return ReflectionUtils.findMethod(clazz, methodName, parameterTypeNames);
	}

	/**
	 * Find the first {@link Method} of the supplied class or interface that
	 * meets the specified criteria, beginning with the specified class or
	 * interface and traversing up the type hierarchy until such a method is
	 * found or the type hierarchy is exhausted.
	 *
	 * <p>The algorithm does not search for methods in {@link Object}.
	 *
	 * @param clazz the class or interface in which to find the method; never {@code null}
	 * @param methodName the name of the method to find; never {@code null} or empty
	 * @param parameterTypes the types of parameters accepted by the method, if any;
	 * never {@code null}
	 * @return an {@code Optional} containing the method found; never {@code null}
	 * but potentially empty if no such method could be found
	 * @see #findMethod(Class, String, String)
	 */
	public static Optional<Method> findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		return ReflectionUtils.findMethod(clazz, methodName, parameterTypes);
	}

	/**
	 * Find all distinct {@linkplain Method methods} of the supplied class or
	 * interface that match the specified {@code predicate}.
	 *
	 * <p>The results will not contain methods that are <em>overridden</em>.
	 *
	 * <p>If you are looking for methods annotated with a certain annotation
	 * type, consider using
	 * {@link AnnotationSupport#findAnnotatedMethods(Class, Class, HierarchyTraversalMode)}.
	 *
	 * @param clazz the class or interface in which to find the methods; never {@code null}
	 * @param predicate the method filter; never {@code null}
	 * @param traversalMode the hierarchy traversal mode; never {@code null}
	 * @return an immutable list of all such methods found; never {@code null}
	 * but potentially empty
	 */
	public static List<Method> findMethods(Class<?> clazz, Predicate<Method> predicate,
			HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(traversalMode, "HierarchyTraversalMode must not be null");

		return ReflectionUtils.findMethods(clazz, predicate,
			ReflectionUtils.HierarchyTraversalMode.valueOf(traversalMode.name()));
	}

	/**
	 * Find all distinct {@linkplain Method methods} of the supplied class or
	 * interface that match the specified {@code predicate}.
	 *
	 * <p>The results will not contain methods that are <em>overridden</em>.
	 *
	 * <p>If you are looking for methods annotated with a certain annotation
	 * type, consider using
	 * {@link AnnotationSupport#findAnnotatedMethods(Class, Class, HierarchyTraversalMode)}.
	 *
	 * @param clazz the class or interface in which to find the methods; never {@code null}
	 * @param predicate the method filter; never {@code null}
	 * @param traversalMode the hierarchy traversal mode; never {@code null}
	 * @return a stream of all such methods found; never {@code null}
	 * but potentially empty
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.10")
	public static Stream<Method> streamMethods(Class<?> clazz, Predicate<Method> predicate,
			HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(traversalMode, "HierarchyTraversalMode must not be null");

		return ReflectionUtils.streamMethods(clazz, predicate,
			ReflectionUtils.HierarchyTraversalMode.valueOf(traversalMode.name()));
	}

	/**
	 * Find all nested classes within the supplied class, or inherited by the
	 * supplied class, that conform to the supplied predicate.
	 *
	 * <p>This method does <strong>not</strong> search for nested classes
	 * recursively.
	 *
	 * <p>Nested classes declared in the same enclosing class or interface will
	 * be ordered using an algorithm that is deterministic but intentionally
	 * nonobvious.
	 *
	 * <p>This method detects cycles in <em>inner</em> class hierarchies &mdash;
	 * from the supplied class up to the outermost enclosing class &mdash; and
	 * throws a {@link JUnitException} if such a cycle is detected. Cycles within
	 * inner class hierarchies <em>below</em> the supplied class are not detected
	 * by this method.
	 *
	 * @param clazz the class to be searched; never {@code null}
	 * @param predicate the predicate against which the list of nested classes is
	 * checked; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @throws JUnitException if a cycle is detected within an inner class hierarchy
	 */
	public static List<Class<?>> findNestedClasses(Class<?> clazz, Predicate<Class<?>> predicate)
			throws JUnitException {

		return ReflectionUtils.findNestedClasses(clazz, predicate);
	}

	/**
	 * Find all nested classes within the supplied class, or inherited by the
	 * supplied class, that conform to the supplied predicate.
	 *
	 * <p>This method does <strong>not</strong> search for nested classes
	 * recursively.
	 *
	 * <p>Nested classes declared in the same enclosing class or interface will
	 * be ordered using an algorithm that is deterministic but intentionally
	 * nonobvious.
	 *
	 * <p>This method detects cycles in <em>inner</em> class hierarchies &mdash;
	 * from the supplied class up to the outermost enclosing class &mdash; and
	 * throws a {@link JUnitException} if such a cycle is detected. Cycles within
	 * inner class hierarchies <em>below</em> the supplied class are not detected
	 * by this method.
	 *
	 * @param clazz the class to be searched; never {@code null}
	 * @param predicate the predicate against which the list of nested classes is
	 * checked; never {@code null}
	 * @return a stream of all such classes found; never {@code null}
	 * but potentially empty
	 * @throws JUnitException if a cycle is detected within an inner class hierarchy
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.10")
	public static Stream<Class<?>> streamNestedClasses(Class<?> clazz, Predicate<Class<?>> predicate)
			throws JUnitException {

		return ReflectionUtils.streamNestedClasses(clazz, predicate);
	}

	/**
	 * Make the supplied field accessible via reflection.
	 *
	 * <p>If you're looking for similar functionality for constructors or
	 * methods, consider using {@link #newInstance(Class, Object...)} or
	 * {@link #invokeMethod(Method, Object, Object...)}.
	 *
	 * @param field the field to make accessible; never {@code null}
	 * @return the supplied field
	 * @since 1.12
	 * @see Field#setAccessible(boolean)
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static Field makeAccessible(Field field) {
		return ReflectionUtils.makeAccessible(Preconditions.notNull(field, "field must not be null"));
	}

	@SuppressWarnings("removal")
	private static ResourceFilter toResourceFilter(Predicate<Resource> resourceFilter) {
		Preconditions.notNull(resourceFilter, "resourceFilter must not be null");
		return ResourceFilter.of(r -> resourceFilter.test(Resource.of(r)));
	}

	@SuppressWarnings("removal")
	static List<Resource> toSupportResourcesList(List<org.junit.platform.commons.io.Resource> resources) {
		return toSupportResourcesStream(resources.stream()).toList();
	}

	@SuppressWarnings("removal")
	static Set<Resource> toSupportResourcesSet(Set<org.junit.platform.commons.io.Resource> resources) {
		return toSupportResourcesStream(resources.stream()).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@SuppressWarnings("removal")
	static Stream<Resource> toSupportResourcesStream(Stream<org.junit.platform.commons.io.Resource> resources) {
		return resources.map(Resource::of);
	}

}
