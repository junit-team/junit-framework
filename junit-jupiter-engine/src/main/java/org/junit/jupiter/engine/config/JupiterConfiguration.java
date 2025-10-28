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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.PreInterruptCallback;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.OutputDirectoryCreator;

/**
 * @since 5.4
 */
@API(status = INTERNAL, since = "5.4")
public interface JupiterConfiguration {

	String EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME = "junit.jupiter.extensions.autodetection.include";
	String EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME = "junit.jupiter.extensions.autodetection.exclude";
	String DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME = "junit.jupiter.conditions.deactivate";
	String PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME = "junit.jupiter.execution.parallel.enabled";
	String CLOSING_STORED_AUTO_CLOSEABLE_ENABLED_PROPERTY_NAME = "junit.jupiter.extensions.store.close.autocloseable.enabled";
	String DEFAULT_EXECUTION_MODE_PROPERTY_NAME = Execution.DEFAULT_EXECUTION_MODE_PROPERTY_NAME;
	String DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME = Execution.DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME;
	String EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME = "junit.jupiter.extensions.autodetection.enabled";
	String EXTENSIONS_TIMEOUT_THREAD_DUMP_ENABLED_PROPERTY_NAME = PreInterruptCallback.THREAD_DUMP_ENABLED_PROPERTY_NAME;
	String DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME = TestInstance.Lifecycle.DEFAULT_LIFECYCLE_PROPERTY_NAME;
	String DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME = DisplayNameGenerator.DEFAULT_GENERATOR_PROPERTY_NAME;
	String DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME = MethodOrderer.DEFAULT_ORDER_PROPERTY_NAME;
	String DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME = ClassOrderer.DEFAULT_ORDER_PROPERTY_NAME;
	String DEFAULT_TEST_INSTANTIATION_EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME = ExtensionContextScope.DEFAULT_SCOPE_PROPERTY_NAME;

	Predicate<Class<? extends Extension>> getFilterForAutoDetectedExtensions();

	Optional<String> getRawConfigurationParameter(String key);

	<T> Optional<T> getRawConfigurationParameter(String key, Function<? super String, ? extends T> transformer);

	boolean isParallelExecutionEnabled();

	boolean isClosingStoredAutoCloseablesEnabled();

	boolean isExtensionAutoDetectionEnabled();

	boolean isThreadDumpOnTimeoutEnabled();

	ExecutionMode getDefaultExecutionMode();

	ExecutionMode getDefaultClassesExecutionMode();

	TestInstance.Lifecycle getDefaultTestInstanceLifecycle();

	Predicate<ExecutionCondition> getExecutionConditionFilter();

	DisplayNameGenerator getDefaultDisplayNameGenerator();

	Optional<MethodOrderer> getDefaultTestMethodOrderer();

	Optional<ClassOrderer> getDefaultTestClassOrderer();

	CleanupMode getDefaultTempDirCleanupMode();

	Supplier<TempDirFactory> getDefaultTempDirFactorySupplier();

	ExtensionContextScope getDefaultTestInstantiationExtensionContextScope();

	OutputDirectoryCreator getOutputDirectoryCreator();
}
