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

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;
import org.junit.platform.engine.UniqueId;

/**
 * Collection of {@code static} factory methods for creating
 * {@link DiscoverySelector DiscoverySelectors}.
 *
 * @since 1.0
 * @see UriSelector
 * @see FileSelector
 * @see DirectorySelector
 * @see ClasspathRootSelector
 * @see ClasspathResourceSelector
 * @see ModuleSelector
 * @see PackageSelector
 * @see ClassSelector
 * @see MethodSelector
 * @see NestedClassSelector
 * @see NestedMethodSelector
 * @see UniqueIdSelector
 * @see DiscoverySelectorIdentifier
 */
@API(status = STABLE, since = "1.0")
public final class DiscoverySelectors {

	private DiscoverySelectors() {
		/* no-op */
	}

	/**
	 * Create a {@code UriSelector} for the supplied URI.
	 *
	 * @param uri the URI to select; never {@code null} or blank
	 * @see UriSelector
	 * @see #selectUri(URI)
	 * @see #selectFile(String)
	 * @see #selectFile(File)
	 * @see #selectDirectory(String)
	 * @see #selectDirectory(File)
	 */
	public static UriSelector selectUri(String uri) {
		Preconditions.notBlank(uri, "URI must not be null or blank");
		try {
			return new UriSelector(new URI(uri));
		}
		catch (URISyntaxException ex) {
			throw new PreconditionViolationException("Failed to create a java.net.URI from: " + uri, ex);
		}
	}

	/**
	 * Create a {@code UriSelector} for the supplied {@link URI}.
	 *
	 * @param uri the URI to select; never {@code null}
	 * @see UriSelector
	 * @see #selectUri(String)
	 * @see #selectFile(String)
	 * @see #selectFile(File)
	 * @see #selectDirectory(String)
	 * @see #selectDirectory(File)
	 */
	public static UriSelector selectUri(URI uri) {
		Preconditions.notNull(uri, "URI must not be null");
		return new UriSelector(uri);
	}

	/**
	 * Create a {@code FileSelector} for the supplied file path.
	 *
	 * <p>This method selects the file using the supplied path <em>as is</em>,
	 * without verifying if the file exists.
	 *
	 * @param path the path to the file to select; never {@code null} or blank
	 * @see FileSelector
	 * @see #selectFile(File)
	 * @see #selectFile(String, FilePosition)
	 * @see #selectFile(File, FilePosition)
	 * @see #selectDirectory(String)
	 * @see #selectDirectory(File)
	 */
	public static FileSelector selectFile(String path) {
		return selectFile(path, null);
	}

	/**
	 * Create a {@code FileSelector} for the supplied {@linkplain File file}.
	 *
	 * <p>This method selects the file in its {@linkplain File#getCanonicalPath()
	 * canonical} form and throws a {@link PreconditionViolationException} if the
	 * file does not exist.
	 *
	 * @param file the file to select; never {@code null}
	 * @see FileSelector
	 * @see #selectFile(String)
	 * @see #selectFile(File, FilePosition)
	 * @see #selectFile(String, FilePosition)
	 * @see #selectDirectory(String)
	 * @see #selectDirectory(File)
	 */
	public static FileSelector selectFile(File file) {
		return selectFile(file, null);
	}

	/**
	 * Create a {@code FileSelector} for the supplied file path.
	 *
	 * <p>This method selects the file using the supplied path <em>as is</em>,
	 * without verifying if the file exists.
	 *
	 * @param path the path to the file to select; never {@code null} or blank
	 * @param position the position inside the file; may be {@code null}
	 * @see FileSelector
	 * @see #selectFile(String)
	 * @see #selectFile(File)
	 * @see #selectFile(File, FilePosition)
	 * @see #selectDirectory(String)
	 * @see #selectDirectory(File)
	 */
	public static FileSelector selectFile(String path, @Nullable FilePosition position) {
		Preconditions.notBlank(path, "File path must not be null or blank");
		return new FileSelector(path, position);
	}

	/**
	 * Create a {@code FileSelector} for the supplied {@linkplain File file}.
	 *
	 * <p>This method selects the file in its {@linkplain File#getCanonicalPath()
	 * canonical} form and throws a {@link PreconditionViolationException} if the
	 * file does not exist.
	 *
	 * @param file the file to select; never {@code null}
	 * @param position the position inside the file; may be {@code null}
	 * @see FileSelector
	 * @see #selectFile(File)
	 * @see #selectFile(String)
	 * @see #selectFile(String, FilePosition)
	 * @see #selectDirectory(String)
	 * @see #selectDirectory(File)
	 */
	public static FileSelector selectFile(File file, @Nullable FilePosition position) {
		Preconditions.notNull(file, "File must not be null");
		Preconditions.condition(file.isFile(),
			() -> "The supplied java.io.File [%s] must represent an existing file".formatted(file));
		try {
			return new FileSelector(file.getCanonicalPath(), position);
		}
		catch (IOException ex) {
			throw new PreconditionViolationException("Failed to retrieve canonical path for file: " + file, ex);
		}
	}

	/**
	 * Create a {@code DirectorySelector} for the supplied directory path.
	 *
	 * <p>This method selects the directory using the supplied path <em>as is</em>,
	 * without verifying if the directory exists.
	 *
	 * @param path the path to the directory to select; never {@code null} or blank
	 * @see DirectorySelector
	 * @see #selectDirectory(File)
	 * @see #selectFile(String)
	 * @see #selectFile(File)
	 */
	public static DirectorySelector selectDirectory(String path) {
		Preconditions.notBlank(path, "Directory path must not be null or blank");
		return new DirectorySelector(path);
	}

	/**
	 * Create a {@code DirectorySelector} for the supplied {@linkplain File directory}.
	 *
	 * <p>This method selects the directory in its {@linkplain File#getCanonicalPath()
	 * canonical} form and throws a {@link PreconditionViolationException} if the
	 * directory does not exist.
	 *
	 * @param directory the directory to select; never {@code null}
	 * @see DirectorySelector
	 * @see #selectDirectory(String)
	 * @see #selectFile(String)
	 * @see #selectFile(File)
	 */
	public static DirectorySelector selectDirectory(File directory) {
		Preconditions.notNull(directory, "Directory must not be null");
		Preconditions.condition(directory.isDirectory(),
			() -> "The supplied java.io.File [%s] must represent an existing directory".formatted(directory));
		try {
			return new DirectorySelector(directory.getCanonicalPath());
		}
		catch (IOException ex) {
			throw new PreconditionViolationException("Failed to retrieve canonical path for directory: " + directory,
				ex);
		}
	}

	/**
	 * Create a list of {@code ClasspathRootSelectors} for the supplied
	 * <em>classpath roots</em> (directories or JAR files).
	 *
	 * <p>Since the supplied paths are converted to {@link URI URIs}, the
	 * {@link java.nio.file.FileSystem} that created them must be the
	 * {@linkplain java.nio.file.FileSystems#getDefault() default} or one that
	 * has been created by an installed
	 * {@link java.nio.file.spi.FileSystemProvider}.
	 *
	 * <p>Since {@linkplain org.junit.platform.engine.TestEngine engines} are not
	 * expected to modify the classpath, the classpath roots represented by the
	 * resulting selectors must be on the classpath of the
	 * {@linkplain Thread#getContextClassLoader() context class loader} of the
	 * {@linkplain Thread thread} that uses these selectors.
	 *
	 * <p>The {@link Set} supplied to this method should have a reliable iteration
	 * order to support reliable discovery and execution order. It is therefore
	 * recommended that the set be a {@link java.util.SequencedSet} (on Java 21
	 * or higher), {@link java.util.SortedSet}, {@link java.util.LinkedHashSet},
	 * or similar. Note that {@link Set#of(Object[])} and related {@code Set.of()}
	 * methods do not guarantee a reliable iteration order.
	 *
	 * @param classpathRoots set of directories and JAR files in the filesystem
	 * that represent classpath roots; never {@code null}
	 * @return a list of selectors for the supplied classpath roots; elements
	 * which do not physically exist in the filesystem will be filtered out
	 * @see ClasspathRootSelector
	 * @see Thread#getContextClassLoader()
	 */
	public static List<ClasspathRootSelector> selectClasspathRoots(Set<Path> classpathRoots) {
		Preconditions.notNull(classpathRoots, "classpathRoots must not be null");

		// @formatter:off
		return classpathRoots.stream()
				.filter(Files::exists)
				.map(Path::toUri)
				.map(ClasspathRootSelector::new)
				// unmodifiable since selectClasspathRoots is a public, non-internal method
				.toList();
		// @formatter:on
	}

	/**
	 * Create a {@code ClasspathResourceSelector} for the supplied classpath
	 * resource name.
	 *
	 * <p>The name of a <em>classpath resource</em> must follow the semantics
	 * for resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * <p>If the supplied classpath resource name is prefixed with a slash
	 * ({@code /}), the slash will be removed.
	 *
	 * <p>Since {@linkplain org.junit.platform.engine.TestEngine engines} are not
	 * expected to modify the classpath, the supplied classpath resource must be
	 * on the classpath of the
	 * {@linkplain Thread#getContextClassLoader() context class loader} of the
	 * {@linkplain Thread thread} that uses the resulting selector.
	 *
	 * @param classpathResourceName the name of the classpath resource; never
	 * {@code null} or blank
	 * @see #selectClasspathResource(String, FilePosition)
	 * @see #selectClasspathResources(String...)
	 * @see #selectClasspathResourceByName(Set)
	 * @see ClasspathResourceSelector
	 * @see ClassLoader#getResource(String)
	 * @see ClassLoader#getResourceAsStream(String)
	 * @see ClassLoader#getResources(String)
	 */
	public static ClasspathResourceSelector selectClasspathResource(String classpathResourceName) {
		return selectClasspathResource(classpathResourceName, null);
	}

	/**
	 * Create a {@code ClasspathResourceSelector} for the supplied classpath
	 * resource name.
	 *
	 * <p>The name of a <em>classpath resource</em> must follow the semantics
	 * for resource paths as defined in {@link ClassLoader#getResource(String)}.
	 *
	 * <p>If the supplied classpath resource name is prefixed with a slash
	 * ({@code /}), the slash will be removed.
	 *
	 * <p>Since {@linkplain org.junit.platform.engine.TestEngine engines} are not
	 * expected to modify the classpath, the supplied classpath resource must be
	 * on the classpath of the
	 * {@linkplain Thread#getContextClassLoader() context class loader} of the
	 * {@linkplain Thread thread} that uses the resulting selector.
	 *
	 * @param classpathResourceName the name of the classpath resource; never
	 * {@code null} or blank
	 * @param position the position inside the classpath resource; may be {@code null}
	 * @see #selectClasspathResource(String)
	 * @see #selectClasspathResources(String...)
	 * @see #selectClasspathResourceByName(Set)
	 * @see ClasspathResourceSelector
	 * @see ClassLoader#getResource(String)
	 * @see ClassLoader#getResourceAsStream(String)
	 * @see ClassLoader#getResources(String)
	 */
	public static ClasspathResourceSelector selectClasspathResource(String classpathResourceName,
			@Nullable FilePosition position) {
		Preconditions.notBlank(classpathResourceName, "classpath resource name must not be null or blank");
		return new ClasspathResourceSelector(classpathResourceName, position);
	}

	/**
	 * Create a {@code ClasspathResourceSelector} for the supplied classpath
	 * resources.
	 *
	 * <p>Since {@linkplain org.junit.platform.engine.TestEngine engines} are not
	 * expected to modify the classpath, the supplied resource must be on the
	 * classpath of the
	 * {@linkplain Thread#getContextClassLoader() context class loader} of the
	 * {@linkplain Thread thread} that uses the resulting selector.
	 *
	 * <p>Note: Since Java 9, all resources are on the module path. Either in
	 * named or unnamed modules. These resources are also considered to be
	 * classpath resources.
	 *
	 * <p>The {@link Set} supplied to this method should have a reliable iteration
	 * order to support reliable discovery and execution order. It is therefore
	 * recommended that the set be a {@link java.util.SequencedSet} (on Java 21
	 * or higher), {@link java.util.SortedSet}, {@link java.util.LinkedHashSet},
	 * or similar. Note that {@link Set#of(Object[])} and related {@code Set.of()}
	 * methods do not guarantee a reliable iteration order.
	 *
	 * @param classpathResources a set of classpath resources; never
	 * {@code null} or empty. All resources must have the same name, may not
	 * be {@code null} or blank.
	 * @since 1.12
	 * @see #selectClasspathResource(String, FilePosition)
	 * @see #selectClasspathResource(String)
	 * @see ClasspathResourceSelector
	 * @see ReflectionSupport#tryToGetResources(String)
	 * @deprecated Please use {@link #selectClasspathResourceByName(Set)} instead.
	 */
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	@SuppressWarnings("removal")
	public static ClasspathResourceSelector selectClasspathResource(
			Set<org.junit.platform.commons.support.Resource> classpathResources) {
		return selectClasspathResourceByName(classpathResources);
	}

	/**
	 * Create a {@code ClasspathResourceSelector} for the supplied classpath
	 * resources.
	 *
	 * <p>Since {@linkplain org.junit.platform.engine.TestEngine engines} are not
	 * expected to modify the classpath, the supplied resource must be on the
	 * classpath of the
	 * {@linkplain Thread#getContextClassLoader() context class loader} of the
	 * {@linkplain Thread thread} that uses the resulting selector.
	 *
	 * <p>Note: Since Java 9, all resources are on the module path. Either in
	 * named or unnamed modules. These resources are also considered to be
	 * classpath resources.
	 *
	 * <p>The {@link Set} supplied to this method should have a reliable iteration
	 * order to support reliable discovery and execution order. It is therefore
	 * recommended that the set be a {@link java.util.SequencedSet} (on Java 21
	 * or higher), {@link java.util.SortedSet}, {@link java.util.LinkedHashSet},
	 * or similar. Note that {@link Set#of(Object[])} and related {@code Set.of()}
	 * methods do not guarantee a reliable iteration order.
	 *
	 * @param classpathResources a set of classpath resources; never
	 * {@code null} or empty. All resources must have the same name, may not
	 * be {@code null} or blank.
	 * @since 1.14
	 * @see #selectClasspathResource(String, FilePosition)
	 * @see #selectClasspathResource(String)
	 * @see ClasspathResourceSelector
	 * @see org.junit.platform.commons.support.ResourceSupport#tryToGetResources(String)
	 */
	@API(status = MAINTAINED, since = "1.14")
	public static ClasspathResourceSelector selectClasspathResourceByName(Set<? extends Resource> classpathResources) {
		Preconditions.notEmpty(classpathResources, "classpath resources must not be null or empty");
		Preconditions.containsNoNullElements(classpathResources, "individual classpath resources must not be null");
		List<String> resourceNames = classpathResources.stream().map(Resource::getName).distinct().toList();
		Preconditions.condition(resourceNames.size() == 1, "all classpath resources must have the same name");
		Preconditions.notBlank(resourceNames.get(0), "classpath resource names must not be null or blank");
		return new ClasspathResourceSelector(classpathResources);
	}

	/**
	 * Create a {@code ClasspathResourceSelector} for each supplied classpath
	 * resource name.
	 *
	 * @param classpathResourceNames the names of the classpath resource; never
	 * {@code null} and never containing {@code null} or blank references.
	 * @since 6.1
	 * @see #selectClasspathResource(String)
	 * @see #selectClasspathResources(String...)
	 * @see ClasspathResourceSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	public static List<ClasspathResourceSelector> selectClasspathResources(String... classpathResourceNames) {
		Preconditions.notNull(classpathResourceNames, "classpathResourceNames must not be null");
		return selectClasspathResources(Arrays.asList(classpathResourceNames));
	}

	/**
	 * Create a {@code ClasspathResourceSelector} for each supplied classpath
	 * resource name.
	 *
	 * @param classpathResourceNames the names of the classpath resource; never
	 * {@code null} and never containing {@code null} or blank references.
	 * @since 6.1
	 * @see #selectClasspathResource(String)
	 * @see #selectClasspathResources(String...)
	 * @see ClasspathResourceSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	public static List<ClasspathResourceSelector> selectClasspathResources(List<String> classpathResourceNames) {
		Preconditions.notNull(classpathResourceNames, "classpathResourceNames must not be null");
		Preconditions.containsNoBlankElements(classpathResourceNames,
			"Individual classpathResourceNames must not be null or blank");
		return classpathResourceNames.stream() //
				.distinct() //
				.map(DiscoverySelectors::selectClasspathResource) //
				.toList();
	}

	/**
	 * Create a {@code ModuleSelector} for the supplied module name.
	 *
	 * <p>The unnamed module is not supported.
	 *
	 * @param moduleName the module name to select; never {@code null} or blank
	 * @since 1.1
	 * @see ModuleSelector
	 */
	@API(status = STABLE, since = "1.10")
	public static ModuleSelector selectModule(String moduleName) {
		Preconditions.notBlank(moduleName, "Module name must not be null or blank");
		return new ModuleSelector(moduleName.strip());
	}

	/**
	 * Create a {@code ModuleSelector} for the supplied module.
	 *
	 * <p>The unnamed module is not supported.
	 *
	 * @param module the module to select; never {@code null} or <em>unnamed</em>
	 * @since 6.1
	 * @see ModuleSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.1")
	public static ModuleSelector selectModule(Module module) {
		Preconditions.notNull(module, "Module must not be null");
		Preconditions.condition(module.isNamed(), "Module must be named");
		return new ModuleSelector(module);
	}

	/**
	 * Create a list of {@code ModuleSelectors} for the supplied module names.
	 *
	 * <p>The unnamed module is not supported.
	 *
	 * <p>The {@link Set} supplied to this method should have a reliable iteration
	 * order to support reliable discovery and execution order. It is therefore
	 * recommended that the set be a {@link java.util.SequencedSet} (on Java 21
	 * or higher), {@link java.util.SortedSet}, {@link java.util.LinkedHashSet},
	 * or similar. Note that {@link Set#of(Object[])} and related {@code Set.of()}
	 * methods do not guarantee a reliable iteration order.
	 *
	 * @param moduleNames the module names to select; never {@code null}, never
	 * containing {@code null} or blank
	 * @since 1.1
	 * @see ModuleSelector
	 */
	@API(status = STABLE, since = "1.10")
	public static List<ModuleSelector> selectModules(Set<String> moduleNames) {
		Preconditions.notNull(moduleNames, "Module names must not be null");
		Preconditions.containsNoNullElements(moduleNames, "Individual module name must not be null");

		// @formatter:off
		return moduleNames.stream()
				.map(DiscoverySelectors::selectModule)
				// unmodifiable since this is a public, non-internal method
				.toList();
		// @formatter:on
	}

	/**
	 * Create a {@code PackageSelector} for the supplied package name.
	 *
	 * <p>The default package is represented by an empty string ({@code ""}).
	 *
	 * @param packageName the package name to select; never {@code null} and
	 * never containing whitespace only
	 * @see PackageSelector
	 */
	public static PackageSelector selectPackage(String packageName) {
		Preconditions.notNull(packageName, "Package name must not be null");
		Preconditions.condition(packageName.isEmpty() || !packageName.isBlank(),
			"Package name must not contain only whitespace");
		return new PackageSelector(packageName.strip());
	}

	/**
	 * Create a {@code ClassSelector} for the supplied {@link Class}.
	 *
	 * @param clazz the class to select; never {@code null}
	 * @see ClassSelector
	 */
	public static ClassSelector selectClass(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		return new ClassSelector(clazz);
	}

	/**
	 * Create a {@code ClassSelector} for the supplied class name.
	 *
	 * @param className the fully qualified name of the class to select; never
	 * {@code null} or blank
	 * @see ClassSelector
	 */
	public static ClassSelector selectClass(String className) {
		return selectClass(null, className);
	}

	/**
	 * Create a {@code ClassSelector} for the supplied class name and class loader.
	 *
	 * @param classLoader the class loader to use to load the class, or {@code null}
	 * to signal that the default {@code ClassLoader} should be used
	 * @param className the fully qualified name of the class to select; never
	 * {@code null} or blank
	 * @since 1.10
	 * @see ClassSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static ClassSelector selectClass(@Nullable ClassLoader classLoader, String className) {
		Preconditions.notBlank(className, "Class name must not be null or blank");
		return new ClassSelector(classLoader, className);
	}

	/**
	 * Create a {@code ClassSelector} for each supplied {@link Class}.
	 *
	 * @param classes the classes to select; never {@code null} and never containing
	 * {@code null} class references
	 * @since 6.0
	 * @see #selectClass(Class)
	 * @see #selectClasses(List)
	 * @see ClassSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static List<ClassSelector> selectClasses(Class<?>... classes) {
		Preconditions.notNull(classes, "classes must not be null");
		return selectClasses(Arrays.asList(classes));
	}

	/**
	 * Create a {@code ClassSelector} for each supplied {@link Class}.
	 *
	 * @param classes the classes to select; never {@code null} and never containing
	 * {@code null} class references
	 * @since 6.0
	 * @see #selectClass(Class)
	 * @see #selectClasses(Class...)
	 * @see ClassSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static List<ClassSelector> selectClasses(List<Class<?>> classes) {
		Preconditions.notNull(classes, "classes must not be null");
		Preconditions.containsNoNullElements(classes, "Individual classes must not be null");

		// @formatter:off
		return classes.stream()
				.distinct()
				.map(DiscoverySelectors::selectClass)
				.toList();
		// @formatter:on
	}

	/**
	 * Create a {@code ClassSelector} for each supplied class name.
	 *
	 * @param classNames the fully qualified names of the classes to select;
	 * never {@code null} and never containing {@code null} or blank names
	 * @since 6.0
	 * @see #selectClass(String)
	 * @see #selectClassesByName(List)
	 * @see #selectClassesByName(ClassLoader, String...)
	 * @see ClassSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static List<ClassSelector> selectClassesByName(String... classNames) {
		Preconditions.notNull(classNames, "classNames must not be null");
		return selectClassesByName(Arrays.asList(classNames));
	}

	/**
	 * Create a {@code ClassSelector} for each supplied class name.
	 *
	 * @param classNames the fully qualified names of the classes to select;
	 * never {@code null} and never containing {@code null} or blank names
	 * @since 6.0
	 * @see #selectClass(String)
	 * @see #selectClassesByName(String...)
	 * @see #selectClassesByName(ClassLoader, List)
	 * @see ClassSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static List<ClassSelector> selectClassesByName(List<String> classNames) {
		Preconditions.notNull(classNames, "classNames must not be null");
		return selectClassesByName(null, classNames);
	}

	/**
	 * Create a {@code ClassSelector} for each supplied class name, using the
	 * supplied class loader.
	 *
	 * @param classLoader the class loader to use to load the classes, or {@code null}
	 * to signal that the default {@code ClassLoader} should be used
	 * @param classNames the fully qualified names of the classes to select;
	 * never {@code null} and never containing {@code null} or blank names
	 * @since 6.0
	 * @see #selectClass(ClassLoader, String)
	 * @see #selectClassesByName(ClassLoader, List)
	 * @see ClassSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static List<ClassSelector> selectClassesByName(@Nullable ClassLoader classLoader, String... classNames) {
		Preconditions.notNull(classNames, "classNames must not be null");
		return selectClassesByName(classLoader, Arrays.asList(classNames));
	}

	/**
	 * Create a {@code ClassSelector} for each supplied class name, using the
	 * supplied class loader.
	 *
	 * @param classLoader the class loader to use to load the classes, or {@code null}
	 * to signal that the default {@code ClassLoader} should be used
	 * @param classNames the fully qualified names of the classes to select;
	 * never {@code null} and never containing {@code null} or blank names
	 * @since 6.0
	 * @see #selectClass(ClassLoader, String)
	 * @see ClassSelector
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static List<ClassSelector> selectClassesByName(@Nullable ClassLoader classLoader, List<String> classNames) {
		Preconditions.notNull(classNames, "classNames must not be null");
		Preconditions.containsNoBlankElements(classNames, "Individual class names must not be null or blank");

		// @formatter:off
		return classNames.stream()
				.distinct()
				.map(className -> selectClass(classLoader, className))
				.toList();
		// @formatter:on
	}

	/**
	 * Create a {@code MethodSelector} for the supplied <em>fully qualified
	 * method name</em>.
	 *
	 * <p>The following formats are supported.
	 *
	 * <ul>
	 * <li>{@code [fully qualified class name]#[methodName]}</li>
	 * <li>{@code [fully qualified class name]#[methodName](parameter type list)}
	 * </ul>
	 *
	 * <p>The <em>parameter type list</em> is a comma-separated list of primitive
	 * names or fully qualified class names for the types of parameters accepted
	 * by the method.
	 *
	 * <p>Array parameter types may be specified using either the JVM's internal
	 * String representation (e.g., {@code [[I} for {@code int[][]},
	 * {@code [Ljava.lang.String;} for {@code java.lang.String[]}, etc.) or
	 * <em>source code syntax</em> (e.g., {@code int[][]}, {@code java.lang.String[]},
	 * etc.).
	 *
	 * <table class="plain">
	 * <caption>Examples</caption>
	 * <tr><th>Method</th><th>Fully Qualified Method Name</th></tr>
	 * <tr><td>{@code java.lang.String.chars()}</td><td>{@code java.lang.String#chars}</td></tr>
	 * <tr><td>{@code java.lang.String.chars()}</td><td>{@code java.lang.String#chars()}</td></tr>
	 * <tr><td>{@code java.lang.String.equalsIgnoreCase(String)}</td><td>{@code java.lang.String#equalsIgnoreCase(java.lang.String)}</td></tr>
	 * <tr><td>{@code java.lang.String.substring(int, int)}</td><td>{@code java.lang.String#substring(int, int)}</td></tr>
	 * <tr><td>{@code example.Calc.avg(int[])}</td><td>{@code example.Calc#avg([I)}</td></tr>
	 * <tr><td>{@code example.Calc.avg(int[])}</td><td>{@code example.Calc#avg(int[])}</td></tr>
	 * <tr><td>{@code example.Matrix.multiply(double[][])}</td><td>{@code example.Matrix#multiply([[D)}</td></tr>
	 * <tr><td>{@code example.Matrix.multiply(double[][])}</td><td>{@code example.Matrix#multiply(double[][])}</td></tr>
	 * <tr><td>{@code example.Service.process(String[])}</td><td>{@code example.Service#process([Ljava.lang.String;)}</td></tr>
	 * <tr><td>{@code example.Service.process(String[])}</td><td>{@code example.Service#process(java.lang.String[])}</td></tr>
	 * <tr><td>{@code example.Service.process(String[][])}</td><td>{@code example.Service#process([[Ljava.lang.String;)}</td></tr>
	 * <tr><td>{@code example.Service.process(String[][])}</td><td>{@code example.Service#process(java.lang.String[][])}</td></tr>
	 * </table>
	 *
	 * @param fullyQualifiedMethodName the fully qualified name of the method to
	 * select; never {@code null} or blank
	 * @see MethodSelector
	 */
	public static MethodSelector selectMethod(String fullyQualifiedMethodName) throws PreconditionViolationException {
		return selectMethod((ClassLoader) null, fullyQualifiedMethodName);
	}

	/**
	 * Create a {@code MethodSelector} for the supplied <em>fully qualified
	 * method name</em> and class loader.
	 *
	 * <p>See {@link #selectMethod(String)} for the supported formats for a
	 * fully qualified method name.
	 *
	 * @param classLoader the class loader to use to load the method's declaring
	 * class, or {@code null} to signal that the default {@code ClassLoader}
	 * should be used
	 * @param fullyQualifiedMethodName the fully qualified name of the method to
	 * select; never {@code null} or blank
	 * @since 1.10
	 * @see #selectMethod(String)
	 * @see MethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static MethodSelector selectMethod(@Nullable ClassLoader classLoader, String fullyQualifiedMethodName)
			throws PreconditionViolationException {
		String[] methodParts = ReflectionUtils.parseFullyQualifiedMethodName(fullyQualifiedMethodName);
		return selectMethod(classLoader, methodParts[0], methodParts[1], methodParts[2]);
	}

	/**
	 * Create a {@code MethodSelector} for the supplied class name and method name
	 * using the default class loader.
	 *
	 * @param className the fully qualified name of the class in which the method
	 * is declared, or a subclass thereof; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @see MethodSelector
	 */
	public static MethodSelector selectMethod(String className, String methodName) {
		return selectMethod((ClassLoader) null, className, methodName);
	}

	/**
	 * Create a {@code MethodSelector} for the supplied class name, method name,
	 * and class loader.
	 *
	 * @param classLoader the class loader to use to load the class, or {@code null}
	 * to signal that the default {@code ClassLoader} should be used
	 * @param className the fully qualified name of the class in which the method
	 * is declared, or a subclass thereof; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @since 1.10
	 * @see MethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static MethodSelector selectMethod(@Nullable ClassLoader classLoader, String className, String methodName) {
		return selectMethod(classLoader, className, methodName, "");
	}

	/**
	 * Create a {@code MethodSelector} for the supplied class name, method name,
	 * and parameter type names.
	 *
	 * <p>The parameter type names {@code String} is typically a comma-separated
	 * list of atomic types, fully qualified class names, or array types; however,
	 * the exact syntax depends on the underlying test engine.
	 *
	 * @param className the fully qualified name of the class in which the method
	 * is declared, or a subclass thereof; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypeNames the parameter type names as a single string; never
	 * {@code null} though potentially an empty string if the method does not declare
	 * parameters
	 * @see MethodSelector
	 */
	public static MethodSelector selectMethod(String className, String methodName, String parameterTypeNames) {
		return selectMethod(null, className, methodName, parameterTypeNames);
	}

	/**
	 * Create a {@code MethodSelector} for the supplied class name, method name,
	 * parameter type names, and class loader.
	 *
	 * <p>The parameter type names {@code String} is typically a comma-separated
	 * list of atomic types, fully qualified class names, or array types; however,
	 * the exact syntax depends on the underlying test engine.
	 *
	 * @param classLoader the class loader to use to load the class, or {@code null}
	 * to signal that the default {@code ClassLoader} should be used
	 * @param className the fully qualified name of the class in which the method
	 * is declared, or a subclass thereof; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypeNames the parameter type names as a single string; never
	 * {@code null} though potentially an empty string if the method does not declare
	 * any parameters
	 * @since 1.10
	 * @see MethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static MethodSelector selectMethod(@Nullable ClassLoader classLoader, String className, String methodName,
			String parameterTypeNames) {
		Preconditions.notBlank(className, "Class name must not be null or blank");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypeNames, "Parameter type names must not be null");
		return new MethodSelector(classLoader, className, methodName, parameterTypeNames.strip());
	}

	/**
	 * Create a {@code MethodSelector} for the supplied {@link Class} and method name.
	 *
	 * @param javaClass the class in which the method is declared, or a subclass thereof;
	 * never {@code null}
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @see MethodSelector
	 */
	public static MethodSelector selectMethod(Class<?> javaClass, String methodName) {
		return selectMethod(javaClass, methodName, "");
	}

	/**
	 * Create a {@code MethodSelector} for the supplied {@link Class}, method name,
	 * and parameter type names.
	 *
	 * <p>The parameter type names {@code String} is typically a comma-separated
	 * list of atomic types, fully qualified class names, or array types; however,
	 * the exact syntax depends on the underlying test engine.
	 *
	 * @param javaClass the class in which the method is declared, or a subclass thereof;
	 * never {@code null}
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypeNames the parameter type names as a single string; never
	 * {@code null} though potentially an empty string if the method does not declare
	 * any parameters
	 * @see MethodSelector
	 */
	public static MethodSelector selectMethod(Class<?> javaClass, String methodName, String parameterTypeNames) {
		Preconditions.notNull(javaClass, "Class must not be null");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypeNames, "Parameter type names must not be null");
		return new MethodSelector(javaClass, methodName, parameterTypeNames.strip());
	}

	/**
	 * Create a {@code MethodSelector} for the supplied class name, method name,
	 * and parameter types.
	 *
	 * @param className the fully qualified name of the class in which the method
	 * is declared, or a subclass thereof; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypes the formal parameter types of the method; never
	 * {@code null} though potentially empty if the method does not declare parameters
	 * @since 1.10
	 * @see MethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static MethodSelector selectMethod(String className, String methodName, Class<?>... parameterTypes) {
		Preconditions.notBlank(className, "Class name must not be null or blank");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
		Preconditions.containsNoNullElements(parameterTypes, "Parameter types array must not contain null elements");
		return new MethodSelector(null, className, methodName, parameterTypes);
	}

	/**
	 * Create a {@code MethodSelector} for the supplied {@link Class}, method name,
	 * and parameter types.
	 *
	 * @param javaClass the class in which the method is declared, or a subclass thereof;
	 * never {@code null}
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypes the formal parameter types of the method; never
	 * {@code null} though potentially empty if the method does not declare parameters
	 * @since 1.10
	 * @see MethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static MethodSelector selectMethod(Class<?> javaClass, String methodName, Class<?>... parameterTypes) {
		Preconditions.notNull(javaClass, "Class must not be null");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
		Preconditions.containsNoNullElements(parameterTypes, "Parameter types array must not contain null elements");
		return new MethodSelector(javaClass, methodName, parameterTypes);
	}

	/**
	 * Create a {@code MethodSelector} for the supplied {@link Class} and {@link Method}.
	 *
	 * @param javaClass the class in which the method is declared, or a subclass thereof;
	 * never {@code null}
	 * @param method the method to select; never {@code null}
	 * @see MethodSelector
	 */
	public static MethodSelector selectMethod(Class<?> javaClass, Method method) {
		Preconditions.notNull(javaClass, "Class must not be null");
		Preconditions.notNull(method, "Method must not be null");
		return new MethodSelector(javaClass, method);
	}

	/**
	 * Create a {@code NestedClassSelector} for the supplied nested {@link Class} and its
	 * enclosing classes.
	 *
	 * @param enclosingClasses the path to the nested class to select; never {@code null} or empty
	 * @param nestedClass the nested class to select; never {@code null}
	 * @since 1.6
	 * @see NestedClassSelector
	 */
	@API(status = STABLE, since = "1.6")
	public static NestedClassSelector selectNestedClass(List<Class<?>> enclosingClasses, Class<?> nestedClass) {
		Preconditions.notEmpty(enclosingClasses, "Enclosing classes must not be null or empty");
		Preconditions.notNull(nestedClass, "Nested class must not be null");
		return new NestedClassSelector(enclosingClasses, nestedClass);
	}

	/**
	 * Create a {@code NestedClassSelector} for the supplied class name and its enclosing
	 * classes' names.
	 *
	 * @param enclosingClassNames the names of the enclosing classes; never {@code null} or empty
	 * @param nestedClassName the name of the nested class to select; never {@code null} or blank
	 * @since 1.6
	 * @see NestedClassSelector
	 */
	@API(status = STABLE, since = "1.6")
	public static NestedClassSelector selectNestedClass(List<String> enclosingClassNames, String nestedClassName) {
		return selectNestedClass(null, enclosingClassNames, nestedClassName);
	}

	/**
	 * Create a {@code NestedClassSelector} for the supplied class name, its enclosing
	 * classes' names, and class loader.
	 *
	 * @param classLoader the class loader to use to load the enclosing and nested classes, or
	 * {@code null} to signal that the default {@code ClassLoader} should be used
	 * @param enclosingClassNames the names of the enclosing classes; never {@code null} or empty
	 * @param nestedClassName the name of the nested class to select; never {@code null} or blank
	 * @since 1.10
	 * @see NestedClassSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static NestedClassSelector selectNestedClass(@Nullable ClassLoader classLoader,
			List<String> enclosingClassNames, String nestedClassName) {
		Preconditions.notEmpty(enclosingClassNames, "Enclosing class names must not be null or empty");
		Preconditions.notBlank(nestedClassName, "Nested class name must not be null or blank");
		return new NestedClassSelector(classLoader, enclosingClassNames, nestedClassName);
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied nested class name and method name.
	 *
	 * @param enclosingClassNames the names of the enclosing classes; never {@code null} or empty
	 * @param nestedClassName the name of the nested class to select; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @since 1.6
	 * @see NestedMethodSelector
	 */
	@API(status = STABLE, since = "1.6")
	public static NestedMethodSelector selectNestedMethod(List<String> enclosingClassNames, String nestedClassName,
			String methodName) {
		return selectNestedMethod(null, enclosingClassNames, nestedClassName, methodName);
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied nested class name, method name,
	 * and class loader.
	 *
	 * @param classLoader the class loader to use to load the method's declaring
	 * class, or {@code null} to signal that the default {@code ClassLoader}
	 * should be used
	 * @param enclosingClassNames the names of the enclosing classes; never {@code null} or empty
	 * @param nestedClassName the name of the nested class to select; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @since 1.10
	 * @see NestedMethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static NestedMethodSelector selectNestedMethod(@Nullable ClassLoader classLoader,
			List<String> enclosingClassNames, String nestedClassName, String methodName)
			throws PreconditionViolationException {
		Preconditions.notEmpty(enclosingClassNames, "Enclosing class names must not be null or empty");
		Preconditions.notBlank(nestedClassName, "Nested class name must not be null or blank");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		return new NestedMethodSelector(classLoader, enclosingClassNames, nestedClassName, methodName, "");
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied nested class name, method name,
	 * and parameter type names.
	 *
	 * <p>The parameter type names {@code String} is typically a comma-separated
	 * list of atomic types, fully qualified class names, or array types; however,
	 * the exact syntax depends on the underlying test engine.
	 *
	 * @param enclosingClassNames the names of the enclosing classes; never {@code null} or empty
	 * @param nestedClassName the name of the nested class to select; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypeNames the parameter type names as a single string; never
	 * {@code null} though potentially an empty string if the method does not declare
	 * parameters
	 * @since 1.6
	 * @see NestedMethodSelector
	 */
	@API(status = STABLE, since = "1.6")
	public static NestedMethodSelector selectNestedMethod(List<String> enclosingClassNames, String nestedClassName,
			String methodName, String parameterTypeNames) {
		return selectNestedMethod(null, enclosingClassNames, nestedClassName, methodName, parameterTypeNames);
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied nested class name, method name,
	 * parameter type names, and class loader.
	 *
	 * @param classLoader the class loader to use to load the method's declaring
	 * class, or {@code null} to signal that the default {@code ClassLoader}
	 * should be used
	 * @param enclosingClassNames the names of the enclosing classes; never {@code null} or empty
	 * @param nestedClassName the name of the nested class to select; never {@code null} or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypeNames the parameter type names as a single string; never
	 * {@code null} though potentially an empty string if the method does not declare
	 * parameters
	 * @since 1.10
	 * @see #selectNestedMethod(List, String, String, String)
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static NestedMethodSelector selectNestedMethod(@Nullable ClassLoader classLoader,
			List<String> enclosingClassNames, String nestedClassName, String methodName, String parameterTypeNames) {

		Preconditions.notEmpty(enclosingClassNames, "Enclosing class names must not be null or empty");
		Preconditions.notBlank(nestedClassName, "Nested class name must not be null or blank");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypeNames, "Parameter types must not be null");
		return new NestedMethodSelector(classLoader, enclosingClassNames, nestedClassName, methodName,
			parameterTypeNames.strip());
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied enclosing class names,
	 * nested class name, method name, and parameter types.
	 *
	 * @param enclosingClassNames the names of the enclosing classes; never {@code null}
	 * or empty
	 * @param nestedClassName the name of the nested class to select; never {@code null}
	 * or blank
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypes the formal parameter types of the method; never {@code null}
	 * though potentially empty if the method does not declare parameters
	 * @since 1.10
	 * @see NestedMethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static NestedMethodSelector selectNestedMethod(List<String> enclosingClassNames, String nestedClassName,
			String methodName, Class<?>... parameterTypes) {

		Preconditions.notEmpty(enclosingClassNames, "Enclosing class names must not be null or empty");
		Preconditions.notBlank(nestedClassName, "Nested class name must not be null or blank");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
		Preconditions.containsNoNullElements(parameterTypes, "Parameter types array must not contain null elements");
		return new NestedMethodSelector(null, enclosingClassNames, nestedClassName, methodName, parameterTypes);
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied nested {@link Class} and method name.
	 *
	 * @param enclosingClasses the path to the nested class to select; never {@code null} or empty
	 * @param nestedClass the nested class to select; never {@code null}
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @since 1.6
	 * @see NestedMethodSelector
	 */
	@API(status = STABLE, since = "1.6")
	public static NestedMethodSelector selectNestedMethod(List<Class<?>> enclosingClasses, Class<?> nestedClass,
			String methodName) {

		return selectNestedMethod(enclosingClasses, nestedClass, methodName, "");
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied {@link Class}, method name,
	 * and parameter type names.
	 *
	 * <p>The parameter type names {@code String} is typically a comma-separated
	 * list of atomic types, fully qualified class names, or array types; however,
	 * the exact syntax depends on the underlying test engine.
	 *
	 * @param enclosingClasses the path to the nested class to select; never {@code null} or empty
	 * @param nestedClass the nested class to select; never {@code null}
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypeNames the parameter type names as a single string; never
	 * {@code null} though potentially an empty string if the method does not declare
	 * parameters
	 * @since 1.6
	 * @see NestedMethodSelector
	 */
	@API(status = STABLE, since = "1.6")
	public static NestedMethodSelector selectNestedMethod(List<Class<?>> enclosingClasses, Class<?> nestedClass,
			String methodName, String parameterTypeNames) {

		Preconditions.notEmpty(enclosingClasses, "Enclosing classes must not be null or empty");
		Preconditions.notNull(nestedClass, "Nested class must not be null");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypeNames, "Parameter types must not be null");
		return new NestedMethodSelector(enclosingClasses, nestedClass, methodName, parameterTypeNames.strip());
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied enclosing classes,
	 * nested class, method name, and parameter types.
	 *
	 * @param enclosingClasses the path to the nested class to select; never {@code null}
	 * or empty
	 * @param nestedClass the nested class to select; never {@code null}
	 * @param methodName the name of the method to select; never {@code null} or blank
	 * @param parameterTypes the formal parameter types of the method; never {@code null}
	 * though potentially empty if the method does not declare parameters
	 * @since 1.10
	 * @see NestedMethodSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static NestedMethodSelector selectNestedMethod(List<Class<?>> enclosingClasses, Class<?> nestedClass,
			String methodName, Class<?>... parameterTypes) {

		Preconditions.notEmpty(enclosingClasses, "Enclosing classes must not be null or empty");
		Preconditions.notNull(nestedClass, "Nested class must not be null");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
		Preconditions.containsNoNullElements(parameterTypes, "Parameter types array must not contain null elements");
		return new NestedMethodSelector(enclosingClasses, nestedClass, methodName, parameterTypes);
	}

	/**
	 * Create a {@code NestedMethodSelector} for the supplied nested {@link Class} and {@link Method}.
	 *
	 * @param enclosingClasses the path to the nested class to select; never {@code null} or empty
	 * @param nestedClass the nested class to select; never {@code null}
	 * @param method the method to select; never {@code null}
	 * @since 1.6
	 * @see NestedMethodSelector
	 */
	@API(status = STABLE, since = "1.6")
	public static NestedMethodSelector selectNestedMethod(List<Class<?>> enclosingClasses, Class<?> nestedClass,
			Method method) {

		Preconditions.notEmpty(enclosingClasses, "Enclosing classes must not be null or empty");
		Preconditions.notNull(nestedClass, "Nested class must not be null");
		Preconditions.notNull(method, "Method must not be null");
		return new NestedMethodSelector(enclosingClasses, nestedClass, method);
	}

	/**
	 * Create a {@code UniqueIdSelector} for the supplied {@link UniqueId}.
	 *
	 * @param uniqueId the {@code UniqueId} to select; never {@code null}
	 * @see UniqueIdSelector
	 */
	public static UniqueIdSelector selectUniqueId(UniqueId uniqueId) {
		Preconditions.notNull(uniqueId, "UniqueId must not be null");
		return new UniqueIdSelector(uniqueId);
	}

	/**
	 * Create a {@code UniqueIdSelector} for the supplied unique ID.
	 *
	 * @param uniqueId the unique ID to select; never {@code null} or blank
	 * @see UniqueIdSelector
	 */
	public static UniqueIdSelector selectUniqueId(String uniqueId) {
		Preconditions.notBlank(uniqueId, "Unique ID must not be null or blank");
		return new UniqueIdSelector(UniqueId.parse(uniqueId));
	}

	/**
	 * Create an {@code IterationSelector} for the supplied parent selector and
	 * iteration indices.
	 *
	 * @param parentSelector the parent selector to select iterations for; never
	 * {@code null}
	 * @param iterationIndices the iteration indices to select; never {@code null}
	 * or empty
	 * @since 1.9
	 * @see IterationSelector
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static IterationSelector selectIteration(DiscoverySelector parentSelector, int... iterationIndices) {
		Preconditions.notNull(parentSelector, "Parent selector must not be null");
		Preconditions.notEmpty(iterationIndices, "iteration indices must not be empty");
		return new IterationSelector(parentSelector, iterationIndices);
	}

	/**
	 * Parse the supplied string representation of a {@link DiscoverySelectorIdentifier}.
	 *
	 * @param identifier the string representation of a {@code DiscoverySelectorIdentifier};
	 * never {@code null} or blank
	 * @return an {@link Optional} containing the corresponding {@link DiscoverySelector};
	 * never {@code null} but potentially empty
	 * @since 1.11
	 * @see DiscoverySelectorIdentifierParser
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static Optional<? extends DiscoverySelector> parse(String identifier) {
		return DiscoverySelectorIdentifierParsers.parse(identifier);
	}

	/**
	 * Parse the supplied {@link DiscoverySelectorIdentifier}.
	 *
	 * @param identifier the {@code DiscoverySelectorIdentifier} to parse;
	 * never {@code null}
	 * @return an {@link Optional} containing the corresponding {@link DiscoverySelector};
	 * never {@code null} but potentially empty
	 * @since 1.11
	 * @see DiscoverySelectorIdentifierParser
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static Optional<? extends DiscoverySelector> parse(DiscoverySelectorIdentifier identifier) {
		return DiscoverySelectorIdentifierParsers.parse(identifier);
	}

	/**
	 * Parse the supplied string representations of
	 * {@link DiscoverySelectorIdentifier DiscoverySelectorIdentifiers}.
	 *
	 * @param identifiers the string representations of
	 * {@code DiscoverySelectorIdentifiers} to parse; never {@code null}
	 * @return a stream of the corresponding {@link DiscoverySelector DiscoverySelectors};
	 * never {@code null} but potentially empty
	 * @since 1.11
	 * @see DiscoverySelectorIdentifierParser
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static Stream<? extends DiscoverySelector> parseAll(String... identifiers) {
		return DiscoverySelectorIdentifierParsers.parseAll(identifiers);
	}

	/**
	 * Parse the supplied {@link DiscoverySelectorIdentifier
	 * DiscoverySelectorIdentifiers}.
	 *
	 * @param identifiers the {@code DiscoverySelectorIdentifiers} to parse;
	 * never {@code null}
	 * @return a stream of the corresponding {@link DiscoverySelector DiscoverySelectors};
	 * never {@code null} but potentially empty
	 * @since 1.11
	 * @see DiscoverySelectorIdentifierParser
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static Stream<? extends DiscoverySelector> parseAll(Collection<DiscoverySelectorIdentifier> identifiers) {
		return DiscoverySelectorIdentifierParsers.parseAll(identifiers);
	}

}
