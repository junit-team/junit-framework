/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/**
 * Unit tests for {@link LauncherConfigurationParameters}.
 *
 * @since 1.0
 */
class LauncherConfigurationParametersTests {

	private static final String CONFIG_FILE_NAME = "test-junit-platform.properties";
	private static final String KEY = LauncherConfigurationParametersTests.class.getName();
	private static final String INHERITED_PARAM = "parent config param";
	private static final String CONFIG_PARAM = "explicit config param";
	private static final String CONFIG_FILE = "from config file";
	private static final String SYSTEM_PROPERTY = "system property";

	@BeforeEach
	@AfterEach
	void reset() {
		System.clearProperty(KEY);
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void constructorPreconditions() {
		assertPreconditionViolationFor(() -> fromMap(null));
		assertPreconditionViolationFor(() -> fromMapAndFile(Map.of(), null));
		assertPreconditionViolationFor(() -> fromMapAndFile(Map.of(), ""));
		assertPreconditionViolationFor(() -> fromMapAndFile(Map.of(), "  "));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getPreconditions() {
		ConfigurationParameters configParams = fromMap(Map.of());
		assertPreconditionViolationFor(() -> configParams.get(null));
		assertPreconditionViolationFor(() -> configParams.get(""));
		assertPreconditionViolationFor(() -> configParams.get("  "));
	}

	@Test
	void noConfigParams() {
		ConfigurationParameters configParams = fromMap(Map.of());
		assertThat(configParams.get(KEY)).isEmpty();
		assertThat(configParams.keySet()).doesNotContain(KEY);
		assertThat(configParams.toString()).doesNotContain(KEY);
	}

	@Test
	void explicitConfigParam() {
		ConfigurationParameters configParams = fromMap(Map.of(KEY, CONFIG_PARAM));
		assertThat(configParams.get(KEY)).contains(CONFIG_PARAM);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(CONFIG_PARAM);
	}

	@Test
	void systemProperty() {
		System.setProperty(KEY, SYSTEM_PROPERTY);
		ConfigurationParameters configParams = fromMap(Map.of());
		assertThat(configParams.get(KEY)).contains(SYSTEM_PROPERTY);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).doesNotContain(KEY);
	}

	@Test
	void configFile() {
		ConfigurationParameters configParams = fromMapAndFile(Map.of(), CONFIG_FILE_NAME);
		assertThat(configParams.get(KEY)).contains(CONFIG_FILE);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(CONFIG_FILE);
	}

	@Test
	void inherited() {
		ConfigurationParameters configParams = fromMapAndParent( //
			Map.of(), //
			Map.of(KEY, INHERITED_PARAM));
		assertThat(configParams.get(KEY)).contains(INHERITED_PARAM);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(KEY);
	}

	@Test
	void explicitConfigParamOverridesSystemProperty() {
		System.setProperty(KEY, SYSTEM_PROPERTY);
		ConfigurationParameters configParams = fromMap(Map.of(KEY, CONFIG_PARAM));
		assertThat(configParams.get(KEY)).contains(CONFIG_PARAM);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(CONFIG_PARAM);
	}

	@Test
	void explicitConfigParamOverridesConfigFile() {
		ConfigurationParameters configParams = fromMapAndFile(Map.of(KEY, CONFIG_PARAM), CONFIG_FILE_NAME);
		assertThat(configParams.get(KEY)).contains(CONFIG_PARAM);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(CONFIG_PARAM);
	}

	@Test
	void explicitConfigParamOverridesInheritedProperty() {
		System.setProperty(KEY, SYSTEM_PROPERTY);
		ConfigurationParameters configParams = fromMapAndParent( //
			Map.of(KEY, CONFIG_PARAM), //
			Map.of(KEY, INHERITED_PARAM));
		assertThat(configParams.get(KEY)).contains(CONFIG_PARAM);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(CONFIG_PARAM);
	}

	@Test
	void systemPropertyOverridesConfigFile() {
		System.setProperty(KEY, SYSTEM_PROPERTY);
		ConfigurationParameters configParams = fromMapAndFile(Map.of(), CONFIG_FILE_NAME);
		assertThat(configParams.get(KEY)).contains(SYSTEM_PROPERTY);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(CONFIG_FILE);
	}

	@Test
	void inheritedPropertyOverridesSystemProperty() {
		System.setProperty(KEY, SYSTEM_PROPERTY);
		ConfigurationParameters configParams = fromMapAndParent(Map.of(), Map.of(KEY, INHERITED_PARAM));
		assertThat(configParams.get(KEY)).contains(INHERITED_PARAM);
		assertThat(configParams.keySet()).contains(KEY);
		assertThat(configParams.toString()).contains(KEY);
	}

	@Test
	void getValueInExtensionContext() {
		var summary = new SummaryGeneratingListener();
		var request = LauncherDiscoveryRequestBuilder.request() //
				.configurationParameter("thing", "one else!") //
				.selectors(DiscoverySelectors.selectClass(Something.class)) //
				.forExecution() //
				.listeners(summary) //
				.build();
		LauncherFactory.create().execute(request);
		assertEquals(0, summary.getSummary().getTestsFailedCount());
	}

	@Test
	void getWithSuccessfulTransformer() {
		ConfigurationParameters configParams = fromMap(Map.of(KEY, "42"));
		assertThat(configParams.get(KEY, Integer::valueOf)).contains(42);
	}

	@Test
	void getWithErroneousTransformer() {
		ConfigurationParameters configParams = fromMap(Map.of(KEY, "42"));
		var exception = assertThrows(JUnitException.class, () -> configParams.get(KEY, input -> {
			throw new RuntimeException("foo");
		}));
		assertThat(exception).hasMessageContaining(
			"Failed to transform configuration parameter with key '" + KEY + "' and initial value '42'");
	}

	@Test
	void ignoresSystemPropertyAndConfigFileWhenImplicitLookupsAreDisabled() {
		System.setProperty(KEY, SYSTEM_PROPERTY);
		ConfigurationParameters configParams = LauncherConfigurationParameters.builder() //
				.enableImplicitProviders(false) //
				.build();
		assertThat(configParams.get(KEY)).isEmpty();
	}

	@Test
	void warnsOnMultiplePropertyResources(@TempDir Path tempDir, @TrackLogRecords LogRecordListener logRecordListener)
			throws Exception {

		Properties properties = new Properties();
		properties.setProperty(KEY, "from second config file");
		try (var out = Files.newOutputStream(tempDir.resolve(CONFIG_FILE_NAME))) {
			properties.store(out, "");
		}

		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		URL originalResource = originalClassLoader.getResource(CONFIG_FILE_NAME);

		try (var customClassLoader = new URLClassLoader(new URL[] { tempDir.toUri().toURL() }, originalClassLoader)) {
			Thread.currentThread().setContextClassLoader(customClassLoader);
			ConfigurationParameters configParams = fromMapAndFile(Map.of(), CONFIG_FILE_NAME);

			assertThat(configParams.get(KEY)).contains(CONFIG_FILE);

			assertThat(logRecordListener.stream(Level.WARNING).map(LogRecord::getMessage)) //
					.hasSize(1) //
					.first(as(InstanceOfAssertFactories.STRING)) //
					.contains("""
							Discovered 2 '%s' configuration files on the classpath (see below); \
							only the first (*) will be used.
							- %s (*)
							- %s"""//
							.formatted(CONFIG_FILE_NAME, originalResource,
								tempDir.resolve(CONFIG_FILE_NAME).toUri().toURL()));
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private static LauncherConfigurationParameters fromMap(Map<String, String> map) {
		return LauncherConfigurationParameters.builder().explicitParameters(map).build();
	}

	private static LauncherConfigurationParameters fromMapAndFile(Map<String, String> map, String configFileName) {
		return LauncherConfigurationParameters.builder() //
				.explicitParameters(map) //
				.configFileName(configFileName) //
				.build();
	}

	private static LauncherConfigurationParameters fromMapAndParent(Map<String, String> map,
			Map<String, String> inherited) {
		var parameters = LauncherConfigurationParameters.builder() //
				.explicitParameters(inherited) //
				.build();

		return LauncherConfigurationParameters.builder() //
				.explicitParameters(map) //
				.parentConfigurationParameters(parameters) //
				.build();
	}

	private static class Mutator implements TestInstancePostProcessor {
		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
			var value = context.getConfigurationParameter("thing").orElse("thing");
			Something.class.getField("thing").set(testInstance, value);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(Mutator.class)
	static class Something {

		// `public` is needed for simple "Class#getField(String)" to work
		public String thing = "body.";

		@Test
		void some() {
			assertEquals("Someone else!", "Some" + thing);
		}
	}

}
