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
import static org.junit.jupiter.api.io.TempDir.DEFAULT_CLEANUP_MODE_PROPERTY_NAME;
import static org.junit.jupiter.api.io.TempDir.DEFAULT_FACTORY_PROPERTY_NAME;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
 * Caching implementation of the {@link JupiterConfiguration} API.
 *
 * @since 5.4
 */
@API(status = INTERNAL, since = "5.4")
public class CachingJupiterConfiguration implements JupiterConfiguration {

	private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
	private final JupiterConfiguration delegate;

	public CachingJupiterConfiguration(JupiterConfiguration delegate) {
		this.delegate = delegate;
	}

	@Override
	public Predicate<Class<? extends Extension>> getFilterForAutoDetectedExtensions() {
		return delegate.getFilterForAutoDetectedExtensions();
	}

	@Override
	public Optional<String> getRawConfigurationParameter(String key) {
		return delegate.getRawConfigurationParameter(key);
	}

	@Override
	public <T> Optional<T> getRawConfigurationParameter(String key, Function<? super String, ? extends T> transformer) {
		return delegate.getRawConfigurationParameter(key, transformer);
	}

	@Override
	public boolean isParallelExecutionEnabled() {
		return (boolean) cache.computeIfAbsent(PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME,
			__ -> delegate.isParallelExecutionEnabled());
	}

	@Override
	public boolean isClosingStoredAutoCloseablesEnabled() {
		return (boolean) cache.computeIfAbsent(CLOSING_STORED_AUTO_CLOSEABLE_ENABLED_PROPERTY_NAME,
			__ -> delegate.isClosingStoredAutoCloseablesEnabled());
	}

	@Override
	public boolean isExtensionAutoDetectionEnabled() {
		return (boolean) cache.computeIfAbsent(EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME,
			__ -> delegate.isExtensionAutoDetectionEnabled());
	}

	@Override
	public boolean isThreadDumpOnTimeoutEnabled() {
		return (boolean) cache.computeIfAbsent(EXTENSIONS_TIMEOUT_THREAD_DUMP_ENABLED_PROPERTY_NAME,
			__ -> delegate.isThreadDumpOnTimeoutEnabled());
	}

	@Override
	public ExecutionMode getDefaultExecutionMode() {
		return (ExecutionMode) cache.computeIfAbsent(DEFAULT_EXECUTION_MODE_PROPERTY_NAME,
			__ -> delegate.getDefaultExecutionMode());
	}

	@Override
	public ExecutionMode getDefaultClassesExecutionMode() {
		return (ExecutionMode) cache.computeIfAbsent(DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME,
			__ -> delegate.getDefaultClassesExecutionMode());
	}

	@Override
	public TestInstance.Lifecycle getDefaultTestInstanceLifecycle() {
		return (TestInstance.Lifecycle) cache.computeIfAbsent(DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME,
			__ -> delegate.getDefaultTestInstanceLifecycle());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Predicate<ExecutionCondition> getExecutionConditionFilter() {
		return (Predicate<ExecutionCondition>) cache.computeIfAbsent(DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME,
			__ -> delegate.getExecutionConditionFilter());
	}

	@Override
	public DisplayNameGenerator getDefaultDisplayNameGenerator() {
		return (DisplayNameGenerator) cache.computeIfAbsent(DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME,
			__ -> delegate.getDefaultDisplayNameGenerator());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<MethodOrderer> getDefaultTestMethodOrderer() {
		return (Optional<MethodOrderer>) cache.computeIfAbsent(DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME,
			__ -> delegate.getDefaultTestMethodOrderer());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<ClassOrderer> getDefaultTestClassOrderer() {
		return (Optional<ClassOrderer>) cache.computeIfAbsent(DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME,
			__ -> delegate.getDefaultTestClassOrderer());
	}

	@Override
	public CleanupMode getDefaultTempDirCleanupMode() {
		return (CleanupMode) cache.computeIfAbsent(DEFAULT_CLEANUP_MODE_PROPERTY_NAME,
			__ -> delegate.getDefaultTempDirCleanupMode());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Supplier<TempDirFactory> getDefaultTempDirFactorySupplier() {
		return (Supplier<TempDirFactory>) cache.computeIfAbsent(DEFAULT_FACTORY_PROPERTY_NAME,
			__ -> delegate.getDefaultTempDirFactorySupplier());
	}

	@Override
	public ExtensionContextScope getDefaultTestInstantiationExtensionContextScope() {
		return (ExtensionContextScope) cache.computeIfAbsent(
			DEFAULT_TEST_INSTANTIATION_EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME,
			__ -> delegate.getDefaultTestInstantiationExtensionContextScope());
	}

	@Override
	public OutputDirectoryCreator getOutputDirectoryCreator() {
		return delegate.getOutputDirectoryCreator();
	}
}
