/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilder.request;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.FilePosition;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UriSelector;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.ConfigurationParametersResource;
import org.junit.platform.suite.api.DisableParentConfigurationParameters;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.ExcludeEngines;
import org.junit.platform.suite.api.ExcludePackages;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.IncludePackages;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.Select;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.SelectDirectories;
import org.junit.platform.suite.api.SelectFile;
import org.junit.platform.suite.api.SelectMethod;
import org.junit.platform.suite.api.SelectModules;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.SelectUris;

class SuiteLauncherDiscoveryRequestBuilderTests {

	SuiteLauncherDiscoveryRequestBuilder builder = request();

	@Test
	void configurationParameter() {
		@ConfigurationParameter(key = "com.example", value = "*")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applyConfigurationParametersFromSuite(Suite.class).build();
		ConfigurationParameters configuration = request.getConfigurationParameters();
		Optional<String> parameter = configuration.get("com.example");
		assertEquals(Optional.of("*"), parameter);
	}

	@Test
	void configurationParametersResource() {
		@ConfigurationParametersResource("config-test.properties")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applyConfigurationParametersFromSuite(Suite.class).build();
		ConfigurationParameters configuration = request.getConfigurationParameters();
		Optional<String> parameter = configuration.get("com.example.prop.first");
		assertEquals(Optional.of("first value"), parameter);
	}

	@Test
	void configurationParametersResources() {
		@ConfigurationParametersResource("config-test.properties")
		@ConfigurationParametersResource("config-test-override.properties")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applyConfigurationParametersFromSuite(Suite.class).build();
		ConfigurationParameters configuration = request.getConfigurationParameters();
		Optional<String> parameterOne = configuration.get("com.example.prop.first");
		assertEquals(Optional.of("first value from override file"), parameterOne);
		Optional<String> parameterTwo = configuration.get("com.example.prop.second");
		assertEquals(Optional.of("second value"), parameterTwo);
	}

	@Test
	void configurationParametersResource_explicitParametersTakePrecedence() {
		@ConfigurationParametersResource("config-test.properties")
		@ConfigurationParameter(key = "com.example.prop.first", value = "first value from explicit parameter")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applyConfigurationParametersFromSuite(Suite.class).build();
		ConfigurationParameters configuration = request.getConfigurationParameters();
		Optional<String> parameterOne = configuration.get("com.example.prop.first");
		assertEquals(Optional.of("first value from explicit parameter"), parameterOne);
		Optional<String> parameterTwo = configuration.get("com.example.prop.second");
		assertEquals(Optional.of("second value"), parameterTwo);
	}

	@Test
	void excludeClassNamePatterns() {
		class TestCase {
		}
		@ExcludeClassNamePatterns("^.*TestCase$")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClassNameFilter> filters = request.getFiltersByType(ClassNameFilter.class);
		assertTrue(exactlyOne(filters).apply(TestCase.class.getName()).excluded());
	}

	@Test
	void excludeEngines() {
		@ExcludeEngines("junit-jupiter")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<EngineFilter> filters = request.getEngineFilters();
		assertTrue(exactlyOne(filters).apply(new JupiterTestEngine()).excluded());
	}

	@Test
	void excludePackages() {
		@ExcludePackages("com.example.testcases")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<PackageNameFilter> filters = request.getFiltersByType(PackageNameFilter.class);
		assertTrue(exactlyOne(filters).apply("com.example.testcases").excluded());
	}

	@Test
	void excludeTags() {
		@ExcludeTags("test-tag")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<PostDiscoveryFilter> filters = request.getPostDiscoveryFilters();
		TestDescriptor testDescriptor = new StubAbstractTestDescriptor();
		assertTrue(exactlyOne(filters).apply(testDescriptor).excluded());
	}

	@Test
	void includeClassNamePatterns() {
		class TestCase {
		}
		@IncludeClassNamePatterns("^.*TestCase$")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClassNameFilter> filters = request.getFiltersByType(ClassNameFilter.class);
		assertTrue(exactlyOne(filters).apply(TestCase.class.getName()).included());
		assertTrue(exactlyOne(filters).apply(Suite.class.getName()).excluded());
	}

	@Test
	void filtersOnStandardClassNamePatternsWhenIncludeClassNamePatternsIsOmitted() {
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		assertTrue(request.getFiltersByType(ClassNameFilter.class).isEmpty());
	}

	@Test
	void filtersOnStandardClassNamePatternsWhenIncludeClassNamePatternsIsOmittedUnlessDisabled() {
		class ExampleTest {
		}
		class Suite {
		}

		// @formatter:off
		LauncherDiscoveryRequest request = builder
				.filterStandardClassNamePatterns()
				.applySelectorsAndFiltersFromSuite(Suite.class)
				.build();
		// @formatter:on
		List<ClassNameFilter> filters = request.getFiltersByType(ClassNameFilter.class);
		assertTrue(exactlyOne(filters).apply(ExampleTest.class.getName()).included());
		assertTrue(exactlyOne(filters).apply(Suite.class.getName()).excluded());
	}

	@Test
	void includeEngines() {
		@IncludeEngines("junit-jupiter")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<EngineFilter> filters = request.getEngineFilters();
		assertTrue(exactlyOne(filters).apply(new JupiterTestEngine()).included());
	}

	@Test
	void includePackages() {
		@IncludePackages("com.example.testcases")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<PackageNameFilter> filters = request.getFiltersByType(PackageNameFilter.class);
		assertTrue(exactlyOne(filters).apply("com.example.testcases").included());
	}

	@Test
	void includeTags() {
		@IncludeTags("test-tag")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<PostDiscoveryFilter> filters = request.getPostDiscoveryFilters();
		TestDescriptor testDescriptor = new StubAbstractTestDescriptor();
		assertTrue(exactlyOne(filters).apply(testDescriptor).included());
	}

	@Test
	void selectClassesByReference() {
		class TestCase {
		}
		@SelectClasses(TestCase.class)
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClassSelector> selectors = request.getSelectorsByType(ClassSelector.class);
		assertFalse(selectors.isEmpty());
		assertEquals(TestCase.class, exactlyOne(selectors).getJavaClass());
	}

	@Test
	void selectClassesByName() {
		@SelectClasses(names = "org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$NonLocalTestCase")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClassSelector> selectors = request.getSelectorsByType(ClassSelector.class);
		assertEquals(NonLocalTestCase.class, exactlyOne(selectors).getJavaClass());
	}

	@Test
	void selectClassesWithoutReferencesOrNames() {
		@SelectClasses
		class Suite {
		}

		assertPreconditionViolationFor(() -> builder.applySelectorsAndFiltersFromSuite(Suite.class))//
				.withMessageMatching("@SelectClasses on class \\["
						+ Pattern.quote(SuiteLauncherDiscoveryRequestBuilderTests.class.getName())
						+ "\\$\\d+Suite] must declare at least one class reference or name");
	}

	static class NonLocalTestCase {
	}

	@TestFactory
	Stream<DynamicTest> selectOneMethodWithNoParameters() {
		@SelectMethod("org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$NoParameterTestCase#testMethod")
		class SuiteA {
		}
		@SelectMethod(type = NoParameterTestCase.class, name = "testMethod")
		class SuiteB {
		}
		@SelectMethod(typeName = "org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$NoParameterTestCase", name = "testMethod")
		class SuiteC {
		}

		return Stream.of(SuiteA.class, SuiteB.class, SuiteC.class) //
				.map(suiteClass -> dynamicTest(suiteClass.getSimpleName(), () -> {
					LauncherDiscoveryRequest request = request().applySelectorsAndFiltersFromSuite(suiteClass).build();
					List<MethodSelector> selectors = request.getSelectorsByType(MethodSelector.class);
					assertEquals(DiscoverySelectors.selectMethod(NoParameterTestCase.class, "testMethod"),
						exactlyOne(selectors));
				}));
	}

	static class NoParameterTestCase {
		@SuppressWarnings("unused")
		void testMethod() {
		}
	}

	@TestFactory
	Stream<DynamicTest> selectOneMethodWithOneParameter() {
		@SelectMethod("org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$OneParameterTestCase#testMethod(int)")
		class SuiteA {
		}
		@SelectMethod(type = OneParameterTestCase.class, name = "testMethod", parameterTypeNames = "int")
		class SuiteB {
		}
		@SelectMethod(type = OneParameterTestCase.class, name = "testMethod", parameterTypes = int.class)
		class SuiteC {
		}
		@SelectMethod(typeName = "org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$OneParameterTestCase", name = "testMethod", parameterTypeNames = "int")
		class SuiteD {
		}
		@SelectMethod(typeName = "org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$OneParameterTestCase", name = "testMethod", parameterTypes = int.class)
		class SuiteE {
		}

		return Stream.of(SuiteA.class, SuiteB.class, SuiteC.class, SuiteD.class, SuiteE.class) //
				.map(suiteClass -> dynamicTest(suiteClass.getSimpleName(), () -> {
					LauncherDiscoveryRequest request = request().applySelectorsAndFiltersFromSuite(suiteClass).build();
					List<MethodSelector> selectors = request.getSelectorsByType(MethodSelector.class);
					assertEquals(DiscoverySelectors.selectMethod(OneParameterTestCase.class, "testMethod", "int"),
						exactlyOne(selectors));
				}));
	}

	static class OneParameterTestCase {
		@SuppressWarnings("unused")
		void testMethod(int i) {
		}
	}

	@Test
	void selectTwoMethodsWithTwoParameters() {
		@SuppressWarnings("unused")
		class TestClass {
			void firstTestMethod(int i, String j) {
			}

			void secondTestMethod(boolean i, float j) {
			}
		}
		@SelectMethod(type = TestClass.class, name = "firstTestMethod", parameterTypeNames = "int, java.lang.String")
		@SelectMethod(type = TestClass.class, name = "secondTestMethod", parameterTypes = { boolean.class,
				float.class })
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<MethodSelector> selectors = request.getSelectorsByType(MethodSelector.class);
		assertEquals(2, selectors.size());
		assertEquals(DiscoverySelectors.selectMethod(TestClass.class, "firstTestMethod", "int, java.lang.String"),
			selectors.get(0));
		assertEquals(DiscoverySelectors.selectMethod(TestClass.class, "secondTestMethod", "boolean, float"),
			selectors.get(1));
	}

	@TestFactory
	Stream<DynamicTest> selectMethodCausesExceptionOnInvalidUsage() {
		@SelectMethod(value = "irrelevant", type = NoParameterTestCase.class)
		class ValueAndType {
		}
		@SelectMethod(value = "SomeClass#someMethod", typeName = "org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$NoParameterTestCase")
		class ValueAndTypeName {
		}
		@SelectMethod(value = "SomeClass#someMethod", name = "testMethod")
		class ValueAndMethodName {
		}
		@SelectMethod(value = "SomeClass#someMethod", parameterTypes = int.class)
		class ValueAndParameterTypes {
		}
		@SelectMethod(value = "SomeClass#someMethod", parameterTypeNames = "int")
		class ValueAndParameterTypeNames {
		}
		@SelectMethod(type = NoParameterTestCase.class)
		class MissingMethodName {
		}
		@SelectMethod(name = "testMethod", type = NoParameterTestCase.class, typeName = "org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$NoParameterTestCase")
		class TypeAndTypeName {
		}
		@SelectMethod(name = "testMethod", parameterTypes = int.class, parameterTypeNames = "int")
		class ParameterTypesAndParameterTypeNames {
		}

		var expectedFailureMessages = Map.ofEntries( //
			entry(ValueAndType.class, "type must not be set in conjunction with fully qualified method name"), //
			entry(ValueAndTypeName.class, "type name must not be set in conjunction with fully qualified method name"), //
			entry(ValueAndMethodName.class,
				"method name must not be set in conjunction with fully qualified method name"), //
			entry(ValueAndParameterTypes.class,
				"parameter types must not be set in conjunction with fully qualified method name"), //
			entry(ValueAndParameterTypeNames.class,
				"parameter type names must not be set in conjunction with fully qualified method name"), //
			entry(MissingMethodName.class, "method name must not be blank"), //
			entry(TypeAndTypeName.class, "either type name or type must be set but not both"), //
			entry(ParameterTypesAndParameterTypeNames.class,
				"either parameter type names or parameter types must be set but not both") //
		);
		return expectedFailureMessages.entrySet().stream() //
				.map(entry -> {
					Class<?> suiteClassName = entry.getKey();
					var expectedFailureMessage = entry.getValue();
					return dynamicTest(suiteClassName.getSimpleName(), () -> {
						assertPreconditionViolationFor(
							() -> request().applySelectorsAndFiltersFromSuite(suiteClassName))//
									.withMessage("@SelectMethod on class [" + suiteClassName.getName() + "]: "
											+ expectedFailureMessage);
					});
				});
	}

	@Test
	void selectClasspathResource() {
		@SelectClasspathResource("com.example.testcases")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClasspathResourceSelector> selectors = request.getSelectorsByType(ClasspathResourceSelector.class);
		assertEquals("com.example.testcases", exactlyOne(selectors).getClasspathResourceName());
	}

	@Test
	void selectClasspathResourcePosition() {
		@SelectClasspathResource(value = "com.example.testcases", line = 42)
		@SelectClasspathResource(value = "com.example.testcases", line = 14, column = 15)
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClasspathResourceSelector> selectors = request.getSelectorsByType(ClasspathResourceSelector.class);
		assertEquals(Optional.of(FilePosition.from(42)), selectors.get(0).getPosition());
		assertEquals(Optional.of(FilePosition.from(14, 15)), selectors.get(1).getPosition());
	}

	@Test
	void ignoreClasspathResourcePosition() {
		@SelectClasspathResource(value = "com.example.testcases", line = -1)
		@SelectClasspathResource(value = "com.example.testcases", column = 12)
		@SelectClasspathResource(value = "com.example.testcases", line = 42, column = -12)
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClasspathResourceSelector> selectors = request.getSelectorsByType(ClasspathResourceSelector.class);
		assertEquals(Optional.empty(), selectors.get(0).getPosition());
		assertEquals(Optional.empty(), selectors.get(1).getPosition());
		assertEquals(Optional.of(FilePosition.from(42)), selectors.get(2).getPosition());
	}

	@Test
	void selectDirectories() {
		@SelectDirectories("path/to/root")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<DirectorySelector> selectors = request.getSelectorsByType(DirectorySelector.class);
		assertEquals(Path.of("path/to/root"), exactlyOne(selectors).getPath());
	}

	@Test
	void selectDirectoriesFiltersEmptyPaths() {
		@SelectDirectories("")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		assertTrue(request.getSelectorsByType(DirectorySelector.class).isEmpty());
	}

	@Test
	void selectFile() {
		@SelectFile("path/to/root")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<FileSelector> selectors = request.getSelectorsByType(FileSelector.class);
		assertEquals(Path.of("path/to/root"), exactlyOne(selectors).getPath());
	}

	@Test
	void selectFilePosition() {
		@SelectFile(value = "path/to/root", line = 42)
		@SelectFile(value = "path/to/root", line = 14, column = 15)
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<FileSelector> selectors = request.getSelectorsByType(FileSelector.class);
		assertEquals(Optional.of(FilePosition.from(42)), selectors.get(0).getPosition());
		assertEquals(Optional.of(FilePosition.from(14, 15)), selectors.get(1).getPosition());
	}

	@Test
	void ignoreInvalidFilePosition() {
		@SelectFile(value = "path/to/root", line = -1)
		@SelectFile(value = "path/to/root", column = 12)
		@SelectFile(value = "path/to/root", line = 42, column = -12)
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<FileSelector> selectors = request.getSelectorsByType(FileSelector.class);
		assertEquals(Optional.empty(), selectors.get(0).getPosition());
		assertEquals(Optional.empty(), selectors.get(1).getPosition());
		assertEquals(Optional.of(FilePosition.from(42)), selectors.get(2).getPosition());
	}

	@Test
	void selectModules() {
		@SelectModules("com.example.testcases")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ModuleSelector> selectors = request.getSelectorsByType(ModuleSelector.class);
		assertEquals("com.example.testcases", exactlyOne(selectors).getModuleName());
	}

	@Test
	void selectUris() {
		@SelectUris("path/to/root")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<UriSelector> selectors = request.getSelectorsByType(UriSelector.class);
		assertEquals(URI.create("path/to/root"), exactlyOne(selectors).getUri());
	}

	@Test
	void selectUrisFiltersEmptyUris() {
		@SelectUris("")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		assertTrue(request.getSelectorsByType(UriSelector.class).isEmpty());
	}

	@Test
	void selectPackages() {
		@SelectPackages("com.example.testcases")
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<PackageSelector> selectors = request.getSelectorsByType(PackageSelector.class);
		assertEquals("com.example.testcases", exactlyOne(selectors).getPackageName());
	}

	@SelectPackages("com.example.testcases")
	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta {
	}

	@Test
	void metaAnnotations() {
		@Meta
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<PackageSelector> pSelectors = request.getSelectorsByType(PackageSelector.class);
		assertEquals("com.example.testcases", exactlyOne(pSelectors).getPackageName());
	}

	@Test
	void enableParentConfigurationParametersByDefault() {
		class Suite {

		}
		// @formatter:off
		var configuration = new ParentConfigurationParameters("parent", "parent parameters were used");
		var request = builder.applyConfigurationParametersFromSuite(Suite.class)
				.parentConfigurationParameters(configuration)
				.build();
		// @formatter:on
		var configurationParameters = request.getConfigurationParameters();
		assertEquals(Optional.of("parent parameters were used"), configurationParameters.get("parent"));
	}

	@Test
	void disableParentConfigurationParameters() {
		@DisableParentConfigurationParameters
		class Suite {

		}
		// @formatter:off
		var configuration = new ParentConfigurationParameters("parent", "parent parameters were used");
		var request = builder.applyConfigurationParametersFromSuite(Suite.class)
				.parentConfigurationParameters(configuration)
				.build();
		// @formatter:on
		var configurationParameters = request.getConfigurationParameters();
		assertEquals(Optional.empty(), configurationParameters.get("parent"));
	}

	@Test
	void selectByIdentifier() {
		// @formatter:off
		@Select({
				"class:org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$NonLocalTestCase",
				"method:org.junit.platform.suite.engine.SuiteLauncherDiscoveryRequestBuilderTests$NoParameterTestCase#testMethod"
		})
		// @formatter:on
		class Suite {
		}

		LauncherDiscoveryRequest request = builder.applySelectorsAndFiltersFromSuite(Suite.class).build();
		List<ClassSelector> classSelectors = request.getSelectorsByType(ClassSelector.class);
		assertEquals(NonLocalTestCase.class, exactlyOne(classSelectors).getJavaClass());
		List<MethodSelector> methodSelectors = request.getSelectorsByType(MethodSelector.class);
		// @formatter:off
		assertThat(exactlyOne(methodSelectors))
				.extracting(MethodSelector::getJavaClass, MethodSelector::getMethodName)
				.containsExactly(NoParameterTestCase.class, "testMethod");
		// @formatter:on
	}

	private static <T> T exactlyOne(List<T> list) {
		return CollectionUtils.getOnlyElement(list);
	}

	private static class StubAbstractTestDescriptor extends AbstractTestDescriptor {

		StubAbstractTestDescriptor() {
			super(UniqueId.forEngine("test"), "stub");
		}

		@Override
		public Type getType() {
			return Type.CONTAINER;
		}

		@Override
		public Set<TestTag> getTags() {
			return Set.of(TestTag.create("test-tag"));
		}

	}

	private static class ParentConfigurationParameters implements ConfigurationParameters {

		private final Map<String, String> map;

		ParentConfigurationParameters(String key, String value) {
			this.map = Map.of(key, value);
		}

		@Override
		public Optional<String> get(String key) {
			return Optional.ofNullable(map.get(key));
		}

		@Override
		public Optional<Boolean> getBoolean(String key) {
			return Optional.empty();
		}

		@Override
		public Set<String> keySet() {
			return Set.of();
		}

	}

}
