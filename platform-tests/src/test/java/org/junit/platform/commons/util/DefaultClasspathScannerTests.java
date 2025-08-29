/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.platform.commons.test.ConcurrencyTestingUtils.executeConcurrently;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.spi.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.DisabledInEclipse;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.support.scanning.ClassFilter;

/**
 * Unit tests for {@link DefaultClasspathScanner}.
 *
 * @since 1.0
 */
@TrackLogRecords
class DefaultClasspathScannerTests {

	private static final ClassFilter allClasses = ClassFilter.of(__ -> true);
	private static final ResourceFilter allResources = ResourceFilter.of(__ -> true);

	private final List<Class<?>> loadedClasses = new ArrayList<>();

	private final BiFunction<String, ClassLoader, Try<Class<?>>> trackingClassLoader = (name,
			classLoader) -> ReflectionUtils.tryToLoadClass(name, classLoader).ifSuccess(loadedClasses::add);

	private final DefaultClasspathScanner classpathScanner = new DefaultClasspathScanner(
		ClassLoaderUtils::getDefaultClassLoader, trackingClassLoader);

	@Test
	void scanForClassesInClasspathRootWhenMalformedClassnameInternalErrorOccursWithNullDetailedMessage(
			LogRecordListener listener) throws Exception {

		Predicate<Class<?>> malformedClassNameSimulationFilter = clazz -> {
			if (clazz.getSimpleName().equals(ClassForMalformedClassNameSimulation.class.getSimpleName())) {
				throw new InternalError();
			}
			return true;
		};

		assertClassesScannedWhenExceptionIsThrown(malformedClassNameSimulationFilter);
		assertDebugMessageLogged(listener, "Failed to load .+ during classpath scanning.");
	}

	@Test
	void scanForClassesInClasspathRootWhenMalformedClassnameInternalErrorOccurs(LogRecordListener listener)
			throws Exception {

		Predicate<Class<?>> malformedClassNameSimulationFilter = clazz -> {
			if (clazz.getSimpleName().equals(ClassForMalformedClassNameSimulation.class.getSimpleName())) {
				throw new InternalError("Malformed class name");
			}
			return true;
		};

		assertClassesScannedWhenExceptionIsThrown(malformedClassNameSimulationFilter);
		assertDebugMessageLogged(listener, "The java.lang.Class loaded from path .+ has a malformed class name .+");
	}

	@Test
	void scanForClassesInClasspathRootWhenOtherInternalErrorOccurs(LogRecordListener listener) throws Exception {
		Predicate<Class<?>> otherInternalErrorSimulationFilter = clazz -> {
			if (clazz.getSimpleName().equals(ClassForOtherInternalErrorSimulation.class.getSimpleName())) {
				throw new InternalError("other internal error");
			}
			return true;
		};

		assertClassesScannedWhenExceptionIsThrown(otherInternalErrorSimulationFilter);
		assertDebugMessageLogged(listener, "Failed to load .+ during classpath scanning.");
	}

	@Test
	void scanForClassesInClasspathRootWhenGenericRuntimeExceptionOccurs(LogRecordListener listener) throws Exception {
		Predicate<Class<?>> runtimeExceptionSimulationFilter = clazz -> {
			if (clazz.getSimpleName().equals(ClassForGenericRuntimeExceptionSimulation.class.getSimpleName())) {
				throw new RuntimeException("a generic exception");
			}
			return true;
		};

		assertClassesScannedWhenExceptionIsThrown(runtimeExceptionSimulationFilter);
		assertDebugMessageLogged(listener, "Failed to load .+ during classpath scanning.");
	}

	private void assertClassesScannedWhenExceptionIsThrown(Predicate<Class<?>> filter) throws Exception {
		var classFilter = ClassFilter.of(filter);
		var classes = this.classpathScanner.scanForClassesInClasspathRoot(getTestClasspathRoot(), classFilter);
		assertThat(classes).hasSizeGreaterThanOrEqualTo(150);
	}

	@Test
	void scanForResourcesInClasspathRootWhenGenericRuntimeExceptionOccurs(LogRecordListener listener) throws Exception {
		Predicate<Resource> runtimeExceptionSimulationFilter = resource -> {
			if (resource.getName().equals("org/junit/platform/commons/other-example.resource")) {
				throw new RuntimeException("a generic exception");
			}
			return true;
		};

		assertResourcesScannedWhenExceptionIsThrown(runtimeExceptionSimulationFilter);
		assertDebugMessageLogged(listener, "Failed to load .+ during classpath scanning.");
	}

	private void assertResourcesScannedWhenExceptionIsThrown(Predicate<Resource> filter) {
		var resources = this.classpathScanner.scanForResourcesInClasspathRoot(getTestClasspathResourceRoot(),
			ResourceFilter.of(filter));
		assertThat(resources).hasSizeGreaterThanOrEqualTo(150);
	}

	private void assertDebugMessageLogged(LogRecordListener listener, String regex) {
		// @formatter:off
		assertThat(listener.stream(DefaultClasspathScanner.class, Level.FINE)
				.map(LogRecord::getMessage)
				.filter(m -> m.matches(regex))
		).hasSize(1);
		// @formatter:on
	}

	@Test
	void scanForClassesInClasspathRootWhenOutOfMemoryErrorOccurs() {
		Predicate<Class<?>> outOfMemoryErrorSimulationFilter = clazz -> {
			if (clazz.getSimpleName().equals(ClassForOutOfMemoryErrorSimulation.class.getSimpleName())) {
				throw new OutOfMemoryError();
			}
			return true;
		};
		var classFilter = ClassFilter.of(outOfMemoryErrorSimulationFilter);

		assertThrows(OutOfMemoryError.class,
			() -> this.classpathScanner.scanForClassesInClasspathRoot(getTestClasspathRoot(), classFilter));
	}

	@Test
	void scanForClassesInClasspathRootWithinJarFile() throws Exception {
		scanForClassesInClasspathRootWithinJarFile("/jartest.jar");
	}

	@Test
	void scanForClassesInClasspathRootWithinJarWithSpacesInPath() throws Exception {
		scanForClassesInClasspathRootWithinJarFile("/folder with spaces/jar test with spaces.jar");
	}

	private void scanForClassesInClasspathRootWithinJarFile(String resourceName) throws Exception {
		var jarfile = getClass().getResource(resourceName);

		try (var classLoader = new URLClassLoader(new URL[] { jarfile }, null)) {
			var classpathScanner = new DefaultClasspathScanner(() -> classLoader, ReflectionUtils::tryToLoadClass);

			var classes = classpathScanner.scanForClassesInClasspathRoot(jarfile.toURI(), allClasses);
			assertThat(classes).extracting(Class::getName) //
					.containsExactlyInAnyOrder("org.junit.platform.jartest.notincluded.NotIncluded",
						"org.junit.platform.jartest.included.recursive.RecursivelyIncluded",
						"org.junit.platform.jartest.included.Included");
		}
	}

	@Test
	void scanForResourcesInClasspathRootWithinJarFile() throws Exception {
		scanForResourcesInClasspathRootWithinJarFile("/jartest.jar");
	}

	@Test
	void scanForResourcesInClasspathRootWithinJarWithSpacesInPath() throws Exception {
		scanForResourcesInClasspathRootWithinJarFile("/folder with spaces/jar test with spaces.jar");
	}

	private void scanForResourcesInClasspathRootWithinJarFile(String resourceName) throws Exception {
		var jarfile = getClass().getResource(resourceName);

		try (var classLoader = new URLClassLoader(new URL[] { jarfile }, null)) {
			var classpathScanner = new DefaultClasspathScanner(() -> classLoader, ReflectionUtils::tryToLoadClass);

			var resources = classpathScanner.scanForResourcesInClasspathRoot(jarfile.toURI(), allResources);
			assertThat(resources).extracting(Resource::getName) //
					.containsExactlyInAnyOrder("org/junit/platform/jartest/notincluded/not-included.resource",
						"org/junit/platform/jartest/included/included.resource",
						"org/junit/platform/jartest/included/recursive/recursively-included.resource",
						"META-INF/MANIFEST.MF");
		}
	}

	@Test
	void scanForResourcesInShadowedClassPathRoot() throws Exception {
		var jarFile = getClass().getResource("/jartest.jar");
		var shadowedJarFile = getClass().getResource("/jartest-shadowed.jar");

		try (var classLoader = new URLClassLoader(new URL[] { jarFile, shadowedJarFile }, null)) {
			var classpathScanner = new DefaultClasspathScanner(() -> classLoader, ReflectionUtils::tryToLoadClass);

			var resources = classpathScanner.scanForResourcesInClasspathRoot(shadowedJarFile.toURI(), allResources);
			assertThat(resources).extracting(Resource::getName).containsExactlyInAnyOrder(
				"org/junit/platform/jartest/included/unique.resource", //
				"org/junit/platform/jartest/included/included.resource", //
				"org/junit/platform/jartest/included/recursive/recursively-included.resource", //
				"META-INF/MANIFEST.MF");

			assertThat(resources).extracting(Resource::getUri) //
					.map(DefaultClasspathScannerTests::jarFileAndEntry) //
					.containsExactlyInAnyOrder(
						// This resource only exists in the shadowed jar file
						"jartest-shadowed.jar!/org/junit/platform/jartest/included/unique.resource",
						// These resources exist in both the jar and shadowed jar file.
						// They must be discovered in the shadowed jar as we're searching in that classpath root.
						"jartest-shadowed.jar!/org/junit/platform/jartest/included/included.resource",
						"jartest-shadowed.jar!/org/junit/platform/jartest/included/recursive/recursively-included.resource",
						"jartest-shadowed.jar!/META-INF/MANIFEST.MF");
		}
	}

	@Test
	void scanForResourcesInPackageWithDuplicateResources() throws Exception {
		var jarFile = getClass().getResource("/jartest.jar");
		var shadowedJarFile = getClass().getResource("/jartest-shadowed.jar");

		try (var classLoader = new URLClassLoader(new URL[] { jarFile, shadowedJarFile }, null)) {
			var classpathScanner = new DefaultClasspathScanner(() -> classLoader, ReflectionUtils::tryToLoadClass);

			var resources = classpathScanner.scanForResourcesInPackage("org.junit.platform.jartest.included",
				allResources);

			assertThat(resources).extracting(Resource::getUri) //
					.map(DefaultClasspathScannerTests::jarFileAndEntry) //
					.containsExactlyInAnyOrder(
						// This resource only exists in the shadowed jar file
						"jartest-shadowed.jar!/org/junit/platform/jartest/included/unique.resource",
						// These resources exist in both the jar and shadowed jar file.
						"jartest.jar!/org/junit/platform/jartest/included/included.resource",
						"jartest-shadowed.jar!/org/junit/platform/jartest/included/included.resource",
						"jartest.jar!/org/junit/platform/jartest/included/recursive/recursively-included.resource",
						"jartest-shadowed.jar!/org/junit/platform/jartest/included/recursive/recursively-included.resource");
		}
	}

	private static String jarFileAndEntry(URI uri) {
		var uriString = uri.toString();
		int lastJarUriSeparator = uriString.lastIndexOf("!/");
		var jarUri = uriString.substring(0, lastJarUriSeparator);
		var jarEntry = uriString.substring(lastJarUriSeparator + 1);
		var fileName = jarUri.substring(jarUri.lastIndexOf("/") + 1);
		return fileName + "!" + jarEntry;
	}

	@Test
	void scanForClassesInPackage() {
		var classes = classpathScanner.scanForClassesInPackage("org.junit.platform.commons", allClasses);
		assertThat(classes).hasSizeGreaterThanOrEqualTo(20);
		assertTrue(classes.contains(NestedClassToBeFound.class));
		assertTrue(classes.contains(MemberClassToBeFound.class));
	}

	@Test
	void scanForResourcesInPackage() {
		var resources = classpathScanner.scanForResourcesInPackage("org.junit.platform.commons", allResources);
		assertThat(resources).extracting(Resource::getUri).containsExactlyInAnyOrder(
			uriOf("/org/junit/platform/commons/example.resource"),
			uriOf("/org/junit/platform/commons/other-example.resource"));
	}

	@Test // #2500
	@DisabledInEclipse
	void scanForClassesInPackageWithinModulesSharingNamePrefix(@TempDir Path temp) throws Exception {
		var moduleSourcePath = Path.of(getClass().getResource("/modules-2500/").toURI()).toString();
		run("javac", "--module", "foo,foo.bar", "--module-source-path", moduleSourcePath, "-d", temp.toString());

		checkModules2500(ModuleFinder.of(temp)); // exploded modules

		var foo = temp.resolve("foo.jar");
		var bar = temp.resolve("foo.bar.jar");
		run("jar", "--create", "--file", foo.toString(), "-C", temp.resolve("foo").toString(), ".");
		run("jar", "--create", "--file", bar.toString(), "-C", temp.resolve("foo.bar").toString(), ".");

		checkModules2500(ModuleFinder.of(foo, bar)); // jarred modules

		System.gc(); // required on Windows in order to release JAR file handles
	}

	private static int run(String tool, String... args) {
		return ToolProvider.findFirst(tool).orElseThrow().run(System.out, System.err, args);
	}

	private void checkModules2500(ModuleFinder finder) {
		var root = "foo.bar";
		var before = ModuleFinder.of();
		var boot = ModuleLayer.boot();
		var configuration = boot.configuration().resolve(before, finder, Set.of(root));
		var parent = ClassLoader.getPlatformClassLoader();
		var layer = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent).layer();

		var classpathScanner = new DefaultClasspathScanner(() -> layer.findLoader(root),
			ReflectionUtils::tryToLoadClass);
		{
			var classes = classpathScanner.scanForClassesInPackage("foo", allClasses);
			var classNames = classes.stream().map(Class::getName).toList();
			assertThat(classNames).hasSize(2).contains("foo.Foo", "foo.bar.FooBar");
		}
		{
			var classes = classpathScanner.scanForClassesInPackage("foo.bar", allClasses);
			var classNames = classes.stream().map(Class::getName).toList();
			assertThat(classNames).hasSize(1).contains("foo.bar.FooBar");
		}
	}

	@Test
	void findAllClassesInPackageWithinJarFileConcurrently() throws Exception {
		var jarFile = getClass().getResource("/jartest.jar");
		var jarUri = URI.create("jar:" + jarFile);

		try (var classLoader = new URLClassLoader(new URL[] { jarFile })) {
			var classpathScanner = new DefaultClasspathScanner(() -> classLoader, ReflectionUtils::tryToLoadClass);

			var results = executeConcurrently(10,
				() -> classpathScanner.scanForClassesInPackage("org.junit.platform.jartest.included", allClasses));

			assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri),
				"FileSystem should be closed");

			results.forEach(classes -> assertThat(classes) //
					.hasSize(2) //
					.extracting(Class::getSimpleName) //
					.containsExactlyInAnyOrder("Included", "RecursivelyIncluded"));
		}
	}

	@Test
	void findAllResourcesInPackageWithinJarFileConcurrently() throws Exception {
		var jarFile = getClass().getResource("/jartest.jar");
		var jarUri = URI.create("jar:" + jarFile);

		try (var classLoader = new URLClassLoader(new URL[] { jarFile })) {
			var classpathScanner = new DefaultClasspathScanner(() -> classLoader, ReflectionUtils::tryToLoadClass);

			var results = executeConcurrently(10,
				() -> classpathScanner.scanForResourcesInPackage("org.junit.platform.jartest.included", allResources));

			assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri),
				"FileSystem should be closed");

			// @formatter:off
			results.forEach(resources -> assertThat(resources)
					.hasSize(2)
					.extracting(Resource::getName).containsExactlyInAnyOrder(
							"org/junit/platform/jartest/included/included.resource",
							"org/junit/platform/jartest/included/recursive/recursively-included.resource"
					));
			// @formatter:on
		}
	}

	@Test
	void scanForClassesInDefaultPackage() {
		var classFilter = ClassFilter.of(this::inDefaultPackage);
		var classes = classpathScanner.scanForClassesInPackage("", classFilter);

		assertThat(classes).as("number of classes found in default package").isNotEmpty();
		assertTrue(classes.stream().allMatch(this::inDefaultPackage));
		assertTrue(classes.stream().anyMatch(clazz -> "DefaultPackageTestCase".equals(clazz.getName())));
	}

	@Test
	void scanForResourcesInDefaultPackage() {
		var resourceFilter = ResourceFilter.of(this::inDefaultPackage);
		var resources = classpathScanner.scanForResourcesInPackage("", resourceFilter);

		assertThat(resources).as("number of resources found in default package").isNotEmpty();
		assertTrue(resources.stream().allMatch(this::inDefaultPackage));
		assertTrue(resources.stream().anyMatch(resource -> "default-package.resource".equals(resource.getName())));
	}

	@Test
	void scanForClassesInPackageWithFilter() {
		var thisClassOnly = ClassFilter.of(clazz -> clazz == DefaultClasspathScannerTests.class);
		var classes = classpathScanner.scanForClassesInPackage("org.junit.platform.commons", thisClassOnly);
		assertSame(DefaultClasspathScannerTests.class, classes.getFirst());
	}

	@Test
	void scanForResourcesInPackageWithFilter() {
		var thisResourceOnly = ResourceFilter.of(
			resource -> "org/junit/platform/commons/example.resource".equals(resource.getName()));
		var resources = classpathScanner.scanForResourcesInPackage("org.junit.platform.commons", thisResourceOnly);
		assertThat(resources).extracting(Resource::getName).containsExactly(
			"org/junit/platform/commons/example.resource");
	}

	@Test
	void resourcesCanBeRead() throws IOException {
		var thisResourceOnly = ResourceFilter.of(
			resource -> "org/junit/platform/commons/example.resource".equals(resource.getName()));
		var resources = classpathScanner.scanForResourcesInPackage("org.junit.platform.commons", thisResourceOnly);
		Resource resource = resources.getFirst();

		assertThat(resource.getName()).isEqualTo("org/junit/platform/commons/example.resource");
		assertThat(resource.getUri()).isEqualTo(uriOf("/org/junit/platform/commons/example.resource"));
		try (InputStream is = resource.getInputStream()) {
			String contents = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(contents).isEqualTo("This file was unintentionally left blank.\n");
		}
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void scanForClassesInPackageForNullBasePackage() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForClassesInPackage(null, allClasses));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void scanForResourcesInPackageForNullBasePackage() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForResourcesInPackage(null, allResources));
	}

	@Test
	void scanForClassesInPackageForWhitespaceBasePackage() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForClassesInPackage("    ", allClasses));
	}

	@Test
	void scanForResourcesInPackageForWhitespaceBasePackage() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForResourcesInPackage("    ", allResources));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void scanForClassesInPackageForNullClassFilter() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForClassesInPackage("org.junit.platform.commons", null));
	}

	@Test
	void scanForClassesInPackageWhenIOExceptionOccurs() {
		var scanner = new DefaultClasspathScanner(ThrowingClassLoader::new, ReflectionUtils::tryToLoadClass);
		var classes = scanner.scanForClassesInPackage("org.junit.platform.commons", allClasses);
		assertThat(classes).isEmpty();
	}

	@Test
	void scanForResourcesInPackageWhenIOExceptionOccurs() {
		var scanner = new DefaultClasspathScanner(ThrowingClassLoader::new, ReflectionUtils::tryToLoadClass);
		var classes = scanner.scanForResourcesInPackage("org.junit.platform.commons", allResources);
		assertThat(classes).isEmpty();
	}

	@Test
	void scanForClassesInPackageOnlyLoadsClassesThatAreIncludedByTheClassNameFilter() {
		Predicate<String> classNameFilter = name -> DefaultClasspathScannerTests.class.getName().equals(name);
		var classFilter = ClassFilter.of(classNameFilter, type -> true);

		classpathScanner.scanForClassesInPackage("org.junit.platform.commons", classFilter);

		assertThat(loadedClasses).containsExactly(DefaultClasspathScannerTests.class);
	}

	@Test
	void findAllClassesInClasspathRoot() throws Exception {
		var thisClassOnly = ClassFilter.of(clazz -> clazz == DefaultClasspathScannerTests.class);
		var root = getTestClasspathRoot();
		var classes = classpathScanner.scanForClassesInClasspathRoot(root, thisClassOnly);
		assertSame(DefaultClasspathScannerTests.class, classes.getFirst());
	}

	@Test
	void findAllClassesInDefaultPackageInClasspathRoot() throws Exception {
		var classFilter = ClassFilter.of(this::inDefaultPackage);
		var classes = classpathScanner.scanForClassesInClasspathRoot(getTestClasspathRoot(), classFilter);

		assertEquals(1, classes.size(), "number of classes found in default package");
		var testClass = classes.getFirst();
		assertTrue(inDefaultPackage(testClass));
		assertEquals("DefaultPackageTestCase", testClass.getName());
	}

	@Test
	void doesNotLoopInfinitelyWithCircularSymlinks(@TempDir Path tempDir) throws Exception {

		// Abort if running on Microsoft Windows since we are testing symbolic links
		assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));

		var directory = Files.createDirectory(tempDir.resolve("directory"));
		var symlink1 = Files.createSymbolicLink(tempDir.resolve("symlink1"), directory);
		Files.createSymbolicLink(directory.resolve("symlink2"), symlink1);

		var classes = classpathScanner.scanForClassesInClasspathRoot(symlink1.toUri(), allClasses);

		assertThat(classes).isEmpty();
	}

	private boolean inDefaultPackage(Class<?> clazz) {
		// OpenJDK returns NULL for the default package.
		var pkg = clazz.getPackage();
		return pkg == null || "".equals(clazz.getPackage().getName());
	}

	private boolean inDefaultPackage(Resource resource) {
		return !resource.getName().contains("/");
	}

	@Test
	void findAllClassesInClasspathRootWithFilter() throws Exception {
		var root = getTestClasspathRoot();
		var classes = classpathScanner.scanForClassesInClasspathRoot(root, allClasses);

		assertThat(classes).hasSizeGreaterThanOrEqualTo(20);
		assertTrue(classes.contains(DefaultClasspathScannerTests.class));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllClassesInClasspathRootForNullRoot() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForClassesInClasspathRoot(null, allClasses));
	}

	@Test
	void findAllClassesInClasspathRootForNonExistingRoot() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForClassesInClasspathRoot(Path.of("does_not_exist").toUri(), allClasses));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllClassesInClasspathRootForNullClassFilter() {
		assertThrows(PreconditionViolationException.class,
			() -> classpathScanner.scanForClassesInClasspathRoot(getTestClasspathRoot(), null));
	}

	@Test
	void onlyLoadsClassesInClasspathRootThatAreIncludedByTheClassNameFilter() throws Exception {
		var classFilter = ClassFilter.of(name -> DefaultClasspathScannerTests.class.getName().equals(name),
			type -> true);
		var root = getTestClasspathRoot();

		classpathScanner.scanForClassesInClasspathRoot(root, classFilter);

		assertThat(loadedClasses).containsExactly(DefaultClasspathScannerTests.class);
	}

	private static URI uriOf(String name) {
		var resource = DefaultClasspathScannerTests.class.getResource(name);
		try {
			return requireNonNull(resource).toURI();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private URI getTestClasspathRoot() throws Exception {
		var location = getClass().getProtectionDomain().getCodeSource().getLocation();
		return location.toURI();
	}

	private URI getTestClasspathResourceRoot() {
		// Gradle puts classes and resources in different roots.
		var defaultPackageResource = "/default-package.resource";
		var resourceUri = getClass().getResource(defaultPackageResource).toString();
		return URI.create(resourceUri.substring(0, resourceUri.length() - defaultPackageResource.length()));
	}

	class MemberClassToBeFound {
	}

	static class NestedClassToBeFound {
	}

	static class ClassForMalformedClassNameSimulation {
	}

	static class ClassForOtherInternalErrorSimulation {
	}

	static class ClassForGenericRuntimeExceptionSimulation {
	}

	static class ClassForOutOfMemoryErrorSimulation {
	}

	private static class ThrowingClassLoader extends ClassLoader {

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			throw new IOException("Demo I/O error");
		}
	}

}
