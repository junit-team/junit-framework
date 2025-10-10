/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import static java.text.MessageFormat.format;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.discovery.PackageNameFilter.includePackageNames;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineDiscoveryResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.runner.manipulation.Filter;
import org.junit.vintage.engine.samples.PlainOldJavaClassWithoutAnyTestsTestCase;
import org.junit.vintage.engine.samples.junit3.JUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails;
import org.junit.vintage.engine.samples.junit3.PlainJUnit3TestCaseWithSingleTestWhichFails;
import org.junit.vintage.engine.samples.junit4.Categories.Failing;
import org.junit.vintage.engine.samples.junit4.Categories.Plain;
import org.junit.vintage.engine.samples.junit4.Categories.Skipped;
import org.junit.vintage.engine.samples.junit4.Categories.SkippedWithReason;
import org.junit.vintage.engine.samples.junit4.EmptyIgnoredTestCase;
import org.junit.vintage.engine.samples.junit4.IgnoredJUnit4TestCase;
import org.junit.vintage.engine.samples.junit4.JUnit4SuiteWithJUnit4TestCaseWithRunnerWithCustomUniqueIdsAndDisplayNames;
import org.junit.vintage.engine.samples.junit4.JUnit4SuiteWithPlainJUnit4TestCaseWithSingleTestWhichIsIgnored;
import org.junit.vintage.engine.samples.junit4.JUnit4SuiteWithTwoTestCases;
import org.junit.vintage.engine.samples.junit4.JUnit4TestCaseWithDistinguishableOverloadedMethod;
import org.junit.vintage.engine.samples.junit4.JUnit4TestCaseWithIndistinguishableOverloadedMethod;
import org.junit.vintage.engine.samples.junit4.JUnit4TestCaseWithNotFilterableRunner;
import org.junit.vintage.engine.samples.junit4.ParameterizedTestCase;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithFiveTestMethods;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithSingleInheritedTestWhichFails;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithSingleTestWhichFails;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithSingleTestWhichIsIgnored;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithTwoTestMethods;
import org.junit.vintage.engine.samples.junit4.SingleFailingTheoryTestCase;
import org.junit.vintage.engine.samples.junit4.TestCaseRunWithJUnitPlatformRunner;

/**
 * @since 4.12
 */
class VintageTestEngineDiscoveryTests {

	@Test
	void resolvesSimpleJUnit4TestClass() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(childDescriptor, testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesIgnoredJUnit4TestClass() throws Exception {
		Class<?> testClass = IgnoredJUnit4TestCase.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		assertThat(runnerDescriptor.getChildren()).hasSize(2);
		List<? extends TestDescriptor> children = new ArrayList<>(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(children.get(0), testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
		assertTestMethodDescriptor(children.get(1), testClass, "succeedingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesEmptyIgnoredTestClass() {
		Class<?> testClass = EmptyIgnoredTestCase.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertFalse(runnerDescriptor.isContainer());
		assertTrue(runnerDescriptor.isTest());
		assertEquals(testClass.getSimpleName(), runnerDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForClass(testClass), runnerDescriptor.getUniqueId());
		assertThat(runnerDescriptor.getChildren()).isEmpty();
	}

	@Test
	void resolvesJUnit4TestClassWithCustomRunner() throws Exception {
		Class<?> testClass = SingleFailingTheoryTestCase.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(childDescriptor, testClass, "theory",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesJUnit3TestCase() throws Exception {
		Class<?> testClass = PlainJUnit3TestCaseWithSingleTestWhichFails.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(childDescriptor, testClass, "test",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesJUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails() throws Exception {
		Class<?> suiteClass = JUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails.class;
		Class<?> testClass = PlainJUnit3TestCaseWithSingleTestWhichFails.class;
		var discoveryRequest = discoveryRequestForClass(suiteClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var suiteDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(suiteDescriptor, suiteClass);
		assertThat(suiteDescriptor.getDisplayName()).describedAs("display name") //
				.startsWith(suiteClass.getSimpleName());
		assertThat(suiteDescriptor.getLegacyReportingName()).describedAs("legacy reporting name") //
				.isEqualTo(suiteClass.getName());

		var testClassDescriptor = getOnlyElement(suiteDescriptor.getChildren());
		assertContainerTestDescriptor(testClassDescriptor, suiteClass, testClass);

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertTestMethodDescriptor(testMethodDescriptor, testClass, "test",
			VintageUniqueIdBuilder.uniqueIdForClasses(suiteClass, testClass));
	}

	@Test
	void resolvesJUnit4SuiteWithPlainJUnit4TestCaseWithSingleTestWhichIsIgnored() throws Exception {
		Class<?> suiteClass = JUnit4SuiteWithPlainJUnit4TestCaseWithSingleTestWhichIsIgnored.class;
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichIsIgnored.class;
		var discoveryRequest = discoveryRequestForClass(suiteClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var suiteDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(suiteDescriptor, suiteClass);

		var testClassDescriptor = getOnlyElement(suiteDescriptor.getChildren());
		assertContainerTestDescriptor(testClassDescriptor, suiteClass, testClass);

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertTestMethodDescriptor(testMethodDescriptor, testClass, "ignoredTest",
			VintageUniqueIdBuilder.uniqueIdForClasses(suiteClass, testClass));
	}

	@Test
	void resolvesJUnit4TestCaseWithIndistinguishableOverloadedMethod() {
		Class<?> testClass = JUnit4TestCaseWithIndistinguishableOverloadedMethod.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		List<TestDescriptor> testMethodDescriptors = new ArrayList<>(runnerDescriptor.getChildren());
		assertThat(testMethodDescriptors).hasSize(2);

		var testMethodDescriptor = testMethodDescriptors.getFirst();
		assertEquals("theory", testMethodDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "theory", "0"),
			testMethodDescriptor.getUniqueId());
		assertClassSource(testClass, testMethodDescriptor);

		testMethodDescriptor = testMethodDescriptors.get(1);
		assertEquals("theory", testMethodDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "theory", "1"),
			testMethodDescriptor.getUniqueId());
		assertClassSource(testClass, testMethodDescriptor);
	}

	@Test
	void resolvesJUnit4TestCaseWithDistinguishableOverloadedMethod() throws Exception {
		Class<?> testClass = JUnit4TestCaseWithDistinguishableOverloadedMethod.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		List<TestDescriptor> testMethodDescriptors = new ArrayList<>(runnerDescriptor.getChildren());

		var testMethodDescriptor = getOnlyElement(testMethodDescriptors);
		assertEquals("test", testMethodDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "test"), testMethodDescriptor.getUniqueId());
		assertMethodSource(testClass.getMethod("test"), testMethodDescriptor);
	}

	@Test
	void doesNotResolvePlainOldJavaClassesWithoutAnyTest() {
		assertYieldsNoDescriptors(PlainOldJavaClassWithoutAnyTestsTestCase.class);
	}

	@Test
	void doesNotResolveClassRunWithJUnitPlatform() {
		assertYieldsNoDescriptors(TestCaseRunWithJUnitPlatformRunner.class);
	}

	@Test
	void resolvesClasspathSelector() throws Exception {
		var root = getClasspathRoot(PlainJUnit4TestCaseWithSingleTestWhichFails.class);
		var discoveryRequest = request().selectors(selectClasspathRoots(Set.of(root))).build();
		var engineDescriptor = discoverTests(discoveryRequest);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(PlainJUnit4TestCaseWithSingleTestWhichFails.class.getSimpleName())
			.contains(PlainJUnit3TestCaseWithSingleTestWhichFails.class.getSimpleName())
			.doesNotContain(PlainOldJavaClassWithoutAnyTestsTestCase.class.getSimpleName());
		// @formatter:on
	}

	@Test
	void resolvesClasspathSelectorForJarFile() throws Exception {
		var jarUrl = getClass().getResource("/vintage-testjar.jar");
		var jarFile = Path.of(jarUrl.toURI());

		var originalClassLoader = Thread.currentThread().getContextClassLoader();
		try (var classLoader = new URLClassLoader(new URL[] { jarUrl })) {
			Thread.currentThread().setContextClassLoader(classLoader);

			var discoveryRequest = request().selectors(selectClasspathRoots(Set.of(jarFile))).build();
			var engineDescriptor = discoverTests(discoveryRequest);

			// @formatter:off
			assertThat(engineDescriptor.getChildren())
					.extracting(TestDescriptor::getDisplayName)
					.containsExactly("JUnit4Test");
			// @formatter:on
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	@Test
	void resolvesApplyingClassNameFilters() throws Exception {
		var root = getClasspathRoot(PlainJUnit4TestCaseWithSingleTestWhichFails.class);

		var discoveryRequest = request().selectors(selectClasspathRoots(Set.of(root))).filters(
			includeClassNamePatterns(".*JUnit4.*"), includeClassNamePatterns(".*Plain.*")).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(PlainJUnit4TestCaseWithSingleTestWhichFails.class.getSimpleName())
			.doesNotContain(JUnit4TestCaseWithIndistinguishableOverloadedMethod.class.getSimpleName())
			.doesNotContain(PlainJUnit3TestCaseWithSingleTestWhichFails.class.getSimpleName());
		// @formatter:on
	}

	@Test
	void resolvesApplyingPackageNameFilters() throws Exception {
		var root = getClasspathRoot(PlainJUnit4TestCaseWithSingleTestWhichFails.class);

		var discoveryRequest = request().selectors(selectClasspathRoots(Set.of(root))).filters(
			includePackageNames("org"), includePackageNames("org.junit")).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
				.extracting(TestDescriptor::getDisplayName)
				.contains(PlainJUnit4TestCaseWithSingleTestWhichFails.class.getSimpleName());
		// @formatter:on
	}

	@Test
	void resolvesPackageSelectorForJUnit4SamplesPackage() {
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;

		var discoveryRequest = request().selectors(selectPackage(testClass.getPackage().getName())).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(testClass.getSimpleName())
			.doesNotContain(PlainJUnit3TestCaseWithSingleTestWhichFails.class.getSimpleName());
		// @formatter:on
	}

	@Test
	void resolvesPackageSelectorForJUnit3SamplesPackage() {
		Class<?> testClass = PlainJUnit3TestCaseWithSingleTestWhichFails.class;

		var discoveryRequest = request().selectors(selectPackage(testClass.getPackage().getName())).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(testClass.getSimpleName())
			.doesNotContain(PlainJUnit4TestCaseWithSingleTestWhichFails.class.getSimpleName());
		// @formatter:on
	}

	@Test
	void resolvesClassesWithInheritedMethods() throws Exception {
		Class<?> superclass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;
		Class<?> testClass = PlainJUnit4TestCaseWithSingleInheritedTestWhichFails.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertEquals(testClass.getSimpleName(), runnerDescriptor.getDisplayName());
		assertClassSource(testClass, runnerDescriptor);

		var testDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertEquals("failingTest", testDescriptor.getDisplayName());
		assertMethodSource(testClass, superclass.getMethod("failingTest"), testDescriptor);
	}

	@Test
	void resolvesCategoriesIntoTags() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = discoveryRequestForClass(testClass);

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(runnerDescriptor.getTags()).containsOnly(TestTag.create(Plain.class.getName()));

		var failingTest = findChildByDisplayName(runnerDescriptor, "failingTest");
		assertThat(failingTest.getTags()).containsOnly(//
			TestTag.create(Plain.class.getName()), //
			TestTag.create(Failing.class.getName()));

		var ignoredWithoutReason = findChildByDisplayName(runnerDescriptor, "ignoredTest1_withoutReason");
		assertThat(ignoredWithoutReason.getTags()).containsOnly(//
			TestTag.create(Plain.class.getName()), //
			TestTag.create(Skipped.class.getName()));

		var ignoredWithReason = findChildByDisplayName(runnerDescriptor, "ignoredTest2_withReason");
		assertThat(ignoredWithReason.getTags()).containsOnly(//
			TestTag.create(Plain.class.getName()), //
			TestTag.create(Skipped.class.getName()), //
			TestTag.create(SkippedWithReason.class.getName()));
	}

	@Test
	void resolvesMethodSelectorForSingleMethod() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors(selectMethod(testClass, testClass.getMethod("failingTest"))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(childDescriptor, testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesMethodOfIgnoredJUnit4TestClass() throws Exception {
		Class<?> testClass = IgnoredJUnit4TestCase.class;
		var discoveryRequest = request().selectors(selectMethod(testClass, testClass.getMethod("failingTest"))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(childDescriptor, testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesMethodSelectorForTwoMethodsOfSameClass() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors(selectMethod(testClass, testClass.getMethod("failingTest")),
			selectMethod(testClass, testClass.getMethod("successfulTest"))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		List<TestDescriptor> testMethodDescriptors = new ArrayList<>(runnerDescriptor.getChildren());
		assertThat(testMethodDescriptors).hasSize(2);

		var failingTest = testMethodDescriptors.get(0);
		assertTestMethodDescriptor(failingTest, testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));

		var successfulTest = testMethodDescriptors.get(1);
		assertTestMethodDescriptor(successfulTest, testClass, "successfulTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesUniqueIdSelectorForSingleMethod() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors(
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "failingTest"))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(childDescriptor, testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void resolvesUniqueIdSelectorForSingleClass() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors(
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForClass(testClass))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		assertThat(runnerDescriptor.getChildren()).hasSize(5);
	}

	@Test
	void resolvesUniqueIdSelectorOfSingleClassWithinSuite() throws Exception {
		Class<?> suiteClass = JUnit4SuiteWithTwoTestCases.class;
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;
		var discoveryRequest = request().selectors(
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForClasses(suiteClass, testClass))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var suiteDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(suiteDescriptor, suiteClass);

		var testClassDescriptor = getOnlyElement(suiteDescriptor.getChildren());
		assertContainerTestDescriptor(testClassDescriptor, suiteClass, testClass);

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertTestMethodDescriptor(testMethodDescriptor, testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClasses(suiteClass, testClass));
	}

	@Test
	void resolvesUniqueIdSelectorOfSingleMethodWithinSuite() throws Exception {
		Class<?> suiteClass = JUnit4SuiteWithTwoTestCases.class;
		Class<?> testClass = PlainJUnit4TestCaseWithTwoTestMethods.class;
		var discoveryRequest = request().selectors(selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(
			VintageUniqueIdBuilder.uniqueIdForClasses(suiteClass, testClass), testClass, "successfulTest"))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var suiteDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(suiteDescriptor, suiteClass);

		var testClassDescriptor = getOnlyElement(suiteDescriptor.getChildren());
		assertContainerTestDescriptor(testClassDescriptor, suiteClass, testClass);

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertTestMethodDescriptor(testMethodDescriptor, testClass, "successfulTest",
			VintageUniqueIdBuilder.uniqueIdForClasses(suiteClass, testClass));
	}

	@Test
	void resolvesMultipleUniqueIdSelectorsForMethodsOfSameClass() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithTwoTestMethods.class;
		var discoveryRequest = request().selectors(
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "successfulTest")),
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "failingTest"))).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		List<TestDescriptor> testMethodDescriptors = new ArrayList<>(runnerDescriptor.getChildren());
		assertThat(testMethodDescriptors).hasSize(2);
		assertTestMethodDescriptor(testMethodDescriptors.get(0), testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
		assertTestMethodDescriptor(testMethodDescriptors.get(1), testClass, "successfulTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void doesNotResolveMissingUniqueIdSelectorForSingleClass() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors(
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForClass(testClass) + "/[test:doesNotExist]")).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var testDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertInitializationError(testDescriptor, Filter.class, testClass);
	}

	@Test
	void ignoresMoreFineGrainedSelectorsWhenClassIsSelectedAsWell() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors( //
			selectMethod(testClass, testClass.getMethod("failingTest")), //
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "abortedTest")), selectClass(testClass) //
		).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		assertThat(runnerDescriptor.getChildren()).hasSize(5);
	}

	@Test
	void resolvesCombinationOfMethodAndUniqueIdSelector() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors( //
			selectMethod(testClass, testClass.getMethod("failingTest")), //
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "abortedTest") //
			)).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		List<TestDescriptor> testMethodDescriptors = new ArrayList<>(runnerDescriptor.getChildren());
		assertThat(testMethodDescriptors).hasSize(2);
		assertTestMethodDescriptor(testMethodDescriptors.get(0), testClass, "abortedTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
		assertTestMethodDescriptor(testMethodDescriptors.get(1), testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void ignoresRedundantSelector() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		var discoveryRequest = request().selectors( //
			selectMethod(testClass, testClass.getMethod("failingTest")), //
			selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "failingTest") //
			)).build();

		var engineDescriptor = discoverTests(discoveryRequest);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var testMethodDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTestMethodDescriptor(testMethodDescriptor, testClass, "failingTest",
			VintageUniqueIdBuilder.uniqueIdForClass(testClass));
	}

	@Test
	void doesNotResolveMethodOfClassNotAcceptedByClassNameFilter() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		// @formatter:off
		var request = request()
				.selectors(selectMethod(testClass, testClass.getMethod("failingTest")))
				.filters(includeClassNamePatterns("Foo"))
				.build();
		// @formatter:on

		assertYieldsNoDescriptors(request);
	}

	@Test
	void doesNotResolveMethodOfClassNotAcceptedByPackageNameFilter() throws Exception {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		// @formatter:off
		var request = request()
				.selectors(selectMethod(testClass, testClass.getMethod("failingTest")))
				.filters(includePackageNames("com.acme"))
				.build();
		// @formatter:on

		assertYieldsNoDescriptors(request);
	}

	@Test
	void resolvesClassForMethodSelectorForClassWithNonFilterableRunner() {
		Class<?> testClass = JUnit4TestCaseWithNotFilterableRunner.class;
		// @formatter:off
		var request = request()
				.selectors(selectUniqueId(VintageUniqueIdBuilder.uniqueIdForMethod(testClass, "Test #0")))
				.build();
		// @formatter:on

		var engineDescriptor = discoverTests(request);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertEquals(testClass.getSimpleName(), runnerDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForClass(testClass), runnerDescriptor.getUniqueId());
		assertThat(runnerDescriptor.getChildren()).isNotEmpty();
	}

	@Test
	void usesCustomUniqueIdsAndDisplayNamesWhenPresent() {
		Class<?> suiteClass = JUnit4SuiteWithJUnit4TestCaseWithRunnerWithCustomUniqueIdsAndDisplayNames.class;
		var request = request().selectors(selectClass(suiteClass)).build();

		var engineDescriptor = discoverTests(request);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, suiteClass);

		var testClassDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertEquals("(TestClass)", testClassDescriptor.getDisplayName());

		var childDescriptor = getOnlyElement(testClassDescriptor.getChildren());

		var prefix = VintageUniqueIdBuilder.uniqueIdForClass(suiteClass);
		assertThat(childDescriptor.getUniqueId().toString()).startsWith(prefix.toString());
		assertEquals("(TestMethod)", childDescriptor.getDisplayName());

		var customUniqueIdValue = childDescriptor.getUniqueId().getSegments().get(2).getType();
		assertNotNull(Base64.getDecoder().decode(customUniqueIdValue.getBytes(StandardCharsets.UTF_8)),
			"is a valid Base64 encoding scheme");
	}

	@Test
	void resolvesTestSourceForParameterizedTests() throws Exception {
		Class<?> testClass = ParameterizedTestCase.class;
		var request = request().selectors(selectClass(testClass)).build();

		var engineDescriptor = discoverTests(request);

		var runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertRunnerTestDescriptor(runnerDescriptor, testClass);

		var fooParentDescriptor = findChildByDisplayName(runnerDescriptor, "[foo]");
		assertTrue(fooParentDescriptor.isContainer());
		assertFalse(fooParentDescriptor.isTest());
		assertThat(fooParentDescriptor.getSource()).isEmpty();

		var testMethodDescriptor = getOnlyElement(fooParentDescriptor.getChildren());
		assertEquals("test[foo]", testMethodDescriptor.getDisplayName());
		assertTrue(testMethodDescriptor.isTest());
		assertFalse(testMethodDescriptor.isContainer());
		assertMethodSource(testClass.getMethod("test"), testMethodDescriptor);
	}

	@Test
	void reportsNoDiscoveryIssuesWhenNoTestsAreFound() {
		var request = discoveryRequestForClass(PlainOldJavaClassWithoutAnyTestsTestCase.class);

		var results = discover(request);

		assertThat(results.getDiscoveryIssues()).isEmpty();
	}

	@Test
	void reportDiscoveryIssueWhenTestsAreFoundByDefault() {
		var request = discoveryRequestForClass(PlainJUnit4TestCaseWithSingleTestWhichFails.class);

		var results = discover(request);

		assertThat(results.getDiscoveryIssues()).hasSize(1);

		var issue = results.getDiscoveryIssues().getFirst();
		assertThat(issue.severity()).isEqualTo(Severity.INFO);
		assertThat(issue.message()).contains("JUnit Vintage engine is deprecated");
	}

	@SuppressWarnings("deprecation")
	@Test
	void reportNoDiscoveryIssueWhenTestsAreFoundButConfigurationParameterIsSet() {
		var request = request() //
				.selectors(selectClass(PlainJUnit4TestCaseWithSingleTestWhichFails.class)) //
				.configurationParameter(Constants.DISCOVERY_ISSUE_REPORTING_ENABLED_PROPERTY_NAME, "false").build();

		var results = discover(request);

		assertThat(results.getDiscoveryIssues()).isEmpty();
	}

	private TestDescriptor findChildByDisplayName(TestDescriptor runnerDescriptor, String displayName) {
		// @formatter:off
		var children = runnerDescriptor.getChildren();
		return children
				.stream()
				.filter(where(TestDescriptor::getDisplayName, isEqual(displayName)))
				.findAny()
				.orElseThrow(() ->
					new AssertionError(format("No child with display name \"{0}\" in {1}", displayName, children)));
		// @formatter:on
	}

	private TestDescriptor discoverTests(LauncherDiscoveryRequest discoveryRequest) {
		return discover(discoveryRequest).getEngineDescriptor();
	}

	@SuppressWarnings("deprecation")
	private static EngineDiscoveryResults discover(LauncherDiscoveryRequest discoveryRequest) {
		return EngineTestKit.discover(new VintageTestEngine(), discoveryRequest);
	}

	private Path getClasspathRoot(Class<?> testClass) throws Exception {
		var location = testClass.getProtectionDomain().getCodeSource().getLocation();
		return Path.of(location.toURI());
	}

	private void assertYieldsNoDescriptors(Class<?> testClass) {
		var request = discoveryRequestForClass(testClass);

		assertYieldsNoDescriptors(request);
	}

	private void assertYieldsNoDescriptors(LauncherDiscoveryRequest request) {
		var engineDescriptor = discoverTests(request);

		assertThat(engineDescriptor.getChildren()).isEmpty();
	}

	private static void assertRunnerTestDescriptor(TestDescriptor runnerDescriptor, Class<?> testClass) {
		assertTrue(runnerDescriptor.isContainer());
		assertFalse(runnerDescriptor.isTest());
		assertEquals(testClass.getSimpleName(), runnerDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForClass(testClass), runnerDescriptor.getUniqueId());
		assertClassSource(testClass, runnerDescriptor);
	}

	private static void assertTestMethodDescriptor(TestDescriptor testMethodDescriptor, Class<?> testClass,
			String methodName, UniqueId uniqueContainerId) throws Exception {
		assertTrue(testMethodDescriptor.isTest());
		assertFalse(testMethodDescriptor.isContainer());
		assertEquals(methodName, testMethodDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForMethod(uniqueContainerId, testClass, methodName),
			testMethodDescriptor.getUniqueId());
		assertThat(testMethodDescriptor.getChildren()).isEmpty();
		assertMethodSource(testClass.getMethod(methodName), testMethodDescriptor);
	}

	private static void assertContainerTestDescriptor(TestDescriptor containerDescriptor, Class<?> suiteClass,
			Class<?> testClass) {
		assertTrue(containerDescriptor.isContainer());
		assertFalse(containerDescriptor.isTest());
		assertEquals(testClass.getName(), containerDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForClasses(suiteClass, testClass),
			containerDescriptor.getUniqueId());
		assertClassSource(testClass, containerDescriptor);
	}

	private static void assertInitializationError(TestDescriptor testDescriptor, Class<?> failingClass,
			Class<?> testClass) {
		assertTrue(testDescriptor.isTest());
		assertFalse(testDescriptor.isContainer());
		assertEquals("initializationError", testDescriptor.getDisplayName());
		assertEquals(VintageUniqueIdBuilder.uniqueIdForErrorInClass(testClass, failingClass),
			testDescriptor.getUniqueId());
		assertThat(testDescriptor.getChildren()).isEmpty();
		assertClassSource(failingClass, testDescriptor);
	}

	private static void assertClassSource(Class<?> expectedClass, TestDescriptor testDescriptor) {
		assertThat(testDescriptor.getSource()).containsInstanceOf(ClassSource.class);
		var classSource = (ClassSource) testDescriptor.getSource().get();
		assertThat(classSource.getJavaClass()).isEqualTo(expectedClass);
	}

	private static void assertMethodSource(Method expectedMethod, TestDescriptor testDescriptor) {
		assertMethodSource(expectedMethod.getDeclaringClass(), expectedMethod, testDescriptor);
	}

	private static void assertMethodSource(Class<?> expectedClass, Method expectedMethod,
			TestDescriptor testDescriptor) {
		assertThat(testDescriptor.getSource()).containsInstanceOf(MethodSource.class);
		var methodSource = (MethodSource) testDescriptor.getSource().get();
		assertThat(methodSource.getClassName()).isEqualTo(expectedClass.getName());
		assertThat(methodSource.getMethodName()).isEqualTo(expectedMethod.getName());
		assertThat(methodSource.getMethodParameterTypes()).isEqualTo(
			ClassUtils.nullSafeToString(expectedMethod.getParameterTypes()));
	}

	private static LauncherDiscoveryRequest discoveryRequestForClass(Class<?> testClass) {
		return request().selectors(selectClass(testClass)).build();
	}

}
