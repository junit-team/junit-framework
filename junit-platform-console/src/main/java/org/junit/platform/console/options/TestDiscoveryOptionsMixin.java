/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.options;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apiguardian.api.API;
import org.junit.platform.engine.DiscoverySelectorIdentifier;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

@API(status = INTERNAL, since = "1.14")
public class TestDiscoveryOptionsMixin {

	private static final String CP_OPTION = "cp";

	@ArgGroup(validate = false, order = 2, heading = "%n@|bold SELECTORS|@%n%n")
	SelectorOptions selectorOptions;

	@ArgGroup(validate = false, order = 3, heading = "%n  For more information on selectors including syntax examples, see"
			+ "%n  @|underline https://docs.junit.org/${junit.docs.version}/user-guide/#running-tests-discovery-selectors|@"
			+ "%n%n@|bold FILTERS|@%n%n")
	FilterOptions filterOptions;

	@ArgGroup(validate = false, order = 4, heading = "%n@|bold RUNTIME CONFIGURATION|@%n%n")
	RuntimeConfigurationOptions runtimeConfigurationOptions;

	public static class SelectorOptions {

		@Option(names = { "--scan-classpath",
				"--scan-class-path" }, converter = ClasspathEntriesConverter.class, paramLabel = "PATH", arity = "0..1", description = "Scan all directories on the classpath or explicit classpath roots. " //
						+ "Without arguments, only directories on the system classpath as well as additional classpath " //
						+ "entries supplied via -" + CP_OPTION + " (directories and JAR files) are scanned. " //
						+ "Explicit classpath roots that are not on the classpath will be silently ignored. " //
						+ "This option can be repeated.")
		private List<Path> selectedClasspathEntries;

		@Option(names = { "-scan-class-path",
				"-scan-classpath" }, converter = ClasspathEntriesConverter.class, arity = "0..1", hidden = true)
		private List<Path> selectedClasspathEntries2;

		@Option(names = "--scan-modules", description = "Scan all resolved modules for test discovery.")
		private boolean scanModulepath;

		@Option(names = "-scan-modules", hidden = true)
		private boolean scanModulepath2;

		@Option(names = { "-u",
				"--select-uri" }, paramLabel = "URI", arity = "1..*", converter = SelectorConverter.Uri.class, description = "Select a URI for test discovery. This option can be repeated.")
		private List<UriSelector> selectedUris = new ArrayList<>();

		@Option(names = { "--u",
				"-select-uri" }, arity = "1..*", hidden = true, converter = SelectorConverter.Uri.class)
		private List<UriSelector> selectedUris2 = new ArrayList<>();

		@Option(names = { "-f",
				"--select-file" }, paramLabel = "FILE", arity = "1..*", converter = SelectorConverter.File.class, //
				description = "Select a file for test discovery. "
						+ "The line and column numbers can be provided as URI query parameters (e.g. foo.txt?line=12&column=34). "
						+ "This option can be repeated.")
		private List<FileSelector> selectedFiles = new ArrayList<>();

		@Option(names = { "--f",
				"-select-file" }, arity = "1..*", hidden = true, converter = SelectorConverter.File.class)
		private List<FileSelector> selectedFiles2 = new ArrayList<>();

		@Option(names = { "-d",
				"--select-directory" }, paramLabel = "DIR", arity = "1..*", converter = SelectorConverter.Directory.class, description = "Select a directory for test discovery. This option can be repeated.")
		private List<DirectorySelector> selectedDirectories = new ArrayList<>();

		@Option(names = { "--d",
				"-select-directory" }, arity = "1..*", hidden = true, converter = SelectorConverter.Directory.class)
		private List<DirectorySelector> selectedDirectories2 = new ArrayList<>();

		@Option(names = { "-o",
				"--select-module" }, paramLabel = "NAME", arity = "1..*", converter = SelectorConverter.Module.class, description = "Select single module for test discovery. This option can be repeated.")
		private List<ModuleSelector> selectedModules = new ArrayList<>();

		@Option(names = { "--o",
				"-select-module" }, arity = "1..*", converter = SelectorConverter.Module.class, hidden = true)
		private List<ModuleSelector> selectedModules2 = new ArrayList<>();

		@Option(names = { "-p",
				"--select-package" }, paramLabel = "PKG", arity = "1..*", converter = SelectorConverter.Package.class, description = "Select a package for test discovery. This option can be repeated.")
		private List<PackageSelector> selectedPackages = new ArrayList<>();

		@Option(names = { "--p",
				"-select-package" }, arity = "1..*", hidden = true, converter = SelectorConverter.Package.class)
		private List<PackageSelector> selectedPackages2 = new ArrayList<>();

		@Option(names = { "-c",
				"--select-class" }, paramLabel = "CLASS", arity = "1..*", converter = SelectorConverter.Class.class, description = "Select a class for test discovery. This option can be repeated.")
		private List<ClassSelector> selectedClasses = new ArrayList<>();

		@Option(names = { "--c",
				"-select-class" }, arity = "1..*", hidden = true, converter = SelectorConverter.Class.class)
		private List<ClassSelector> selectedClasses2 = new ArrayList<>();

		@Option(names = { "-m",
				"--select-method" }, paramLabel = "NAME", arity = "1..*", converter = SelectorConverter.Method.class, description = "Select a method for test discovery. This option can be repeated.")
		private List<MethodSelector> selectedMethods = new ArrayList<>();

		@Option(names = { "--m",
				"-select-method" }, arity = "1..*", hidden = true, converter = SelectorConverter.Method.class)
		private List<MethodSelector> selectedMethods2 = new ArrayList<>();

		@Option(names = { "-r",
				"--select-resource" }, paramLabel = "RESOURCE", arity = "1..*", converter = SelectorConverter.ClasspathResource.class, description = "Select a classpath resource for test discovery. This option can be repeated.")
		private List<ClasspathResourceSelector> selectedClasspathResources = new ArrayList<>();

		@Option(names = { "--r",
				"-select-resource" }, arity = "1..*", hidden = true, converter = SelectorConverter.ClasspathResource.class)
		private List<ClasspathResourceSelector> selectedClasspathResources2 = new ArrayList<>();

		@Option(names = { "-i",
				"--select-iteration" }, paramLabel = "PREFIX:VALUE[INDEX(..INDEX)?(,INDEX(..INDEX)?)*]", arity = "1..*", converter = SelectorConverter.Iteration.class, //
				description = "Select iterations for test discovery via a prefixed identifier and a list of indexes or index ranges "
						+ "(e.g. method:com.acme.Foo#m()[1..2] selects the first and second iteration of the m() method in the com.acme.Foo class). "
						+ "This option can be repeated.")
		private List<IterationSelector> selectedIterations = new ArrayList<>();

		@Option(names = { "--i",
				"-select-iteration" }, arity = "1..*", hidden = true, converter = SelectorConverter.Iteration.class)
		private List<IterationSelector> selectedIterations2 = new ArrayList<>();

		@Option(names = { "--select-unique-id",
				"--uid" }, paramLabel = "UNIQUE-ID", arity = "1..*", converter = SelectorConverter.UniqueId.class, //
				description = "Select a unique id for test discovery. This option can be repeated.")
		private List<UniqueIdSelector> selectedUniqueIds = new ArrayList<>();

		@Option(names = "--select", paramLabel = "PREFIX:VALUE", arity = "1..*", converter = SelectorConverter.Identifier.class, //
				description = "Select via a prefixed identifier (e.g. method:com.acme.Foo#m selects the m() method in the com.acme.Foo class). "
						+ "This option can be repeated.")
		private List<DiscoverySelectorIdentifier> selectorIdentifiers = new ArrayList<>();

		SelectorOptions() {
		}

		private void applyTo(TestDiscoveryOptions result) {
			result.setScanClasspath(this.selectedClasspathEntries != null || this.selectedClasspathEntries2 != null); // flag was specified
			result.setScanModulepath(this.scanModulepath || this.scanModulepath2);
			result.setSelectedModules(merge(this.selectedModules, this.selectedModules2));
			result.setSelectedClasspathEntries(merge(this.selectedClasspathEntries, this.selectedClasspathEntries2));
			result.setSelectedUris(merge(this.selectedUris, this.selectedUris2));
			result.setSelectedFiles(merge(this.selectedFiles, this.selectedFiles2));
			result.setSelectedDirectories(merge(this.selectedDirectories, this.selectedDirectories2));
			result.setSelectedPackages(merge(this.selectedPackages, this.selectedPackages2));
			result.setSelectedClasses(merge(this.selectedClasses, this.selectedClasses2));
			result.setSelectedMethods(merge(this.selectedMethods, this.selectedMethods2));
			result.setSelectedClasspathResources(
				merge(this.selectedClasspathResources, this.selectedClasspathResources2));
			result.setSelectedIterations(merge(this.selectedIterations, this.selectedIterations2));
			result.setSelectedUniqueId(this.selectedUniqueIds);
			result.setSelectorIdentifiers(this.selectorIdentifiers);
		}
	}

	public static class FilterOptions {

		@Option(names = { "-n",
				"--include-classname" }, paramLabel = "PATTERN", defaultValue = ClassNameFilter.STANDARD_INCLUDE_PATTERN, arity = "1", description = "Provide a regular expression to include only classes whose fully qualified names match. " //
						+ "To avoid loading classes unnecessarily, the default pattern only includes class " //
						+ "names that begin with \"Test\" or end with \"Test\" or \"Tests\". " //
						+ "When this option is repeated, all patterns will be combined using OR semantics. " //
						+ "Default: ${DEFAULT-VALUE}")
		private List<String> includeClassNamePatterns = new ArrayList<>();

		@Option(names = { "--n", "-include-classname" }, arity = "1", hidden = true)
		private List<String> includeClassNamePatterns2 = new ArrayList<>();

		@Option(names = { "-N",
				"--exclude-classname" }, paramLabel = "PATTERN", arity = "1", description = "Provide a regular expression to exclude those classes whose fully qualified names match. " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.")
		private List<String> excludeClassNamePatterns = new ArrayList<>();

		@Option(names = { "--N", "-exclude-classname" }, arity = "1", hidden = true)
		private List<String> excludeClassNamePatterns2 = new ArrayList<>();

		@Option(names = {
				"--include-package" }, paramLabel = "PKG", arity = "1", description = "Provide a package to be included in the test run. This option can be repeated.")
		private List<String> includePackages = new ArrayList<>();

		@Option(names = { "-include-package" }, arity = "1", hidden = true)
		private List<String> includePackages2 = new ArrayList<>();

		@Option(names = {
				"--exclude-package" }, paramLabel = "PKG", arity = "1", description = "Provide a package to be excluded from the test run. This option can be repeated.")
		private List<String> excludePackages = new ArrayList<>();

		@Option(names = { "-exclude-package" }, arity = "1", hidden = true)
		private List<String> excludePackages2 = new ArrayList<>();

		@Option(names = {
				"--include-methodname" }, paramLabel = "PATTERN", arity = "1", description = "Provide a regular expression to include only methods whose fully qualified names without parameters match. " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.")
		private List<String> includeMethodNamePatterns = new ArrayList<>();

		@Option(names = {
				"--exclude-methodname" }, paramLabel = "PATTERN", arity = "1", description = "Provide a regular expression to exclude those methods whose fully qualified names without parameters match. " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.")
		private List<String> excludeMethodNamePatterns = new ArrayList<>();

		@Option(names = { "-t",
				"--include-tag" }, paramLabel = "TAG", arity = "1", description = "Provide a tag or tag expression to include only tests whose tags match. "
						+ //
						"When this option is repeated, all patterns will be combined using OR semantics.")
		private List<String> includedTags = new ArrayList<>();

		@Option(names = { "--t", "-include-tag" }, arity = "1", hidden = true)
		private List<String> includedTags2 = new ArrayList<>();

		@Option(names = { "-T",
				"--exclude-tag" }, paramLabel = "TAG", arity = "1", description = "Provide a tag or tag expression to exclude those tests whose tags match. "
						+ //
						"When this option is repeated, all patterns will be combined using OR semantics.")
		private List<String> excludedTags = new ArrayList<>();

		@Option(names = { "--T", "-exclude-tag" }, arity = "1", hidden = true)
		private List<String> excludedTags2 = new ArrayList<>();

		@Option(names = { "-e",
				"--include-engine" }, paramLabel = "ID", arity = "1", description = "Provide the ID of an engine to be included in the test run. This option can be repeated.")
		private List<String> includedEngines = new ArrayList<>();

		@Option(names = { "--e", "-include-engine" }, arity = "1", hidden = true)
		private List<String> includedEngines2 = new ArrayList<>();

		@Option(names = { "-E",
				"--exclude-engine" }, paramLabel = "ID", arity = "1", description = "Provide the ID of an engine to be excluded from the test run. This option can be repeated.")
		private List<String> excludedEngines = new ArrayList<>();

		@Option(names = { "--E", "-exclude-engine" }, arity = "1", hidden = true)
		private List<String> excludedEngines2 = new ArrayList<>();

		private void applyTo(TestDiscoveryOptions result) {
			result.setIncludedClassNamePatterns(merge(this.includeClassNamePatterns, this.includeClassNamePatterns2));
			result.setExcludedClassNamePatterns(merge(this.excludeClassNamePatterns, this.excludeClassNamePatterns2));
			result.setIncludedPackages(merge(this.includePackages, this.includePackages2));
			result.setExcludedPackages(merge(this.excludePackages, this.excludePackages2));
			result.setIncludedMethodNamePatterns(new ArrayList<>(this.includeMethodNamePatterns));
			result.setExcludedMethodNamePatterns(new ArrayList<>(this.excludeMethodNamePatterns));
			result.setIncludedTagExpressions(merge(this.includedTags, this.includedTags2));
			result.setExcludedTagExpressions(merge(this.excludedTags, this.excludedTags2));
			result.setIncludedEngines(merge(this.includedEngines, this.includedEngines2));
			result.setExcludedEngines(merge(this.excludedEngines, this.excludedEngines2));
		}
	}

	public static class RuntimeConfigurationOptions {

		@Option(names = { "-" + CP_OPTION, "--classpath",
				"--class-path" }, converter = ClasspathEntriesConverter.class, paramLabel = "PATH", arity = "1", description = "Provide additional classpath entries "
						+ "-- for example, for adding engines and their dependencies. This option can be repeated.")
		private List<Path> additionalClasspathEntries = new ArrayList<>();

		@Option(names = { "--cp", "-classpath",
				"-class-path" }, converter = ClasspathEntriesConverter.class, hidden = true)
		private List<Path> additionalClasspathEntries2 = new ArrayList<>();

		// Implementation note: the @Option annotation is on a setter method to allow validation.
		private Map<String, String> configurationParameters = new LinkedHashMap<>();

		@Option(names = {
				"--config-resource" }, paramLabel = "PATH", arity = "1", description = "Set configuration parameters for test discovery and execution via a classpath resource. This option can be repeated.")
		private List<String> configurationParametersResources = new ArrayList<>();

		@CommandLine.Spec
		private CommandLine.Model.CommandSpec spec;

		@Option(names = { "-config" }, arity = "1", hidden = true)
		public void setConfigurationParameters2(Map<String, String> keyValuePairs) {
			setConfigurationParameters(keyValuePairs);
		}

		/**
		 * Adds the specified key-value pair (or pairs) to the configuration parameters.
		 * A {@code ParameterException} is thrown if the same key is specified multiple times
		 * on the command line.
		 *
		 * @param map the key-value pairs to add
		 * @throws CommandLine.ParameterException if the map already contains this key
		 * @see <a href="https://github.com/junit-team/junit-framework/issues/1308">#1308</a>
		 */
		@Option(names = "--config", paramLabel = "KEY=VALUE", arity = "1", description = "Set a configuration parameter for test discovery and execution. This option can be repeated.")
		public void setConfigurationParameters(Map<String, String> map) {
			for (String key : map.keySet()) {
				String newValue = map.get(key);
				validateUnique(key, newValue);
				configurationParameters.put(key, newValue);
			}
		}

		private void validateUnique(String key, String newValue) {
			String existing = configurationParameters.get(key);
			if (existing != null && !existing.equals(newValue)) {
				throw new CommandLine.ParameterException(spec.commandLine(),
					String.format("Duplicate key '%s' for values '%s' and '%s'.", key, existing, newValue));
			}
		}

		private void applyTo(TestDiscoveryOptions result) {
			result.setAdditionalClasspathEntries(merge(additionalClasspathEntries, additionalClasspathEntries2));
			result.setConfigurationParametersResources(configurationParametersResources);
			result.setConfigurationParameters(configurationParameters);
		}
	}

	public TestDiscoveryOptions toTestDiscoveryOptions() {
		TestDiscoveryOptions result = new TestDiscoveryOptions();
		if (this.selectorOptions != null) {
			this.selectorOptions.applyTo(result);
		}
		if (this.filterOptions != null) {
			this.filterOptions.applyTo(result);
		}
		if (this.runtimeConfigurationOptions != null) {
			this.runtimeConfigurationOptions.applyTo(result);
		}
		return result;
	}

	private static <T> List<T> merge(List<T> list1, List<T> list2) {
		List<T> result = new ArrayList<>(list1 == null ? Collections.emptyList() : list1);
		result.addAll(list2 == null ? Collections.emptyList() : list2);
		return result;
	}

}
