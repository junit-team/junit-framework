/*
 * Copyright 2015-2026 the original author or authors.
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
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.OutputDirectoryCreator;

/**
 * @since 5.4
 */
@API(status = INTERNAL, since = "5.4")
public interface JupiterConfiguration {

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
