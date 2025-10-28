/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_CUSTOM_CLASS_PROPERTY_NAME;
import static org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME;
import static org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME;
import static org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_FIXED_PARALLELISM_PROPERTY_NAME;
import static org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_FIXED_SATURATE_PROPERTY_NAME;
import static org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_STRATEGY_PROPERTY_NAME;

import org.apiguardian.api.API;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.util.ClassNamePatternFilterUtils;
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy;

/**
 * Collection of constants related to the {@link JupiterTestEngine}.
 *
 * @since 5.0
 * @see org.junit.platform.engine.ConfigurationParameters
 */
@API(status = STABLE, since = "5.0")
public final class Constants {

	/**
	 * Property name used to include patterns for auto-detecting extensions: {@value}
	 *
	 * <h4>Pattern Matching Syntax</h4>
	 *
	 * <p>If the property value consists solely of an asterisk ({@code *}), all
	 * extensions will be included. Otherwise, the property value will be treated
	 * as a comma-separated list of patterns where each individual pattern will be
	 * matched against the fully qualified class name (<em>FQCN</em>) of each extension.
	 * Any dot ({@code .}) in a pattern will match against a dot ({@code .})
	 * or a dollar sign ({@code $}) in a FQCN. Any asterisk ({@code *}) will match
	 * against one or more characters in a FQCN. All other characters in a pattern
	 * will be matched one-to-one against a FQCN.
	 *
	 * <h4>Examples</h4>
	 *
	 * <ul>
	 * <li>{@code *}: includes all extensions.
	 * <li>{@code org.junit.*}: includes every extension under the {@code org.junit}
	 * base package and any of its subpackages.
	 * <li>{@code *.MyExtension}: includes every extension whose simple class name is
	 * exactly {@code MyExtension}.
	 * <li>{@code *System*}: includes every extension whose FQCN contains
	 * {@code System}.
	 * <li>{@code *System*, *Dev*}: includes every extension whose FQCN contains
	 * {@code System} or {@code Dev}.
	 * <li>{@code org.example.MyExtension, org.example.TheirExtension}: includes
	 * extensions whose FQCN is exactly {@code org.example.MyExtension} or
	 * {@code org.example.TheirExtension}.
	 * </ul>
	 *
	 * <p>Note: A class that matches both an inclusion and exclusion pattern will be excluded.
	 *
	 * @see JupiterConfiguration#EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME
	 */
	public static final String EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME = JupiterConfiguration.EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME;

	/**
	 * Property name used to exclude patterns for auto-detecting extensions: {@value}
	 *
	 * <h4>Pattern Matching Syntax</h4>
	 *
	 * <p>If the property value consists solely of an asterisk ({@code *}), all
	 * extensions will be excluded. Otherwise, the property value will be treated
	 * as a comma-separated list of patterns where each individual pattern will be
	 * matched against the fully qualified class name (<em>FQCN</em>) of each extension.
	 * Any dot ({@code .}) in a pattern will match against a dot ({@code .})
	 * or a dollar sign ({@code $}) in a FQCN. Any asterisk ({@code *}) will match
	 * against one or more characters in a FQCN. All other characters in a pattern
	 * will be matched one-to-one against a FQCN.
	 *
	 * <h4>Examples</h4>
	 *
	 * <ul>
	 * <li>{@code *}: excludes all extensions.
	 * <li>{@code org.junit.*}: excludes every extension under the {@code org.junit}
	 * base package and any of its subpackages.
	 * <li>{@code *.MyExtension}: excludes every extension whose simple class name is
	 * exactly {@code MyExtension}.
	 * <li>{@code *System*}: excludes every extension whose FQCN contains
	 * {@code System}.
	 * <li>{@code *System*, *Dev*}: excludes every extension whose FQCN contains
	 * {@code System} or {@code Dev}.
	 * <li>{@code org.example.MyExtension, org.example.TheirExtension}: excludes
	 * extensions whose FQCN is exactly {@code org.example.MyExtension} or
	 * {@code org.example.TheirExtension}.
	 * </ul>
	 *
	 * <p>Note: A class that matches both an inclusion and exclusion pattern will be excluded.
	 *
	 * @see JupiterConfiguration#EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME
	 */
	public static final String EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME = JupiterConfiguration.EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME;

	/**
	 * Property name used to provide patterns for deactivating conditions: {@value}
	 *
	 * <h4>Pattern Matching Syntax</h4>
	 *
	 * <p>If the property value consists solely of an asterisk ({@code *}), all
	 * conditions will be deactivated. Otherwise, the property value will be treated
	 * as a comma-separated list of patterns where each individual pattern will be
	 * matched against the fully qualified class name (<em>FQCN</em>) of each registered
	 * condition. Any dot ({@code .}) in a pattern will match against a dot ({@code .})
	 * or a dollar sign ({@code $}) in a FQCN. Any asterisk ({@code *}) will match
	 * against one or more characters in a FQCN. All other characters in a pattern
	 * will be matched one-to-one against a FQCN.
	 *
	 * <h4>Examples</h4>
	 *
	 * <ul>
	 * <li>{@code *}: deactivates all conditions.
	 * <li>{@code org.junit.*}: deactivates every condition under the {@code org.junit}
	 * base package and any of its subpackages.
	 * <li>{@code *.MyCondition}: deactivates every condition whose simple class name is
	 * exactly {@code MyCondition}.
	 * <li>{@code *System*}: deactivates every condition whose FQCN contains
	 * {@code System}.
	 * <li>{@code *System*, *Dev*}: deactivates every condition whose FQCN contains
	 * {@code System} or {@code Dev}.
	 * <li>{@code org.example.MyCondition, org.example.TheirCondition}: deactivates
	 * conditions whose FQCN is exactly {@code org.example.MyCondition} or
	 * {@code org.example.TheirCondition}.
	 * </ul>
	 *
	 * @see #DEACTIVATE_ALL_CONDITIONS_PATTERN
	 * @see org.junit.jupiter.api.extension.ExecutionCondition
	 */
	public static final String DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME = JupiterConfiguration.DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME;

	/**
	 * Wildcard pattern which signals that all conditions should be deactivated: {@value}
	 *
	 * @see #DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME
	 * @see org.junit.jupiter.api.extension.ExecutionCondition
	 */
	public static final String DEACTIVATE_ALL_CONDITIONS_PATTERN = ClassNamePatternFilterUtils.ALL_PATTERN;

	/**
	 * Property name used to set the default display name generator class name: {@value}
	 *
	 * @see DisplayNameGenerator#DEFAULT_GENERATOR_PROPERTY_NAME
	 */
	public static final String DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME = DisplayNameGenerator.DEFAULT_GENERATOR_PROPERTY_NAME;

	/**
	 * Property name used to enable auto-detection and registration of extensions via
	 * Java's {@link java.util.ServiceLoader} mechanism: {@value}
	 *
	 * <p>The default behavior is not to perform auto-detection.
	 */
	public static final String EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME = JupiterConfiguration.EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME;

	/**
	 * Property name used to enable dumping the stack of all
	 * {@linkplain Thread threads} to {@code System.out} when a timeout has occurred.
	 *
	 * <p>This behavior is disabled by default.
	 *
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String EXTENSIONS_TIMEOUT_THREAD_DUMP_ENABLED_PROPERTY_NAME = JupiterConfiguration.EXTENSIONS_TIMEOUT_THREAD_DUMP_ENABLED_PROPERTY_NAME;

	/**
	 * Property name used to set the default test instance lifecycle mode: {@value}
	 *
	 * @see TestInstance.Lifecycle#DEFAULT_LIFECYCLE_PROPERTY_NAME
	 */
	public static final String DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME = TestInstance.Lifecycle.DEFAULT_LIFECYCLE_PROPERTY_NAME;

	/**
	 * Property name used to enable parallel test execution: {@value}
	 *
	 * <p>By default, tests are executed sequentially in a single thread.
	 *
	 * @since 5.3
	 */
	@API(status = STABLE, since = "5.10")
	public static final String PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME = JupiterConfiguration.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME;

	/**
	 * Property name used to enable auto-closing of {@link AutoCloseable} instances
	 *
	 * <p>By default, auto-closing is enabled.
	 *
	 * @since 5.13
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static final String CLOSING_STORED_AUTO_CLOSEABLE_ENABLED_PROPERTY_NAME = JupiterConfiguration.CLOSING_STORED_AUTO_CLOSEABLE_ENABLED_PROPERTY_NAME;

	/**
	 * Property name used to set the default test execution mode: {@value}
	 *
	 * @see Execution#DEFAULT_EXECUTION_MODE_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_PARALLEL_EXECUTION_MODE = Execution.DEFAULT_EXECUTION_MODE_PROPERTY_NAME;

	/**
	 * Property name used to set the default test execution mode for top-level
	 * classes: {@value}
	 *
	 * @see Execution#DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME = Execution.DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME;

	static final String PARALLEL_CONFIG_PREFIX = "junit.jupiter.execution.parallel.config.";

	/**
	 * Property name used to select the
	 * {@link ParallelExecutionConfigurationStrategy}: {@value}
	 *
	 * <p>Potential values: {@code dynamic} (default), {@code fixed}, or
	 * {@code custom}.
	 *
	 * @since 5.3
	 */
	@API(status = STABLE, since = "5.10")
	public static final String PARALLEL_CONFIG_STRATEGY_PROPERTY_NAME = PARALLEL_CONFIG_PREFIX
			+ CONFIG_STRATEGY_PROPERTY_NAME;

	/**
	 * Property name used to set the desired parallelism for the {@code fixed}
	 * configuration strategy: {@value}
	 *
	 * <p>No default value; must be a positive integer.
	 *
	 * @since 5.3
	 */
	@API(status = STABLE, since = "5.10")
	public static final String PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME = PARALLEL_CONFIG_PREFIX
			+ CONFIG_FIXED_PARALLELISM_PROPERTY_NAME;

	/**
	 * Property name used to configure the maximum pool size of the underlying
	 * fork-join pool for the {@code fixed} configuration strategy: {@value}
	 *
	 * <p>Value must be an integer and greater than or equal to
	 * {@value #PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME}; defaults to
	 * {@code 256 + fixed.parallelism}.
	 *
	 * @since 5.10
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME = PARALLEL_CONFIG_PREFIX
			+ CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME;

	/**
	 * Property name used to disable saturation of the underlying fork-join pool
	 * for the {@code fixed} configuration strategy: {@value}
	 *
	 * <p>When set to {@code false} the underlying fork-join pool will reject
	 * additional tasks if all available workers are busy and the maximum
	 * pool-size would be exceeded.
	 *
	 * <p>Value must either {@code true} or {@code false}; defaults to {@code true}.
	 *
	 * @since 5.10
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String PARALLEL_CONFIG_FIXED_SATURATE_PROPERTY_NAME = PARALLEL_CONFIG_PREFIX
			+ CONFIG_FIXED_SATURATE_PROPERTY_NAME;

	/**
	 * Property name used to set the factor to be multiplied with the number of
	 * available processors/cores to determine the desired parallelism for the
	 * {@code dynamic} configuration strategy: {@value}
	 *
	 * <p>Value must be a positive decimal number; defaults to {@code 1}.
	 *
	 * @since 5.3
	 */
	@API(status = STABLE, since = "5.10")
	public static final String PARALLEL_CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME = PARALLEL_CONFIG_PREFIX
			+ CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME;

	/**
	 * Property name used to specify the fully qualified class name of the
	 * {@link ParallelExecutionConfigurationStrategy} to be used for the
	 * {@code custom} configuration strategy: {@value}
	 *
	 * @since 5.3
	 */
	@API(status = STABLE, since = "5.10")
	public static final String PARALLEL_CONFIG_CUSTOM_CLASS_PROPERTY_NAME = PARALLEL_CONFIG_PREFIX
			+ CONFIG_CUSTOM_CLASS_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all testable and
	 * lifecycle methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all testable methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all
	 * {@link Test @Test} methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all
	 * {@link TestTemplate @TestTemplate} methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all
	 * {@link TestFactory @TestFactory} methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all lifecycle methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all
	 * {@link BeforeAll @BeforeAll} methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all
	 * {@link BeforeEach @BeforeEach} methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all
	 * {@link AfterEach @AfterEach} methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout for all
	 * {@link AfterAll @AfterAll} methods: {@value}.
	 *
	 * @see Timeout#DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME = Timeout.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME;

	/**
	 * Property name used to configure whether timeouts are applied to tests: {@value}.
	 *
	 * @see Timeout#TIMEOUT_MODE_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.10")
	public static final String TIMEOUT_MODE_PROPERTY_NAME = Timeout.TIMEOUT_MODE_PROPERTY_NAME;

	/**
	 * Property name used to set the default method orderer class name: {@value}
	 *
	 * @see MethodOrderer#DEFAULT_ORDER_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.9")
	public static final String DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME = MethodOrderer.DEFAULT_ORDER_PROPERTY_NAME;

	/**
	 * Property name used to set the default class orderer class name: {@value}
	 *
	 * @see ClassOrderer#DEFAULT_ORDER_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.9")
	public static final String DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME = ClassOrderer.DEFAULT_ORDER_PROPERTY_NAME;

	/**
	 * Property name used to set the default timeout thread mode.
	 *
	 * @since 5.9
	 * @see Timeout
	 * @see Timeout.ThreadMode
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME = Timeout.DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME;

	/**
	 * Property name used to set the default factory for temporary directories created via
	 * the {@link TempDir @TempDir} annotation: {@value}
	 *
	 * @since 5.10
	 * @see TempDir#DEFAULT_FACTORY_PROPERTY_NAME
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String DEFAULT_TEMP_DIR_FACTORY_PROPERTY_NAME = TempDir.DEFAULT_FACTORY_PROPERTY_NAME;

	/**
	 * Property name used to set the default extension context scope for
	 * extensions that participate in test instantiation: {@value}
	 *
	 * @since 5.12
	 * @see org.junit.jupiter.api.extension.TestInstantiationAwareExtension
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String DEFAULT_TEST_CLASS_INSTANCE_CONSTRUCTION_EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME = ExtensionContextScope.DEFAULT_SCOPE_PROPERTY_NAME;

	private Constants() {
		/* no-op */
	}

}
