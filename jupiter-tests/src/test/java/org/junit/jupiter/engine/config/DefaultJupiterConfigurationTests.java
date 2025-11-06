/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.jupiter.api.io.CleanupMode.ALWAYS;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.engine.Constants;
import org.junit.jupiter.engine.descriptor.CustomDisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.hierarchical.ParallelHierarchicalTestExecutorServiceFactory.ParallelExecutorServiceType;
import org.junit.platform.launcher.core.ConfigurationParametersFactoryForTests;

class DefaultJupiterConfigurationTests {

	private static final String KEY = DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getDefaultTestInstanceLifecyclePreconditions() {
		assertPreconditionViolationNotNullFor("ConfigurationParameters",
			() -> new DefaultJupiterConfiguration(null, dummyOutputDirectoryCreator(), mock()));
	}

	@Test
	void getDefaultTestInstanceLifecycleWithNoConfigParamSet() {
		JupiterConfiguration configuration = new DefaultJupiterConfiguration(configurationParameters(Map.of()),
			dummyOutputDirectoryCreator(), mock());
		Lifecycle lifecycle = configuration.getDefaultTestInstanceLifecycle();
		assertThat(lifecycle).isEqualTo(PER_METHOD);
	}

	@Test
	void getDefaultTempDirCleanupModeWithNoConfigParamSet() {
		JupiterConfiguration configuration = new DefaultJupiterConfiguration(configurationParameters(Map.of()),
			dummyOutputDirectoryCreator(), mock());
		CleanupMode cleanupMode = configuration.getDefaultTempDirCleanupMode();
		assertThat(cleanupMode).isEqualTo(ALWAYS);
	}

	@Test
	void getDefaultTestInstanceLifecycleWithConfigParamSet() {
		assertAll(//
			() -> assertDefaultConfigParam(null, PER_METHOD), //
			() -> assertThatThrownBy(() -> getDefaultTestInstanceLifecycleConfigParam("")) //
					.hasMessage("Invalid test instance lifecycle mode '' set via the '%s' configuration parameter.",
						DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME), //
			() -> assertThatThrownBy(() -> getDefaultTestInstanceLifecycleConfigParam("bogus")) //
					.hasMessage(
						"Invalid test instance lifecycle mode 'BOGUS' set via the '%s' configuration parameter.",
						DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME), //
			() -> assertDefaultConfigParam(PER_METHOD.name(), PER_METHOD), //
			() -> assertDefaultConfigParam(PER_METHOD.name().toLowerCase(), PER_METHOD), //
			() -> assertDefaultConfigParam("  " + PER_METHOD.name() + "  ", PER_METHOD), //
			() -> assertDefaultConfigParam(PER_CLASS.name(), PER_CLASS), //
			() -> assertDefaultConfigParam(PER_CLASS.name().toLowerCase(), PER_CLASS), //
			() -> assertDefaultConfigParam("  " + PER_CLASS.name() + "  ", Lifecycle.PER_CLASS) //
		);
	}

	@Test
	void shouldGetDefaultDisplayNameGeneratorWithConfigParamSet() {
		var parameters = configurationParameters(
			Map.of(Constants.DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME, CustomDisplayNameGenerator.class.getName()));

		JupiterConfiguration configuration = new DefaultJupiterConfiguration(parameters, dummyOutputDirectoryCreator(),
			mock());

		DisplayNameGenerator defaultDisplayNameGenerator = configuration.getDefaultDisplayNameGenerator();

		assertThat(defaultDisplayNameGenerator).isInstanceOf(CustomDisplayNameGenerator.class);
	}

	@Test
	void shouldGetStandardAsDefaultDisplayNameGeneratorWithoutConfigParamSet() {
		JupiterConfiguration configuration = new DefaultJupiterConfiguration(configurationParameters(Map.of()),
			dummyOutputDirectoryCreator(), mock());

		DisplayNameGenerator defaultDisplayNameGenerator = configuration.getDefaultDisplayNameGenerator();

		assertThat(defaultDisplayNameGenerator).isInstanceOf(DisplayNameGenerator.Standard.class);
	}

	@Test
	void shouldGetNothingAsDefaultTestMethodOrderWithoutConfigParamSet() {
		JupiterConfiguration configuration = new DefaultJupiterConfiguration(configurationParameters(Map.of()),
			dummyOutputDirectoryCreator(), mock());

		final Optional<MethodOrderer> defaultTestMethodOrder = configuration.getDefaultTestMethodOrderer();

		assertThat(defaultTestMethodOrder).isEmpty();
	}

	@Test
	void shouldGetDefaultTempDirFactorySupplierWithConfigParamSet() {
		var parameters = configurationParameters(
			Map.of(Constants.DEFAULT_TEMP_DIR_FACTORY_PROPERTY_NAME, CustomFactory.class.getName()));
		JupiterConfiguration configuration = new DefaultJupiterConfiguration(parameters, dummyOutputDirectoryCreator(),
			mock());

		Supplier<TempDirFactory> supplier = configuration.getDefaultTempDirFactorySupplier();

		assertThat(supplier.get()).isInstanceOf(CustomFactory.class);
	}

	@Test
	void shouldGetStandardAsDefaultTempDirFactorySupplierWithoutConfigParamSet() {
		JupiterConfiguration configuration = new DefaultJupiterConfiguration(configurationParameters(Map.of()),
			dummyOutputDirectoryCreator(), mock());

		Supplier<TempDirFactory> supplier = configuration.getDefaultTempDirFactorySupplier();

		assertThat(supplier.get()).isSameAs(TempDirFactory.Standard.INSTANCE);
	}

	@Test
	void doesNotReportAnyIssuesIfConfigurationParametersAreEmpty() {
		List<DiscoveryIssue> issues = new ArrayList<>();

		new DefaultJupiterConfiguration(configurationParameters(Map.of()), dummyOutputDirectoryCreator(),
			DiscoveryIssueReporter.collecting(issues)).getDefaultTestInstanceLifecycle();

		assertThat(issues).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ParallelExecutorServiceType.class)
	void doesNotReportAnyIssuesIfParallelExecutionIsEnabledAndConfigurationParameterIsSet(
			ParallelExecutorServiceType executorServiceType) {
		var parameters = Map.of(JupiterConfiguration.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, true, //
			JupiterConfiguration.PARALLEL_CONFIG_EXECUTOR_SERVICE_PROPERTY_NAME, executorServiceType);
		List<DiscoveryIssue> issues = new ArrayList<>();

		new DefaultJupiterConfiguration(ConfigurationParametersFactoryForTests.create(parameters),
			dummyOutputDirectoryCreator(), DiscoveryIssueReporter.collecting(issues)).getDefaultTestInstanceLifecycle();

		assertThat(issues).isEmpty();
	}

	@Test
	void asksUsersToTryWorkerThreadPoolHierarchicalExecutorServiceIfParallelExecutionIsEnabled() {
		var parameters = Map.of(JupiterConfiguration.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, true);
		List<DiscoveryIssue> issues = new ArrayList<>();

		new DefaultJupiterConfiguration(configurationParameters(parameters), dummyOutputDirectoryCreator(),
			DiscoveryIssueReporter.collecting(issues)).getDefaultTestInstanceLifecycle();

		assertThat(issues).containsExactly(DiscoveryIssue.create(Severity.INFO, """
				Parallel test execution is enabled but the default ForkJoinPool-based executor service will be used. \
				Please give the new implementation based on a regular thread pool a try by setting the \
				'junit.jupiter.execution.parallel.config.executor-service' configuration parameter to \
				'WORKER_THREAD_POOL' and report any issues to the JUnit team. Alternatively, set the configuration \
				parameter to 'FORK_JOIN_POOL' to hide this message and keep using the original implementation."""));
	}

	private void assertDefaultConfigParam(@Nullable String configValue, Lifecycle expected) {
		var lifecycle = getDefaultTestInstanceLifecycleConfigParam(configValue);
		assertThat(lifecycle).isEqualTo(expected);
	}

	private static Lifecycle getDefaultTestInstanceLifecycleConfigParam(@Nullable String configValue) {
		var configParams = configurationParameters(configValue == null ? Map.of() : Map.of(KEY, configValue));
		return new DefaultJupiterConfiguration(configParams, dummyOutputDirectoryCreator(),
			mock()).getDefaultTestInstanceLifecycle();
	}

	private static ConfigurationParameters configurationParameters(Map<@NonNull String, ?> parameters) {
		return ConfigurationParametersFactoryForTests.create(parameters);
	}

	@NullMarked
	private static class CustomFactory implements TempDirFactory {

		@Override
		public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext) {
			throw new UnsupportedOperationException();
		}
	}

}
