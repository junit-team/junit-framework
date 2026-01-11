/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.engine.discovery.ClassNameFilter.STANDARD_INCLUDE_PATTERN;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectDirectory;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectFile;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectIteration;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUri;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.console.options.TestDiscoveryOptions;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * @since 1.0
 */
class DiscoveryRequestCreatorTests {

	private final TestDiscoveryOptions options = new TestDiscoveryOptions();

	@Test
	void convertsScanClasspathOptionWithoutExplicitRootDirectories() {
		options.setScanClasspath(true);

		var request = convert();

		var classpathRootSelectors = request.getSelectorsByType(ClasspathRootSelector.class);
		// @formatter:off
		assertThat(classpathRootSelectors).extracting(ClasspathRootSelector::getClasspathRoot)
				.hasAtLeastOneElementOfType(URI.class)
				.doesNotContainNull();
		// @formatter:on
	}

	@Test
	void convertsScanClasspathOptionWithExplicitRootDirectories() {
		options.setScanClasspath(true);
		options.setSelectedClasspathEntries(List.of(Path.of("."), Path.of("..")));

		var request = convert();

		var classpathRootSelectors = request.getSelectorsByType(ClasspathRootSelector.class);
		// @formatter:off
		assertThat(classpathRootSelectors).extracting(ClasspathRootSelector::getClasspathRoot)
				.containsExactly(new File(".").toURI(), new File("..").toURI());
		// @formatter:on
	}

	@Test
	void convertsScanClasspathOptionWithAdditionalClasspathEntries() {
		options.setScanClasspath(true);
		options.setAdditionalClasspathEntries(List.of(Path.of("."), Path.of("..")));

		var request = convert();

		var classpathRootSelectors = request.getSelectorsByType(ClasspathRootSelector.class);
		// @formatter:off
		assertThat(classpathRootSelectors).extracting(ClasspathRootSelector::getClasspathRoot)
			.contains(new File(".").toURI(), new File("..").toURI());
		// @formatter:on
	}

	@Test
	void doesNotSupportScanClasspathAndExplicitSelectors() {
		options.setScanClasspath(true);
		options.setSelectedClasses(List.of(selectClass("SomeTest")));

		assertPreconditionViolationFor(this::convert).withMessageContaining("not supported");
	}

	@Test
	void convertsDefaultIncludeClassNamePatternOption() {
		options.setScanClasspath(true);

		var request = convert();

		var filters = request.getFiltersByType(ClassNameFilter.class);
		assertThat(filters).hasSize(1);
		assertExcludes(filters.getFirst(), STANDARD_INCLUDE_PATTERN);
	}

	@Test
	void convertsExplicitIncludeClassNamePatternOption() {
		options.setScanClasspath(true);
		options.setIncludedClassNamePatterns(List.of("Foo.*Bar", "Bar.*Foo"));

		var request = convert();

		var filters = request.getFiltersByType(ClassNameFilter.class);
		assertThat(filters).hasSize(1);
		assertIncludes(filters.getFirst(), "Foo.*Bar");
		assertIncludes(filters.getFirst(), "Bar.*Foo");
	}

	@Test
	void includeSelectedClassesAndMethodsRegardlessOfClassNamePatterns() {
		options.setSelectedClasses(List.of(selectClass("SomeTest")));
		options.setSelectedMethods(List.of(selectMethod("com.acme.Foo#m()")));
		options.setSelectedIterations(List.of(selectIteration(selectMethod("com.acme.Bar#n()"), 0)));
		options.setIncludedClassNamePatterns(List.of("Foo.*Bar"));

		var request = convert();

		var filters = request.getFiltersByType(ClassNameFilter.class);
		assertThat(filters).hasSize(1);
		assertIncludes(filters.getFirst(), "SomeTest");
		assertIncludes(filters.getFirst(), "com.acme.Foo");
		assertIncludes(filters.getFirst(), "com.acme.Bar");
		assertIncludes(filters.getFirst(), "Foo.*Bar");
	}

	@Test
	void convertsExcludeClassNamePatternOption() {
		options.setScanClasspath(true);
		options.setExcludedClassNamePatterns(List.of("Foo.*Bar", "Bar.*Foo"));

		var request = convert();

		var filters = request.getFiltersByType(ClassNameFilter.class);
		assertThat(filters).hasSize(2);
		assertExcludes(filters.get(1), "Foo.*Bar");
		assertExcludes(filters.get(1), "Bar.*Foo");
	}

	@Test
	void convertsPackageOptions() {
		options.setScanClasspath(true);
		options.setIncludedPackages(List.of("org.junit.included1", "org.junit.included2", "org.junit.included3"));
		options.setExcludedPackages(List.of("org.junit.excluded1"));

		var request = convert();
		var packageNameFilters = request.getFiltersByType(PackageNameFilter.class);

		assertThat(packageNameFilters).hasSize(2);
		assertIncludes(packageNameFilters.getFirst(), "org.junit.included1");
		assertIncludes(packageNameFilters.get(0), "org.junit.included2");
		assertIncludes(packageNameFilters.get(0), "org.junit.included3");
		assertExcludes(packageNameFilters.get(1), "org.junit.excluded1");
	}

	@Test
	void convertsMethodNamePatternOptions() {
		options.setScanClasspath(true);
		options.setIncludedMethodNamePatterns(List.of(".+#foo.*Bar", ".+#toString", ".+#method.*"));
		options.setExcludedMethodNamePatterns(List.of(".+#bar.*Foo"));
		var request = convert();
		var methodNameFilters = request.getPostDiscoveryFilters();
		assertThat(methodNameFilters).hasSize(2);
		assertThat(methodNameFilters.get(0).toString()).contains(".+#foo.*Bar", ".+#toString", ".+#method.*");
		assertThat(methodNameFilters.get(1).toString()).contains(".+#bar.*Foo");
	}

	@Test
	void convertsTagOptions() {
		options.setScanClasspath(true);
		options.setIncludedTagExpressions(List.of("fast", "medium", "slow"));
		options.setExcludedTagExpressions(List.of("slow"));

		var request = convert();
		var postDiscoveryFilters = request.getPostDiscoveryFilters();

		assertThat(postDiscoveryFilters).hasSize(2);
		assertThat(postDiscoveryFilters.get(0).toString()).contains("TagFilter");
		assertThat(postDiscoveryFilters.get(1).toString()).contains("TagFilter");
	}

	@Test
	void convertsEngineOptions() {
		options.setScanClasspath(true);
		options.setIncludedEngines(List.of("engine1", "engine2", "engine3"));
		options.setExcludedEngines(List.of("engine2"));

		var request = convert();
		var engineFilters = request.getEngineFilters();

		assertThat(engineFilters).hasSize(2);
		assertThat(engineFilters.get(0).toString()).contains("includes", "[engine1, engine2, engine3]");
		assertThat(engineFilters.get(1).toString()).contains("excludes", "[engine2]");
	}

	@Test
	void propagatesUniqueIdSelectors() {
		options.setSelectedUniqueId(List.of(selectUniqueId("[engine:a]/[1:1]"), selectUniqueId("[engine:b]/[2:2]")));

		var request = convert();
		var uriSelectors = request.getSelectorsByType(UniqueIdSelector.class);

		assertThat(uriSelectors) //
				.extracting(UniqueIdSelector::getUniqueId) //
				.containsExactly( //
					UniqueId.parse("[engine:a]/[1:1]"), //, //
					UniqueId.parse("[engine:b]/[2:2]") //
				);
	}

	@Test
	void propagatesUriSelectors() {
		options.setSelectedUris(List.of(selectUri("a"), selectUri("b")));

		var request = convert();
		var uriSelectors = request.getSelectorsByType(UriSelector.class);

		assertThat(uriSelectors).extracting(UriSelector::getUri).containsExactly(URI.create("a"), URI.create("b"));
	}

	@Test
	void propagatesFileSelectors() {
		options.setSelectedFiles(List.of(selectFile("foo.txt"), selectFile("bar.csv")));

		var request = convert();
		var fileSelectors = request.getSelectorsByType(FileSelector.class);

		assertThat(fileSelectors).extracting(FileSelector::getRawPath).containsExactly("foo.txt", "bar.csv");
	}

	@Test
	void propagatesDirectorySelectors() {
		options.setSelectedDirectories(List.of(selectDirectory("foo/bar"), selectDirectory("bar/qux")));

		var request = convert();
		var directorySelectors = request.getSelectorsByType(DirectorySelector.class);

		assertThat(directorySelectors).extracting(DirectorySelector::getRawPath).containsExactly("foo/bar", "bar/qux");
	}

	@Test
	void propagatesPackageSelectors() {
		options.setSelectedPackages(List.of(selectPackage("com.acme.foo"), selectPackage("com.example.bar")));

		var request = convert();
		var packageSelectors = request.getSelectorsByType(PackageSelector.class);

		assertThat(packageSelectors).extracting(PackageSelector::getPackageName).containsExactly("com.acme.foo",
			"com.example.bar");
	}

	@Test
	void propagatesClassSelectors() {
		options.setSelectedClasses(List.of(selectClass("com.acme.Foo"), selectClass("com.example.Bar")));

		var request = convert();
		var classSelectors = request.getSelectorsByType(ClassSelector.class);

		assertThat(classSelectors).extracting(ClassSelector::getClassName).containsExactly("com.acme.Foo",
			"com.example.Bar");
	}

	@Test
	void propagatesMethodSelectors() {
		options.setSelectedMethods(
			List.of(selectMethod("com.acme.Foo#m()"), selectMethod("com.example.Bar#method(java.lang.Object)")));

		var request = convert();
		var methodSelectors = request.getSelectorsByType(MethodSelector.class);

		assertThat(methodSelectors).hasSize(2);
		assertThat(methodSelectors.getFirst().getClassName()).isEqualTo("com.acme.Foo");
		assertThat(methodSelectors.get(0).getMethodName()).isEqualTo("m");
		assertThat(methodSelectors.get(0).getParameterTypeNames()).isEmpty();
		assertThat(methodSelectors.get(1).getClassName()).isEqualTo("com.example.Bar");
		assertThat(methodSelectors.get(1).getMethodName()).isEqualTo("method");
		assertThat(methodSelectors.get(1).getParameterTypeNames()).isEqualTo("java.lang.Object");
	}

	@Test
	void propagatesClasspathResourceSelectors() {
		options.setSelectedClasspathResources(
			List.of(selectClasspathResource("foo.csv"), selectClasspathResource("com/example/bar.json")));

		var request = convert();
		var classpathResourceSelectors = request.getSelectorsByType(ClasspathResourceSelector.class);

		assertThat(classpathResourceSelectors).extracting(
			ClasspathResourceSelector::getClasspathResourceName).containsExactly("foo.csv", "com/example/bar.json");
	}

	@Test
	void propagatesIterationSelectors() {
		var methodSelector = selectMethod("com.acme.Foo#m()");
		var classSelector = selectClass("com.example.Bar");
		options.setSelectedIterations(List.of(selectIteration(methodSelector, 1), selectIteration(classSelector, 2)));

		var request = convert();
		var iterationSelectors = request.getSelectorsByType(IterationSelector.class);

		assertThat(iterationSelectors).hasSize(2);
		assertThat(iterationSelectors.get(0).getParentSelector()).isEqualTo(methodSelector);
		assertThat(iterationSelectors.get(0).getIterationIndices()).containsExactly(1);
		assertThat(iterationSelectors.get(1).getParentSelector()).isEqualTo(classSelector);
		assertThat(iterationSelectors.get(1).getIterationIndices()).containsExactly(2);
	}

	@Test
	void propagatesSelectorIdentifiers() {
		var methodSelector = selectMethod("com.acme.Foo#m()");
		var classSelector = selectClass("com.example.Bar");
		options.setSelectorIdentifiers(
			List.of(methodSelector.toIdentifier().orElseThrow(), classSelector.toIdentifier().orElseThrow()));

		var request = convert();

		assertThat(request.getSelectorsByType(MethodSelector.class)).containsExactly(methodSelector);
		assertThat(request.getSelectorsByType(ClassSelector.class)).containsExactly(classSelector);
	}

	@Test
	void convertsConfigurationParameters() {
		options.setScanClasspath(true);
		options.setConfigurationParameters(mapOf(entry("foo", "bar"), entry("baz", "true")));

		var request = convert();
		var configurationParameters = request.getConfigurationParameters();

		assertThat(configurationParameters.get("foo")).contains("bar");
		assertThat(configurationParameters.getBoolean("baz")).contains(true);
	}

	@Test
	void convertsConfigurationParametersResources() {
		options.setScanClasspath(true);
		options.setConfigurationParameters(mapOf(entry("foo", "bar"), entry("com.example.prop.first", "baz")));
		options.setConfigurationParametersResources(List.of("config-test.properties"));

		var request = convert();
		var configurationParameters = request.getConfigurationParameters();

		assertThat(configurationParameters.get("foo")).contains("bar");
		assertThat(configurationParameters.get("com.example.prop.first")).contains("baz");
		assertThat(configurationParameters.get("com.example.prop.second")).contains("second value");
	}

	@Test
	void logsInvalidSearchPathRoots(@TrackLogRecords LogRecordListener listener) {
		var opts = new TestDiscoveryOptions();
		opts.setScanClasspath(true);
		var missingPath = Path.of("/does/not/exist");
		opts.setSelectedClasspathEntries(List.of(missingPath));

		DiscoveryRequestCreator.toDiscoveryRequestBuilder(opts);

		assertThat(listener.stream(DiscoveryRequestCreator.class)) //
				.map(LogRecord::getMessage) //
				.filteredOn(message -> message.contains(missingPath.toString())) //
				.hasSize(1);
	}

	@Test
	void logsInvalidAdditionalClasspathRoots(@TrackLogRecords LogRecordListener listener) {
		var opts = new TestDiscoveryOptions();
		opts.setScanClasspath(true);
		var missingPath = Path.of("/also/does/not/exist");
		opts.setAdditionalClasspathEntries(List.of(missingPath));

		DiscoveryRequestCreator.toDiscoveryRequestBuilder(opts);

		assertThat(listener.stream(DiscoveryRequestCreator.class)) //
				.map(LogRecord::getMessage) //
				.filteredOn(message -> message.contains(missingPath.toString())) //
				.hasSize(1);
	}

	private LauncherDiscoveryRequest convert() {
		return DiscoveryRequestCreator.toDiscoveryRequestBuilder(options).build();
	}

	private void assertIncludes(Filter<String> filter, String included) {
		assertThat(filter.apply(included).included()).isTrue();
	}

	private void assertExcludes(Filter<String> filter, String excluded) {
		assertThat(filter.apply(excluded).excluded()).isTrue();
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private static <K, V> Map<K, V> mapOf(Entry<K, V>... entries) {
		return Stream.of(entries).collect(toMap(Entry::getKey, Entry::getValue));
	}

}
