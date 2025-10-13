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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
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
		@Nullable
		private List<Path> selectedClasspathEntries;

		@Option(names = "--scan-modules", description = "Scan all resolved modules for test discovery.")
		private boolean scanModulepath;

		@Option(names = { "-u",
				"--select-uri" }, paramLabel = "URI", arity = "1..*", converter = SelectorConverter.Uri.class, description = "Select a URI for test discovery. This option can be repeated.")
		private final List<UriSelector> selectedUris = new ArrayList<>();

		@Option(names = { "-f",
				"--select-file" }, paramLabel = "FILE", arity = "1..*", converter = SelectorConverter.File.class, //
				description = "Select a file for test discovery. "
						+ "The line and column numbers can be provided as URI query parameters (e.g. foo.txt?line=12&column=34). "
						+ "This option can be repeated.")
		private final List<FileSelector> selectedFiles = new ArrayList<>();

		@Option(names = { "-d",
				"--select-directory" }, paramLabel = "DIR", arity = "1..*", converter = SelectorConverter.Directory.class, description = "Select a directory for test discovery. This option can be repeated.")
		private final List<DirectorySelector> selectedDirectories = new ArrayList<>();

		@Option(names = { "-o",
				"--select-module" }, paramLabel = "NAME", arity = "1..*", converter = SelectorConverter.Module.class, description = "Select single module for test discovery. This option can be repeated.")
		private final List<ModuleSelector> selectedModules = new ArrayList<>();

		@Option(names = { "-p",
				"--select-package" }, paramLabel = "PKG", arity = "1..*", converter = SelectorConverter.Package.class, description = "Select a package for test discovery. This option can be repeated.")
		private final List<PackageSelector> selectedPackages = new ArrayList<>();

		@Option(names = { "-c",
				"--select-class" }, paramLabel = "CLASS", arity = "1..*", converter = SelectorConverter.Class.class, description = "Select a class for test discovery. This option can be repeated.")
		private final List<ClassSelector> selectedClasses = new ArrayList<>();

		@Option(names = { "-m",
				"--select-method" }, paramLabel = "NAME", arity = "1..*", converter = SelectorConverter.Method.class, description = "Select a method for test discovery. This option can be repeated.")
		private final List<MethodSelector> selectedMethods = new ArrayList<>();

		@Option(names = { "-r",
				"--select-resource" }, paramLabel = "RESOURCE", arity = "1..*", converter = SelectorConverter.ClasspathResource.class, description = "Select a classpath resource for test discovery. This option can be repeated.")
		private final List<ClasspathResourceSelector> selectedClasspathResources = new ArrayList<>();

		@Option(names = { "-i",
				"--select-iteration" }, paramLabel = "PREFIX:VALUE[INDEX(..INDEX)?(,INDEX(..INDEX)?)*]", arity = "1..*", converter = SelectorConverter.Iteration.class, //
				description = "Select iterations for test discovery via a prefixed identifier and a list of indexes or index ranges "
						+ "(e.g. method:com.acme.Foo#m()[1..2] selects the first and second iteration of the m() method in the com.acme.Foo class). "
						+ "This option can be repeated.")
		private final List<IterationSelector> selectedIterations = new ArrayList<>();

		@Option(names = { "--select-unique-id",
				"--uid" }, paramLabel = "UNIQUE-ID", arity = "1..*", converter = SelectorConverter.UniqueId.class, //
				description = "Select a unique id for test discovery. This option can be repeated.")
		private final List<UniqueIdSelector> selectedUniqueIds = new ArrayList<>();

		@Option(names = "--select", paramLabel = "PREFIX:VALUE", arity = "1..*", converter = SelectorConverter.Identifier.class, //
				description = "Select via a prefixed identifier (e.g. method:com.acme.Foo#m selects the m() method in the com.acme.Foo class). "
						+ "This option can be repeated.")
		private final List<DiscoverySelectorIdentifier> selectorIdentifiers = new ArrayList<>();

		SelectorOptions() {
		}

		private void applyTo(TestDiscoveryOptions result) {
			result.setScanClasspath(this.selectedClasspathEntries != null); // flag was specified
			result.setScanModulepath(this.scanModulepath);
			result.setSelectedModules(this.selectedModules);
			result.setSelectedClasspathEntries(this.selectedClasspathEntries);
			result.setSelectedUris(this.selectedUris);
			result.setSelectedFiles(this.selectedFiles);
			result.setSelectedDirectories(this.selectedDirectories);
			result.setSelectedPackages(this.selectedPackages);
			result.setSelectedClasses(this.selectedClasses);
			result.setSelectedMethods(this.selectedMethods);
			result.setSelectedClasspathResources(this.selectedClasspathResources);
			result.setSelectedIterations(this.selectedIterations);
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
		private final List<String> includeClassNamePatterns = new ArrayList<>();

		@Option(names = { "-N",
				"--exclude-classname" }, paramLabel = "PATTERN", arity = "1", description = "Provide a regular expression to exclude those classes whose fully qualified names match. " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.")
		private final List<String> excludeClassNamePatterns = new ArrayList<>();

		@Option(names = {
				"--include-package" }, paramLabel = "PKG", arity = "1", description = "Provide a package to be included in the test run. This option can be repeated.")
		private final List<String> includePackages = new ArrayList<>();

		@Option(names = {
				"--exclude-package" }, paramLabel = "PKG", arity = "1", description = "Provide a package to be excluded from the test run. This option can be repeated.")
		private final List<String> excludePackages = new ArrayList<>();

		@Option(names = {
				"--include-methodname" }, paramLabel = "PATTERN", arity = "1", description = "Provide a regular expression to include only methods whose fully qualified names without parameters match. " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.")
		private final List<String> includeMethodNamePatterns = new ArrayList<>();

		@Option(names = {
				"--exclude-methodname" }, paramLabel = "PATTERN", arity = "1", description = "Provide a regular expression to exclude those methods whose fully qualified names without parameters match. " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.")
		private final List<String> excludeMethodNamePatterns = new ArrayList<>();

		@Option(names = { "-t",
				"--include-tag" }, paramLabel = "TAG", arity = "1", description = "Provide a tag or tag expression to include only tests whose tags match. "
						+ //
						"When this option is repeated, all patterns will be combined using OR semantics.")
		private final List<String> includedTags = new ArrayList<>();

		@Option(names = { "-T",
				"--exclude-tag" }, paramLabel = "TAG", arity = "1", description = "Provide a tag or tag expression to exclude those tests whose tags match. "
						+ //
						"When this option is repeated, all patterns will be combined using OR semantics.")
		private final List<String> excludedTags = new ArrayList<>();

		@Option(names = { "-e",
				"--include-engine" }, paramLabel = "ID", arity = "1", description = "Provide the ID of an engine to be included in the test run. This option can be repeated.")
		private final List<String> includedEngines = new ArrayList<>();

		@Option(names = { "-E",
				"--exclude-engine" }, paramLabel = "ID", arity = "1", description = "Provide the ID of an engine to be excluded from the test run. This option can be repeated.")
		private final List<String> excludedEngines = new ArrayList<>();

		private void applyTo(TestDiscoveryOptions result) {
			result.setIncludedClassNamePatterns(this.includeClassNamePatterns);
			result.setExcludedClassNamePatterns(this.excludeClassNamePatterns);
			result.setIncludedPackages(this.includePackages);
			result.setExcludedPackages(this.excludePackages);
			result.setIncludedMethodNamePatterns(new ArrayList<>(this.includeMethodNamePatterns));
			result.setExcludedMethodNamePatterns(new ArrayList<>(this.excludeMethodNamePatterns));
			result.setIncludedTagExpressions(this.includedTags);
			result.setExcludedTagExpressions(this.excludedTags);
			result.setIncludedEngines(this.includedEngines);
			result.setExcludedEngines(this.excludedEngines);
		}
	}

	public static class RuntimeConfigurationOptions {

		@Option(names = { "-" + CP_OPTION, "--classpath",
				"--class-path" }, converter = ClasspathEntriesConverter.class, paramLabel = "PATH", arity = "1", description = "Provide additional classpath entries "
						+ "-- for example, for adding engines and their dependencies. This option can be repeated.")
		private final List<Path> additionalClasspathEntries = new ArrayList<>();

		// Implementation note: the @Option annotation is on a setter method to allow validation.
		private final Map<String, String> configurationParameters = new LinkedHashMap<>();

		@Option(names = {
				"--config-resource" }, paramLabel = "PATH", arity = "1", description = "Set configuration parameters for test discovery and execution via a classpath resource. This option can be repeated.")
		private List<String> configurationParametersResources = new ArrayList<>();

		@CommandLine.Spec
		private CommandLine.Model.CommandSpec spec;

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
					"Duplicate key '%s' for values '%s' and '%s'.".formatted(key, existing, newValue));
			}
		}

		private void applyTo(TestDiscoveryOptions result) {
			result.setAdditionalClasspathEntries(additionalClasspathEntries);
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

}
