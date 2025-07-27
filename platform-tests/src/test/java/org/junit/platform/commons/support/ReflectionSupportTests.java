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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.support.PreconditionAssertions.assertPreconditionViolationException;
import static org.junit.platform.commons.support.PreconditionAssertions.assertPreconditionViolationExceptionForString;
import static org.junit.platform.commons.util.ClassLoaderUtils.getDefaultClassLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 1.0
 */
class ReflectionSupportTests {

	private static final Predicate<Class<?>> allTypes = type -> true;
	private static final Predicate<Resource> allResources = type -> true;
	private static final Predicate<String> allNames = name -> true;
	private static final Predicate<Method> allMethods = name -> true;
	private static final Predicate<Field> allFields = name -> true;

	static final String staticField = "static";
	final String instanceField = "instance";

	@Test
	void tryToLoadClassDelegates() {
		assertEquals(ReflectionUtils.tryToLoadClass("-").toOptional(),
			ReflectionSupport.tryToLoadClass("-").toOptional());
		assertEquals(ReflectionUtils.tryToLoadClass("A").toOptional(),
			ReflectionSupport.tryToLoadClass("A").toOptional());
		assertEquals(ReflectionUtils.tryToLoadClass("java.nio.Bits"),
			ReflectionSupport.tryToLoadClass("java.nio.Bits"));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void tryToLoadClassPreconditions() {
		assertPreconditionViolationExceptionForString("Class name", () -> ReflectionSupport.tryToLoadClass(null));
		assertPreconditionViolationExceptionForString("Class name", () -> ReflectionSupport.tryToLoadClass(""));
	}

	/**
	 * @since 1.10
	 */
	@Test
	void tryToLoadClassWithExplicitClassLoaderDelegates() {
		ClassLoader classLoader = getClass().getClassLoader();

		assertEquals(ReflectionUtils.tryToLoadClass("-", classLoader).toOptional(),
			ReflectionSupport.tryToLoadClass("-", classLoader).toOptional());
		assertEquals(ReflectionUtils.tryToLoadClass("A", classLoader).toOptional(),
			ReflectionSupport.tryToLoadClass("A", classLoader).toOptional());
		assertEquals(ReflectionUtils.tryToLoadClass("java.nio.Bits", classLoader),
			ReflectionSupport.tryToLoadClass("java.nio.Bits", classLoader));
	}

	/**
	 * @since 1.10
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void tryToLoadClassWithExplicitClassLoaderPreconditions() {
		var cl = getClass().getClassLoader();

		assertPreconditionViolationExceptionForString("Class name", () -> ReflectionSupport.tryToLoadClass(null, cl));
		assertPreconditionViolationExceptionForString("Class name", () -> ReflectionSupport.tryToLoadClass("", cl));

		assertPreconditionViolationException("ClassLoader", () -> ReflectionSupport.tryToLoadClass("int", null));
	}

	@TestFactory
	List<DynamicTest> findAllClassesInClasspathRootDelegates() throws Throwable {
		List<DynamicTest> tests = new ArrayList<>();
		List<Path> paths = new ArrayList<>();
		paths.add(Path.of(".").toRealPath());
		paths.addAll(ReflectionUtils.getAllClasspathRootDirectories());
		for (var path : paths) {
			var root = path.toUri();
			tests.add(DynamicTest.dynamicTest(createDisplayName(root),
				() -> assertEquals(ReflectionUtils.findAllClassesInClasspathRoot(root, allTypes, allNames),
					ReflectionSupport.findAllClassesInClasspathRoot(root, allTypes, allNames))));
		}
		return tests;
	}

	/**
	 * @since 1.12
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void tryToGetResourcesPreconditions() {
		assertPreconditionViolationExceptionForString("Resource name", () -> ReflectionSupport.tryToGetResources(null));
		assertPreconditionViolationExceptionForString("Resource name", () -> ReflectionSupport.tryToGetResources(""));
		assertPreconditionViolationException("Class loader",
			() -> ReflectionSupport.tryToGetResources("default-package.resource", null));
		assertPreconditionViolationException("Class loader",
			() -> ReflectionSupport.tryToGetResources("default-package.resource", null));
	}

	/**
	 * @since 1.12
	 */
	@Test
	void tryToGetResources() {
		assertEquals(ReflectionUtils.tryToGetResources("default-package.resource").toOptional(),
			ReflectionSupport.tryToGetResources("default-package.resource").toOptional());
		assertEquals(
			ReflectionUtils.tryToGetResources("default-package.resource", getDefaultClassLoader()).toOptional(), //
			ReflectionSupport.tryToGetResources("default-package.resource", getDefaultClassLoader()).toOptional());
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllClassesInClasspathRootPreconditions() {
		var path = Path.of(".").toUri();
		assertPreconditionViolationException("root",
			() -> ReflectionSupport.findAllClassesInClasspathRoot(null, allTypes, allNames));
		assertPreconditionViolationException("class predicate",
			() -> ReflectionSupport.findAllClassesInClasspathRoot(path, null, allNames));
		assertPreconditionViolationException("name predicate",
			() -> ReflectionSupport.findAllClassesInClasspathRoot(path, allTypes, null));
	}

	/**
	 * @since 1.11
	 */
	@TestFactory
	List<DynamicTest> findAllResourcesInClasspathRootDelegates() throws Throwable {
		List<DynamicTest> tests = new ArrayList<>();
		List<Path> paths = new ArrayList<>();
		paths.add(Path.of(".").toRealPath());
		paths.addAll(ReflectionUtils.getAllClasspathRootDirectories());
		for (var path : paths) {
			var root = path.toUri();
			tests.add(DynamicTest.dynamicTest(createDisplayName(root),
				() -> assertThat(ReflectionUtils.findAllResourcesInClasspathRoot(root, allResources)) //
						.containsExactlyElementsOf(
							ReflectionSupport.findAllResourcesInClasspathRoot(root, allResources))));
		}
		return tests;
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllResourcesInClasspathRootPreconditions() {
		var path = Path.of(".").toUri();
		assertPreconditionViolationException("root",
			() -> ReflectionSupport.findAllResourcesInClasspathRoot(null, allResources));
		assertPreconditionViolationException("resourceFilter",
			() -> ReflectionSupport.findAllResourcesInClasspathRoot(path, null));
	}

	/**
	 * @since 1.11
	 */
	@TestFactory
	List<DynamicTest> streamAllResourcesInClasspathRootDelegates() throws Throwable {
		List<DynamicTest> tests = new ArrayList<>();
		List<Path> paths = new ArrayList<>();
		paths.add(Path.of(".").toRealPath());
		paths.addAll(ReflectionUtils.getAllClasspathRootDirectories());
		for (var path : paths) {
			var root = path.toUri();
			tests.add(DynamicTest.dynamicTest(createDisplayName(root),
				() -> assertThat(ReflectionUtils.streamAllResourcesInClasspathRoot(root, allResources)) //
						.containsExactlyElementsOf(
							ReflectionSupport.streamAllResourcesInClasspathRoot(root, allResources).toList())));
		}
		return tests;
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamAllResourcesInClasspathRootPreconditions() {
		var path = Path.of(".").toUri();
		assertPreconditionViolationException("root",
			() -> ReflectionSupport.streamAllResourcesInClasspathRoot(null, allResources));
		assertPreconditionViolationException("resourceFilter",
			() -> ReflectionSupport.streamAllResourcesInClasspathRoot(path, null));
	}

	@Test
	void findAllClassesInPackageDelegates() {
		assertNotEquals(0, ReflectionSupport.findAllClassesInPackage("org.junit", allTypes, allNames).size());
		assertEquals(ReflectionUtils.findAllClassesInPackage("org.junit", allTypes, allNames),
			ReflectionSupport.findAllClassesInPackage("org.junit", allTypes, allNames));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllClassesInPackagePreconditions() {
		assertPreconditionViolationExceptionForString("basePackageName",
			() -> ReflectionSupport.findAllClassesInPackage(null, allTypes, allNames));
		assertPreconditionViolationException("class predicate",
			() -> ReflectionSupport.findAllClassesInPackage("org.junit", null, allNames));
		assertPreconditionViolationException("name predicate",
			() -> ReflectionSupport.findAllClassesInPackage("org.junit", allTypes, null));
	}

	/**
	 * @since 1.11
	 */
	@Test
	void findAllResourcesInPackageDelegates() {
		assertNotEquals(0, ReflectionSupport.findAllResourcesInPackage("org.junit", allResources).size());

		assertEquals(ReflectionUtils.findAllResourcesInPackage("org.junit", allResources),
			ReflectionSupport.findAllResourcesInPackage("org.junit", allResources));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllResourcesInPackagePreconditions() {
		assertPreconditionViolationExceptionForString("basePackageName",
			() -> ReflectionSupport.findAllResourcesInPackage(null, allResources));
		assertPreconditionViolationException("resourceFilter",
			() -> ReflectionSupport.findAllResourcesInPackage("org.junit", null));
	}

	/**
	 * @since 1.11
	 */
	@Test
	void streamAllResourcesInPackageDelegates() {
		assertNotEquals(0, ReflectionSupport.streamAllResourcesInPackage("org.junit", allResources).count());

		assertEquals(ReflectionUtils.streamAllResourcesInPackage("org.junit", allResources).toList(),
			ReflectionSupport.streamAllResourcesInPackage("org.junit", allResources).toList());
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamAllResourcesInPackagePreconditions() {
		assertPreconditionViolationExceptionForString("basePackageName",
			() -> ReflectionSupport.streamAllResourcesInPackage(null, allResources));
		assertPreconditionViolationException("resourceFilter",
			() -> ReflectionSupport.streamAllResourcesInPackage("org.junit", null));
	}

	@Test
	void findAllClassesInModuleDelegates() {
		assertEquals(ReflectionUtils.findAllClassesInModule("org.junit.platform.commons", allTypes, allNames),
			ReflectionSupport.findAllClassesInModule("org.junit.platform.commons", allTypes, allNames));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllClassesInModulePreconditions() {
		var exception = assertThrows(PreconditionViolationException.class,
			() -> ReflectionSupport.findAllClassesInModule(null, allTypes, allNames));
		assertEquals("Module name must not be null or empty", exception.getMessage());
		assertPreconditionViolationException("class predicate",
			() -> ReflectionSupport.findAllClassesInModule("org.junit.platform.commons", null, allNames));
		assertPreconditionViolationException("name predicate",
			() -> ReflectionSupport.findAllClassesInModule("org.junit.platform.commons", allTypes, null));
	}

	/**
	 * @since 1.11
	 */
	@Test
	void findAllResourcesInModuleDelegates() {
		assertEquals(ReflectionUtils.findAllResourcesInModule("org.junit.platform.commons", allResources),
			ReflectionSupport.findAllResourcesInModule("org.junit.platform.commons", allResources));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllResourcesInModulePreconditions() {
		var exception = assertThrows(PreconditionViolationException.class,
			() -> ReflectionSupport.findAllResourcesInModule(null, allResources));
		assertEquals("Module name must not be null or empty", exception.getMessage());
		assertPreconditionViolationException("Resource filter",
			() -> ReflectionSupport.findAllResourcesInModule("org.junit.platform.commons", null));
	}

	/**
	 * @since 1.11
	 */
	@Test
	void streamAllResourcesInModuleDelegates() {
		assertEquals(ReflectionUtils.streamAllResourcesInModule("org.junit.platform.commons", allResources).toList(),
			ReflectionSupport.streamAllResourcesInModule("org.junit.platform.commons", allResources).toList());
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamAllResourcesInModulePreconditions() {
		var exception = assertThrows(PreconditionViolationException.class,
			() -> ReflectionSupport.streamAllResourcesInModule(null, allResources));
		assertEquals("Module name must not be null or empty", exception.getMessage());
		assertPreconditionViolationException("Resource filter",
			() -> ReflectionSupport.streamAllResourcesInModule("org.junit.platform.commons", null));
	}

	@Test
	void newInstanceDelegates() {
		assertEquals(ReflectionUtils.newInstance(String.class, "foo"),
			ReflectionSupport.newInstance(String.class, "foo"));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void newInstancePreconditions() {
		assertPreconditionViolationException("Class", () -> ReflectionSupport.newInstance(null));
		assertPreconditionViolationException("Argument array",
			() -> ReflectionSupport.newInstance(String.class, (Object[]) null));
		assertPreconditionViolationException("Individual arguments",
			() -> ReflectionSupport.newInstance(String.class, new Object[] { null }));
	}

	@Test
	void invokeMethodDelegates() throws Exception {
		var method = Boolean.class.getMethod("valueOf", String.class);
		assertEquals(ReflectionUtils.invokeMethod(method, null, "true"),
			ReflectionSupport.invokeMethod(method, null, "true"));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void invokeMethodPreconditions() throws Exception {
		assertPreconditionViolationException("Method", () -> ReflectionSupport.invokeMethod(null, null, "true"));

		var method = Boolean.class.getMethod("toString");
		var exception = assertThrows(PreconditionViolationException.class,
			() -> ReflectionSupport.invokeMethod(method, null));
		assertEquals("Cannot invoke non-static method [" + method.toGenericString() + "] on a null target.",
			exception.getMessage());
	}

	@Test
	void findFieldsDelegates() {
		assertEquals(
			ReflectionUtils.findFields(ReflectionSupportTests.class, allFields,
				ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP),
			ReflectionSupport.findFields(ReflectionSupportTests.class, allFields, HierarchyTraversalMode.BOTTOM_UP));
		assertEquals(
			ReflectionUtils.findFields(ReflectionSupportTests.class, allFields,
				ReflectionUtils.HierarchyTraversalMode.TOP_DOWN),
			ReflectionSupport.findFields(ReflectionSupportTests.class, allFields, HierarchyTraversalMode.TOP_DOWN));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findFieldsPreconditions() {
		assertPreconditionViolationException("Class",
			() -> ReflectionSupport.findFields(null, allFields, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationException("Class",
			() -> ReflectionSupport.findFields(null, allFields, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationException("Predicate",
			() -> ReflectionSupport.findFields(ReflectionSupportTests.class, null, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationException("Predicate",
			() -> ReflectionSupport.findFields(ReflectionSupportTests.class, null, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationException("HierarchyTraversalMode",
			() -> ReflectionSupport.findFields(ReflectionSupportTests.class, allFields, null));
	}

	@Test
	void tryToReadFieldValueDelegates() throws Exception {
		var staticField = getClass().getDeclaredField("staticField");
		assertEquals(ReflectionUtils.tryToReadFieldValue(staticField, null),
			ReflectionSupport.tryToReadFieldValue(staticField, null));

		var instanceField = getClass().getDeclaredField("instanceField");
		assertEquals(ReflectionUtils.tryToReadFieldValue(instanceField, this),
			ReflectionSupport.tryToReadFieldValue(instanceField, this));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void tryToReadFieldValuePreconditions() throws Exception {
		assertPreconditionViolationException("Field", () -> ReflectionSupport.tryToReadFieldValue(null, this));

		var instanceField = getClass().getDeclaredField("instanceField");
		Exception exception = assertThrows(PreconditionViolationException.class,
			() -> ReflectionSupport.tryToReadFieldValue(instanceField, null));
		assertThat(exception)//
				.hasMessageStartingWith("Cannot read non-static field")//
				.hasMessageEndingWith("on a null instance.");
	}

	@Test
	void findMethodDelegates() {
		assertEquals(ReflectionUtils.findMethod(Boolean.class, "valueOf", String.class.getName()),
			ReflectionSupport.findMethod(Boolean.class, "valueOf", String.class.getName()));

		assertEquals(ReflectionUtils.findMethod(Boolean.class, "valueOf", String.class),
			ReflectionSupport.findMethod(Boolean.class, "valueOf", String.class));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findMethodPreconditions() {
		assertPreconditionViolationException("Class",
			() -> ReflectionSupport.findMethod(null, "valueOf", String.class.getName()));
		assertPreconditionViolationExceptionForString("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "", String.class.getName()));
		assertPreconditionViolationExceptionForString("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "   ", String.class.getName()));

		assertPreconditionViolationException("Class",
			() -> ReflectionSupport.findMethod(null, "valueOf", String.class));
		assertPreconditionViolationExceptionForString("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "", String.class));
		assertPreconditionViolationExceptionForString("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "   ", String.class));
		assertPreconditionViolationException("Parameter types array",
			() -> ReflectionSupport.findMethod(Boolean.class, "valueOf", (Class<?>[]) null));
		assertPreconditionViolationException("Individual parameter types",
			() -> ReflectionSupport.findMethod(Boolean.class, "valueOf", new Class<?>[] { null }));
	}

	@Test
	void findMethodsDelegates() {
		assertEquals(
			ReflectionUtils.findMethods(ReflectionSupportTests.class, allMethods,
				ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP),
			ReflectionSupport.findMethods(ReflectionSupportTests.class, allMethods, HierarchyTraversalMode.BOTTOM_UP));
		assertEquals(
			ReflectionUtils.findMethods(ReflectionSupportTests.class, allMethods,
				ReflectionUtils.HierarchyTraversalMode.TOP_DOWN),
			ReflectionSupport.findMethods(ReflectionSupportTests.class, allMethods, HierarchyTraversalMode.TOP_DOWN));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findMethodsPreconditions() {
		assertPreconditionViolationException("Class",
			() -> ReflectionSupport.findMethods(null, allMethods, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationException("Class",
			() -> ReflectionSupport.findMethods(null, allMethods, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationException("Predicate",
			() -> ReflectionSupport.findMethods(ReflectionSupportTests.class, null, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationException("Predicate",
			() -> ReflectionSupport.findMethods(ReflectionSupportTests.class, null, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationException("HierarchyTraversalMode",
			() -> ReflectionSupport.findMethods(ReflectionSupportTests.class, allMethods, null));
	}

	@Test
	void findNestedClassesDelegates() {
		assertEquals(ReflectionUtils.findNestedClasses(ClassWithNestedClasses.class, ReflectionUtils::isStatic),
			ReflectionSupport.findNestedClasses(ClassWithNestedClasses.class, ReflectionUtils::isStatic));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findNestedClassesPreconditions() {
		assertPreconditionViolationException("Class",
			() -> ReflectionSupport.findNestedClasses(null, ReflectionUtils::isStatic));
		assertPreconditionViolationException("Predicate",
			() -> ReflectionSupport.findNestedClasses(ClassWithNestedClasses.class, null));
	}

	private static String createDisplayName(URI root) {
		var displayName = root.getPath();
		if (displayName.length() > 42) {
			displayName = "..." + displayName.substring(displayName.length() - 42);
		}
		return displayName;
	}

	static class ClassWithNestedClasses {

		@SuppressWarnings({ "InnerClassMayBeStatic", "unused" })
		class Nested1 {
		}

		@SuppressWarnings("unused")
		static class Nested2 {
		}

	}

}
