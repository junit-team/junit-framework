/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.engine.discovery.JupiterUniqueIdBuilder.uniqueIdForTestTemplateMethod;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.platform.commons.test.IdeUtils.runningInEclipse;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.DisabledInEclipse;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * Test correct test discovery in simple test classes for the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class DiscoveryTests extends AbstractJupiterTestEngineTests {

	@Test
	void discoverTestClass() {
		LauncherDiscoveryRequest request = defaultRequest().selectors(selectClass(LocalTestCase.class)).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(7, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void doNotDiscoverAbstractTestClass() {
		LauncherDiscoveryRequest request = defaultRequest().selectors(selectClass(AbstractTestCase.class)).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(0, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@ParameterizedTest
	@ValueSource(strings = { "org.junit.jupiter.engine.discovery.DiscoveryTests$InterfaceTestCase",
			"org.junit.jupiter.engine.kotlin.KotlinInterfaceTestCase" })
	void doNotDiscoverTestInterface(String className) {

		assumeFalse(runningInEclipse() && className.contains(".kotlin."));

		LauncherDiscoveryRequest request = defaultRequest().selectors(selectClass(className)).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(0, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	@DisabledInEclipse
	void doNotDiscoverGeneratedKotlinDefaultImplsClass() {
		LauncherDiscoveryRequest request = defaultRequest() //
				.selectors(selectClass("org.junit.jupiter.engine.kotlin.KotlinInterfaceTestCase$DefaultImpls")) //
				.build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(0, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	@DisabledInEclipse
	void discoverDeclaredKotlinDefaultImplsClass() {
		LauncherDiscoveryRequest request = defaultRequest().selectors(
			selectClass("org.junit.jupiter.engine.kotlin.KotlinDefaultImplsTestCase$DefaultImpls")).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"org.junit.jupiter.engine.discovery.DiscoveryTests$ConcreteImplementationOfInterfaceTestCase",
			"org.junit.jupiter.engine.kotlin.KotlinInterfaceImplementationTestCase" })
	void discoverTestClassInheritingTestsFromInterface(String className) {

		assumeFalse(runningInEclipse() && className.contains(".kotlin."));

		LauncherDiscoveryRequest request = defaultRequest().selectors(selectClass(className)).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverMethodByUniqueId() {
		LauncherDiscoveryRequest request = defaultRequest().selectors(
			selectUniqueId(JupiterUniqueIdBuilder.uniqueIdForMethod(LocalTestCase.class, "test1()"))).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverMethodByUniqueIdForOverloadedMethod() {
		LauncherDiscoveryRequest request = defaultRequest().selectors(
			selectUniqueId(JupiterUniqueIdBuilder.uniqueIdForMethod(LocalTestCase.class, "test4()"))).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverMethodByUniqueIdForOverloadedMethodVariantThatAcceptsArguments() {
		LauncherDiscoveryRequest request = defaultRequest().selectors(
			selectUniqueId(JupiterUniqueIdBuilder.uniqueIdForMethod(LocalTestCase.class,
				"test4(" + TestInfo.class.getName() + ")"))).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverMethodByMethodReference() throws NoSuchMethodException {
		Method testMethod = LocalTestCase.class.getDeclaredMethod("test3");

		LauncherDiscoveryRequest request = defaultRequest().selectors(
			selectMethod(LocalTestCase.class, testMethod)).build();
		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverMultipleMethodsOfSameClass() {
		LauncherDiscoveryRequest request = defaultRequest().selectors(selectMethod(LocalTestCase.class, "test1"),
			selectMethod(LocalTestCase.class, "test2")).build();

		TestDescriptor engineDescriptor = discoverTestsWithoutIssues(request);

		assertThat(engineDescriptor.getChildren()).hasSize(1);
		TestDescriptor classDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(classDescriptor.getChildren()).hasSize(2);
	}

	@Test
	void discoverCompositeSpec() {
		LauncherDiscoveryRequest spec = defaultRequest().selectors(
			selectUniqueId(JupiterUniqueIdBuilder.uniqueIdForMethod(LocalTestCase.class, "test2()")),
			selectClass(LocalTestCase.class)).build();

		TestDescriptor engineDescriptor = discoverTests(spec).getEngineDescriptor();
		assertEquals(7, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverTestTemplateMethodByUniqueId() {
		LauncherDiscoveryRequest spec = defaultRequest().selectors(
			selectUniqueId(uniqueIdForTestTemplateMethod(TestTemplateClass.class, "testTemplate()"))).build();

		TestDescriptor engineDescriptor = discoverTests(spec).getEngineDescriptor();
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverTestTemplateMethodByMethodSelector() {
		LauncherDiscoveryRequest spec = defaultRequest().selectors(
			selectMethod(TestTemplateClass.class, "testTemplate")).build();

		TestDescriptor engineDescriptor = discoverTests(spec).getEngineDescriptor();
		assertEquals(2, engineDescriptor.getDescendants().size(), "# resolved test descriptors");
	}

	@Test
	void discoverDeeplyNestedTestMethodByNestedMethodSelector() throws Exception {
		var selector = selectNestedMethod(
			List.of(TestCaseWithExtendedNested.class, TestCaseWithExtendedNested.ConcreteInner1.class),
			AbstractSuperClass.NestedInAbstractClass.class,
			AbstractSuperClass.NestedInAbstractClass.class.getDeclaredMethod("test"));
		LauncherDiscoveryRequest spec = defaultRequest().selectors(selector).build();

		TestDescriptor engineDescriptor = discoverTests(spec).getEngineDescriptor();

		ClassTestDescriptor topLevelClassDescriptor = (ClassTestDescriptor) getOnlyElement(
			engineDescriptor.getChildren());
		assertThat(topLevelClassDescriptor.getTestClass()).isEqualTo(TestCaseWithExtendedNested.class);

		NestedClassTestDescriptor firstLevelNestedClassDescriptor = (NestedClassTestDescriptor) getOnlyElement(
			topLevelClassDescriptor.getChildren());
		assertThat(firstLevelNestedClassDescriptor.getTestClass()).isEqualTo(
			TestCaseWithExtendedNested.ConcreteInner1.class);

		NestedClassTestDescriptor secondLevelNestedClassDescriptor = (NestedClassTestDescriptor) getOnlyElement(
			firstLevelNestedClassDescriptor.getChildren());
		assertThat(secondLevelNestedClassDescriptor.getTestClass()).isEqualTo(
			AbstractSuperClass.NestedInAbstractClass.class);

		TestMethodTestDescriptor methodDescriptor = (TestMethodTestDescriptor) getOnlyElement(
			secondLevelNestedClassDescriptor.getChildren());
		assertThat(methodDescriptor.getTestMethod().getName()).isEqualTo("test");
	}

	@ParameterizedTest
	@MethodSource("requestsForTestClassWithInvalidTestMethod")
	void reportsWarningForTestClassWithInvalidTestMethod(LauncherDiscoveryRequest request) throws Exception {

		var method = InvalidTestCases.InvalidTestMethodTestCase.class.getDeclaredMethod("test");

		var results = discoverTests(request);

		var discoveryIssues = results.getDiscoveryIssues().stream().sorted(comparing(DiscoveryIssue::message)).toList();
		assertThat(discoveryIssues).hasSize(3);
		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo("@Test method '%s' must not be private. It will not be executed.", method.toGenericString());
		assertThat(discoveryIssues.get(1).message()) //
				.isEqualTo("@Test method '%s' must not be static. It will not be executed.", method.toGenericString());
		assertThat(discoveryIssues.getLast().message()) //
				.isEqualTo("@Test method '%s' must not return a value. It will not be executed.",
					method.toGenericString());
	}

	static List<Named<LauncherDiscoveryRequest>> requestsForTestClassWithInvalidTestMethod() {
		return List.of( //
			named("directly selected",
				defaultRequest().selectors(selectClass(InvalidTestCases.InvalidTestMethodTestCase.class)).build()), //
			named("indirectly selected", defaultRequest() //
					.selectors(selectPackage(InvalidTestCases.InvalidTestMethodTestCase.class.getPackageName())) //
					.filters(includeClassNamePatterns(
						Pattern.quote(InvalidTestCases.InvalidTestMethodTestCase.class.getName()))).build()), //
			named("subclasses", defaultRequest() //
					.selectors(selectClasses(InvalidTestCases.InvalidTestMethodSubclass1TestCase.class,
						InvalidTestCases.InvalidTestMethodSubclass2TestCase.class)) //
					.build()) //
		);
	}

	@ParameterizedTest
	@MethodSource("requestsForTestClassWithInvalidStandaloneTestClass")
	void reportsWarningForInvalidStandaloneTestClass(LauncherDiscoveryRequest request, Class<?> testClass) {

		var results = discoverTests(request);

		var discoveryIssues = results.getDiscoveryIssues().stream().sorted(comparing(DiscoveryIssue::message)).toList();
		assertThat(discoveryIssues).hasSize(2);
		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo(
					"Test class '%s' must not be an inner class unless annotated with @Nested. It will not be executed.",
					testClass.getName());
		assertThat(discoveryIssues.getLast().message()) //
				.isEqualTo("Test class '%s' must not be private. It will not be executed.", testClass.getName());
	}

	static List<Arguments> requestsForTestClassWithInvalidStandaloneTestClass() {
		return List.of( //
			argumentSet("directly selected",
				defaultRequest().selectors(selectClass(InvalidTestCases.InvalidTestClassTestCase.class)).build(),
				InvalidTestCases.InvalidTestClassTestCase.class), //
			argumentSet("indirectly selected", defaultRequest() //
					.selectors(selectPackage(InvalidTestCases.InvalidTestClassTestCase.class.getPackageName())) //
					.filters(includeClassNamePatterns(
						Pattern.quote(InvalidTestCases.InvalidTestClassTestCase.class.getName()))).build(), //
				InvalidTestCases.InvalidTestClassTestCase.class), //
			argumentSet("subclass", defaultRequest() //
					.selectors(selectClass(InvalidTestCases.InvalidTestClassSubclassTestCase.class)) //
					.build(), //
				InvalidTestCases.InvalidTestClassSubclassTestCase.class) //
		);
	}

	@ParameterizedTest
	@MethodSource("requestsForTestClassWithInvalidNestedTestClass")
	void reportsWarningForInvalidNestedTestClass(LauncherDiscoveryRequest request) {

		var results = discoverTests(request);

		var discoveryIssues = results.getDiscoveryIssues().stream().sorted(comparing(DiscoveryIssue::message)).toList();
		assertThat(discoveryIssues).hasSize(2);
		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo("@Nested class '%s' must not be private. It will not be executed.",
					InvalidTestCases.InvalidTestClassTestCase.Inner.class.getName());
		assertThat(discoveryIssues.getLast().message()) //
				.startsWith("@Nested class '%s' must not be static.".formatted(
					InvalidTestCases.InvalidTestClassTestCase.Inner.class.getName()));
	}

	static List<Named<LauncherDiscoveryRequest>> requestsForTestClassWithInvalidNestedTestClass() {
		return List.of( //
			named("directly selected",
				defaultRequest().selectors(selectClass(InvalidTestCases.InvalidTestClassTestCase.Inner.class)).build()), //
			named("subclass", defaultRequest() //
					.selectors(selectNestedClass(List.of(InvalidTestCases.InvalidTestClassSubclassTestCase.class),
						InvalidTestCases.InvalidTestClassTestCase.Inner.class)) //
					.build()) //
		);
	}

	@Test
	void reportsWarningForTestClassWithPotentialNestedTestClasses() {

		var results = discoverTestsForClass(InvalidTestCases.class);

		var discoveryIssues = results.getDiscoveryIssues().stream().sorted(comparing(DiscoveryIssue::message)).toList();
		assertThat(discoveryIssues).hasSize(2);
		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo(
					"Inner class '%s' looks like it was intended to be a test class but will not be executed. It must be static or annotated with @Nested.",
					InvalidTestCases.InvalidTestClassSubclassTestCase.class.getName());
		assertThat(discoveryIssues.getLast().message()) //
				.isEqualTo(
					"Inner class '%s' looks like it was intended to be a test class but will not be executed. It must be static or annotated with @Nested.",
					InvalidTestCases.InvalidTestClassTestCase.class.getName());
	}

	@Test
	void ignoresUnrelatedClassDefinitionCycles() {
		var results = discoverTestsForClass(UnrelatedRecursiveHierarchyTestCase.class);

		assertThat(results.getDiscoveryIssues()).isEmpty();
	}

	@Test
	void ignoresRecursiveNonTestHierarchyCycles() {
		var results = discoverTestsForClass(NonTestRecursiveHierarchyTestCase.class);

		assertThat(results.getDiscoveryIssues()).isEmpty();
	}

	@Test
	void reportsMissingNestedAnnotationOnRecursiveHierarchy() {
		var results = discoverTestsForClass(RecursiveHierarchyWithoutNestedTestCase.class);

		var discoveryIssues = results.getDiscoveryIssues();
		assertThat(discoveryIssues).hasSize(1);
		assertThat(discoveryIssues.getFirst().severity()) //
				.isEqualTo(Severity.WARNING);
		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo(
					"Inner class '%s' looks like it was intended to be a test class but will not be executed. It must be static or annotated with @Nested.",
					RecursiveHierarchyWithoutNestedTestCase.Inner.class.getName());
	}

	@Test
	void reportsWarningsForInvalidTags() throws Exception {

		var results = discoverTestsForClass(InvalidTagsTestCase.class);

		var discoveryIssues = results.getDiscoveryIssues().stream().sorted(comparing(DiscoveryIssue::message)).toList();
		assertThat(discoveryIssues).hasSize(2);

		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo("Invalid tag syntax in @Tag(\"\") declaration on class '%s'. Tag will be ignored.",
					InvalidTagsTestCase.class.getName());
		assertThat(discoveryIssues.getFirst().source()) //
				.contains(ClassSource.from(InvalidTagsTestCase.class));

		var method = InvalidTagsTestCase.class.getDeclaredMethod("test");
		assertThat(discoveryIssues.getLast().message()) //
				.isEqualTo("Invalid tag syntax in @Tag(\"|\") declaration on method '%s'. Tag will be ignored.",
					method.toGenericString());
		assertThat(discoveryIssues.getLast().source()) //
				.contains(org.junit.platform.engine.support.descriptor.MethodSource.from(method));
	}

	@Test
	void reportsWarningsForBlankDisplayNames() throws Exception {

		var results = discoverTestsForClass(BlankDisplayNamesTestCase.class);

		var discoveryIssues = results.getDiscoveryIssues().stream().sorted(comparing(DiscoveryIssue::message)).toList();
		assertThat(discoveryIssues).hasSize(2);

		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo("@DisplayName on class '%s' must be declared with a non-blank value.",
					BlankDisplayNamesTestCase.class.getName());
		assertThat(discoveryIssues.getFirst().source()) //
				.contains(ClassSource.from(BlankDisplayNamesTestCase.class));

		var method = BlankDisplayNamesTestCase.class.getDeclaredMethod("test");
		assertThat(discoveryIssues.getLast().message()) //
				.isEqualTo("@DisplayName on method '%s' must be declared with a non-blank value.",
					method.toGenericString());
		assertThat(discoveryIssues.getLast().source()) //
				.contains(org.junit.platform.engine.support.descriptor.MethodSource.from(method));
	}

	// -------------------------------------------------------------------

	@SuppressWarnings("unused")
	static abstract class AbstractTestCase {

		@Test
		void test() {
		}

		@Test
		abstract void abstractTest();
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class LocalTestCase {

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}

		@Test
		void test3() {
		}

		@Test
		void test4() {
		}

		@Test
		void test4(TestInfo testInfo) {
		}

		@CustomTestAnnotation
		void customTestAnnotation() {
			/* no-op */
		}

	}

	@Test
	@Retention(RetentionPolicy.RUNTIME)
	@interface CustomTestAnnotation {
	}

	static class TestTemplateClass {

		@TestTemplate
		void testTemplate() {
		}

	}

	static abstract class AbstractSuperClass {
		@Nested
		class NestedInAbstractClass {
			@Test
			void test() {
			}
		}
	}

	static class TestCaseWithExtendedNested {
		@Nested
		class ConcreteInner1 extends AbstractSuperClass {
		}
	}

	static class InvalidTestCases {

		@SuppressWarnings("JUnitMalformedDeclaration")
		static class InvalidTestMethodTestCase {
			@Test
			private static int test() {
				return fail("should not be called");
			}
		}

		static class InvalidTestMethodSubclass1TestCase extends InvalidTestMethodTestCase {
		}

		static class InvalidTestMethodSubclass2TestCase extends InvalidTestMethodTestCase {
		}

		@SuppressWarnings({ "JUnitMalformedDeclaration", "InnerClassMayBeStatic" })
		private class InvalidTestClassTestCase {

			@SuppressWarnings("unused")
			@Test
			void test() {
				fail("should not be called");
			}

			@Nested
			private static class Inner {
				@SuppressWarnings("unused")
				@Test
				void test() {
					fail("should not be called");
				}
			}

		}

		private class InvalidTestClassSubclassTestCase extends InvalidTestClassTestCase {
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class UnrelatedRecursiveHierarchyTestCase {

		@Test
		void test() {
		}

		@SuppressWarnings({ "InnerClassMayBeStatic", "unused" })
		class Inner {
			class Recursive extends Inner {
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class RecursiveHierarchyWithoutNestedTestCase {

		@Test
		void test() {
		}

		@SuppressWarnings({ "InnerClassMayBeStatic", "unused" })
		class Inner extends RecursiveHierarchyWithoutNestedTestCase {
		}
	}

	@SuppressWarnings("unused")
	static class NonTestRecursiveHierarchyTestCase {
		@SuppressWarnings("InnerClassMayBeStatic")
		class Inner extends NonTestRecursiveHierarchyTestCase {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Tag("")
	static class InvalidTagsTestCase {
		@Test
		@Tag("|")
		void test() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@DisplayName("")
	static class BlankDisplayNamesTestCase {
		@Test
		@DisplayName("\t")
		void test() {
		}
	}

	interface InterfaceTestCase {
		@Test
		default void test() {
		}
	}

	static class ConcreteImplementationOfInterfaceTestCase implements InterfaceTestCase {
	}

}
