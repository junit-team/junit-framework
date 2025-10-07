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
import static org.junit.platform.commons.support.ReflectionSupport.toSupportResourcesList;
import static org.junit.platform.commons.support.ReflectionSupport.toSupportResourcesStream;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrBlankFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrEmptyFor;
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
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 1.0
 */
class ReflectionSupportTests {

	private static final Predicate<Class<?>> allTypes = type -> true;
	@SuppressWarnings("removal")
	private static final Predicate<Resource> allResources = __ -> true;
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
		assertPreconditionViolationNotNullOrBlankFor("Class name", () -> ReflectionSupport.tryToLoadClass(null));
		assertPreconditionViolationNotNullOrBlankFor("Class name", () -> ReflectionSupport.tryToLoadClass(""));
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

		assertPreconditionViolationNotNullOrBlankFor("Class name", () -> ReflectionSupport.tryToLoadClass(null, cl));
		assertPreconditionViolationNotNullOrBlankFor("Class name", () -> ReflectionSupport.tryToLoadClass("", cl));

		assertPreconditionViolationNotNullFor("ClassLoader", () -> ReflectionSupport.tryToLoadClass("int", null));
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
	@SuppressWarnings({ "DataFlowIssue", "removal" })
	@Test
	void tryToGetResourcesPreconditions() {
		assertPreconditionViolationNotNullOrBlankFor("Resource name", () -> ReflectionSupport.tryToGetResources(null));
		assertPreconditionViolationNotNullOrBlankFor("Resource name", () -> ReflectionSupport.tryToGetResources(""));
		assertPreconditionViolationNotNullFor("Class loader",
			() -> ReflectionSupport.tryToGetResources("default-package.resource", null));
		assertPreconditionViolationNotNullFor("Class loader",
			() -> ReflectionSupport.tryToGetResources("default-package.resource", null));
	}

	/**
	 * @since 1.12
	 */
	@SuppressWarnings("removal")
	@Test
	void tryToGetResources() {
		assertEquals(
			ReflectionUtils.tryToGetResources("default-package.resource").toOptional().map(
				ReflectionSupport::toSupportResourcesSet),
			ReflectionSupport.tryToGetResources("default-package.resource").toOptional());
		assertEquals(
			ReflectionUtils.tryToGetResources("default-package.resource", getDefaultClassLoader()).toOptional().map(
				ReflectionSupport::toSupportResourcesSet), //
			ReflectionSupport.tryToGetResources("default-package.resource", getDefaultClassLoader()).toOptional());
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAllClassesInClasspathRootPreconditions() {
		var path = Path.of(".").toUri();
		assertPreconditionViolationNotNullFor("root",
			() -> ReflectionSupport.findAllClassesInClasspathRoot(null, allTypes, allNames));
		assertPreconditionViolationNotNullFor("class predicate",
			() -> ReflectionSupport.findAllClassesInClasspathRoot(path, null, allNames));
		assertPreconditionViolationNotNullFor("name predicate",
			() -> ReflectionSupport.findAllClassesInClasspathRoot(path, allTypes, null));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("removal")
	@TestFactory
	List<DynamicTest> findAllResourcesInClasspathRootDelegates() throws Throwable {
		List<DynamicTest> tests = new ArrayList<>();
		List<Path> paths = new ArrayList<>();
		paths.add(Path.of(".").toRealPath());
		paths.addAll(ReflectionUtils.getAllClasspathRootDirectories());
		for (var path : paths) {
			var root = path.toUri();
			tests.add(DynamicTest.dynamicTest(createDisplayName(root), () -> assertThat(toSupportResourcesList(
				ReflectionUtils.findAllResourcesInClasspathRoot(root, ResourceFilter.of(__ -> true)))) //
						.containsExactlyElementsOf(
							ReflectionSupport.findAllResourcesInClasspathRoot(root, allResources))));
		}
		return tests;
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings({ "DataFlowIssue", "removal" })
	@Test
	void findAllResourcesInClasspathRootPreconditions() {
		var path = Path.of(".").toUri();
		assertPreconditionViolationNotNullFor("root",
			() -> ReflectionSupport.findAllResourcesInClasspathRoot(null, allResources));
		assertPreconditionViolationNotNullFor("resourceFilter",
			() -> ReflectionSupport.findAllResourcesInClasspathRoot(path, null));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("removal")
	@TestFactory
	List<DynamicTest> streamAllResourcesInClasspathRootDelegates() throws Throwable {
		List<DynamicTest> tests = new ArrayList<>();
		List<Path> paths = new ArrayList<>();
		paths.add(Path.of(".").toRealPath());
		paths.addAll(ReflectionUtils.getAllClasspathRootDirectories());
		for (var path : paths) {
			var root = path.toUri();
			tests.add(DynamicTest.dynamicTest(createDisplayName(root), () -> assertThat(toSupportResourcesStream(
				ReflectionUtils.streamAllResourcesInClasspathRoot(root, ResourceFilter.of(__ -> true)))) //
						.containsExactlyElementsOf(
							ReflectionSupport.streamAllResourcesInClasspathRoot(root, allResources).toList())));
		}
		return tests;
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings({ "DataFlowIssue", "removal" })
	@Test
	void streamAllResourcesInClasspathRootPreconditions() {
		var path = Path.of(".").toUri();
		assertPreconditionViolationNotNullFor("root",
			() -> ReflectionSupport.streamAllResourcesInClasspathRoot(null, allResources));
		assertPreconditionViolationNotNullFor("resourceFilter",
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
		assertPreconditionViolationNotNullOrBlankFor("basePackageName",
			() -> ReflectionSupport.findAllClassesInPackage(null, allTypes, allNames));
		assertPreconditionViolationNotNullFor("class predicate",
			() -> ReflectionSupport.findAllClassesInPackage("org.junit", null, allNames));
		assertPreconditionViolationNotNullFor("name predicate",
			() -> ReflectionSupport.findAllClassesInPackage("org.junit", allTypes, null));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("removal")
	@Test
	void findAllResourcesInPackageDelegates() {
		assertNotEquals(0, ReflectionSupport.findAllResourcesInPackage("org.junit", allResources).size());

		assertEquals(
			toSupportResourcesList(
				ReflectionUtils.findAllResourcesInPackage("org.junit", ResourceFilter.of(__ -> true))),
			ReflectionSupport.findAllResourcesInPackage("org.junit", allResources));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings({ "DataFlowIssue", "removal" })
	@Test
	void findAllResourcesInPackagePreconditions() {
		assertPreconditionViolationNotNullOrBlankFor("basePackageName",
			() -> ReflectionSupport.findAllResourcesInPackage(null, allResources));
		assertPreconditionViolationNotNullFor("resourceFilter",
			() -> ReflectionSupport.findAllResourcesInPackage("org.junit", null));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("removal")
	@Test
	void streamAllResourcesInPackageDelegates() {
		assertNotEquals(0, ReflectionSupport.streamAllResourcesInPackage("org.junit", allResources).count());

		assertEquals(
			toSupportResourcesStream(
				ReflectionUtils.streamAllResourcesInPackage("org.junit", ResourceFilter.of(__ -> true))).toList(),
			ReflectionSupport.streamAllResourcesInPackage("org.junit", allResources).toList());
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings({ "DataFlowIssue", "removal" })
	@Test
	void streamAllResourcesInPackagePreconditions() {
		assertPreconditionViolationNotNullOrBlankFor("basePackageName",
			() -> ReflectionSupport.streamAllResourcesInPackage(null, allResources));
		assertPreconditionViolationNotNullFor("resourceFilter",
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
		assertPreconditionViolationNotNullOrEmptyFor("Module name",
			() -> ReflectionSupport.findAllClassesInModule(null, allTypes, allNames));
		assertPreconditionViolationNotNullFor("class predicate",
			() -> ReflectionSupport.findAllClassesInModule("org.junit.platform.commons", null, allNames));
		assertPreconditionViolationNotNullFor("name predicate",
			() -> ReflectionSupport.findAllClassesInModule("org.junit.platform.commons", allTypes, null));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("removal")
	@Test
	void findAllResourcesInModuleDelegates() {
		assertEquals(
			ReflectionUtils.findAllResourcesInModule("org.junit.platform.commons", ResourceFilter.of(__ -> true)),
			ReflectionSupport.findAllResourcesInModule("org.junit.platform.commons", allResources));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings({ "DataFlowIssue", "removal" })
	@Test
	void findAllResourcesInModulePreconditions() {
		assertPreconditionViolationNotNullOrEmptyFor("Module name",
			() -> ReflectionSupport.findAllResourcesInModule(null, allResources));
		assertPreconditionViolationNotNullFor("resourceFilter",
			() -> ReflectionSupport.findAllResourcesInModule("org.junit.platform.commons", null));
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings("removal")
	@Test
	void streamAllResourcesInModuleDelegates() {
		assertEquals(
			toSupportResourcesStream(ReflectionUtils.streamAllResourcesInModule("org.junit.platform.commons",
				ResourceFilter.of(__ -> true))).toList(),
			ReflectionSupport.streamAllResourcesInModule("org.junit.platform.commons", allResources).toList());
	}

	/**
	 * @since 1.11
	 */
	@SuppressWarnings({ "DataFlowIssue", "removal" })
	@Test
	void streamAllResourcesInModulePreconditions() {
		assertPreconditionViolationNotNullOrEmptyFor("Module name",
			() -> ReflectionSupport.streamAllResourcesInModule(null, allResources));
		assertPreconditionViolationNotNullFor("resourceFilter",
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
		assertPreconditionViolationNotNullFor("Class", () -> ReflectionSupport.newInstance(null));
		assertPreconditionViolationNotNullFor("Argument array",
			() -> ReflectionSupport.newInstance(String.class, (Object[]) null));
		assertPreconditionViolationNotNullFor("Individual arguments",
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
		assertPreconditionViolationNotNullFor("Method", () -> ReflectionSupport.invokeMethod(null, null, "true"));

		var method = Boolean.class.getMethod("toString");
		assertPreconditionViolationFor(() -> ReflectionSupport.invokeMethod(method, null))//
				.withMessage("Cannot invoke non-static method [" + method.toGenericString() + "] on a null target.");
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
		assertPreconditionViolationNotNullFor("Class",
			() -> ReflectionSupport.findFields(null, allFields, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationNotNullFor("Class",
			() -> ReflectionSupport.findFields(null, allFields, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationNotNullFor("Predicate",
			() -> ReflectionSupport.findFields(ReflectionSupportTests.class, null, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationNotNullFor("Predicate",
			() -> ReflectionSupport.findFields(ReflectionSupportTests.class, null, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationNotNullFor("HierarchyTraversalMode",
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
		assertPreconditionViolationNotNullFor("Field", () -> ReflectionSupport.tryToReadFieldValue(null, this));

		var instanceField = getClass().getDeclaredField("instanceField");
		assertPreconditionViolationFor(() -> ReflectionSupport.tryToReadFieldValue(instanceField, null))//
				.withMessageStartingWith("Cannot read non-static field")//
				.withMessageEndingWith("on a null instance.");
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
		assertPreconditionViolationNotNullFor("Class",
			() -> ReflectionSupport.findMethod(null, "valueOf", String.class.getName()));
		assertPreconditionViolationNotNullOrBlankFor("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "", String.class.getName()));
		assertPreconditionViolationNotNullOrBlankFor("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "   ", String.class.getName()));

		assertPreconditionViolationNotNullFor("Class",
			() -> ReflectionSupport.findMethod(null, "valueOf", String.class));
		assertPreconditionViolationNotNullOrBlankFor("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "", String.class));
		assertPreconditionViolationNotNullOrBlankFor("Method name",
			() -> ReflectionSupport.findMethod(Boolean.class, "   ", String.class));
		assertPreconditionViolationNotNullFor("Parameter types array",
			() -> ReflectionSupport.findMethod(Boolean.class, "valueOf", (Class<?>[]) null));
		assertPreconditionViolationNotNullFor("Individual parameter types",
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
		assertPreconditionViolationNotNullFor("Class",
			() -> ReflectionSupport.findMethods(null, allMethods, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationNotNullFor("Class",
			() -> ReflectionSupport.findMethods(null, allMethods, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationNotNullFor("Predicate",
			() -> ReflectionSupport.findMethods(ReflectionSupportTests.class, null, HierarchyTraversalMode.BOTTOM_UP));
		assertPreconditionViolationNotNullFor("Predicate",
			() -> ReflectionSupport.findMethods(ReflectionSupportTests.class, null, HierarchyTraversalMode.TOP_DOWN));
		assertPreconditionViolationNotNullFor("HierarchyTraversalMode",
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
		assertPreconditionViolationNotNullFor("Class",
			() -> ReflectionSupport.findNestedClasses(null, ReflectionUtils::isStatic));
		assertPreconditionViolationNotNullFor("Predicate",
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
