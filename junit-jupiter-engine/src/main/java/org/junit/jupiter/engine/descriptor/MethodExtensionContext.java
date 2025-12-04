/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.LauncherStoreFacade;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.support.hierarchical.Node;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * @since 5.0
 */
final class MethodExtensionContext extends AbstractExtensionContext<TestMethodTestDescriptor> {

	private final ThrowableCollector throwableCollector;

	private @Nullable TestInstances testInstances;

	MethodExtensionContext(ExtensionContext parent, EngineExecutionListener engineExecutionListener,
			TestMethodTestDescriptor testDescriptor, JupiterConfiguration configuration,
			ExtensionRegistry extensionRegistry, LauncherStoreFacade launcherStoreFacade,
			ThrowableCollector throwableCollector) {

		super(parent, engineExecutionListener, testDescriptor, configuration, extensionRegistry, launcherStoreFacade);

		this.throwableCollector = throwableCollector;
	}

	@Override
	public Optional<AnnotatedElement> getElement() {
		return Optional.of(getTestDescriptor().getTestMethod());
	}

	@Override
	public Optional<Class<?>> getTestClass() {
		return Optional.of(getTestDescriptor().getTestClass());
	}

	@Override
	public List<Class<?>> getEnclosingTestClasses() {
		return getTestDescriptor().getEnclosingTestClasses();
	}

	@Override
	public Optional<Lifecycle> getTestInstanceLifecycle() {
		return getParent().flatMap(ExtensionContext::getTestInstanceLifecycle);
	}

	@Override
	public Optional<Object> getTestInstance() {
		return getTestInstances().map(TestInstances::getInnermostInstance);
	}

	@Override
	public Optional<TestInstances> getTestInstances() {
		return Optional.ofNullable(this.testInstances);
	}

	void setTestInstances(TestInstances testInstances) {
		this.testInstances = testInstances;
	}

	@Override
	public Optional<Method> getTestMethod() {
		return Optional.of(getTestDescriptor().getTestMethod());
	}

	@Override
	public Optional<Throwable> getExecutionException() {
		return Optional.ofNullable(this.throwableCollector.getThrowable());
	}

	@Override
	protected Node.ExecutionMode getPlatformExecutionMode() {
		return getTestDescriptor().getExecutionMode();
	}
}
