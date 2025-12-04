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

import static java.util.Collections.emptyList;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.LauncherStoreFacade;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.support.hierarchical.Node;

class DynamicExtensionContext extends AbstractExtensionContext<DynamicNodeTestDescriptor> {

	DynamicExtensionContext(ExtensionContext parent, EngineExecutionListener engineExecutionListener,
			DynamicNodeTestDescriptor testDescriptor, JupiterConfiguration configuration,
			ExtensionRegistry extensionRegistry, LauncherStoreFacade launcherStoreFacade) {
		super(parent, engineExecutionListener, testDescriptor, configuration, extensionRegistry, launcherStoreFacade);
	}

	@Override
	public Optional<AnnotatedElement> getElement() {
		return Optional.empty();
	}

	@Override
	public Optional<Class<?>> getTestClass() {
		return Optional.empty();
	}

	@Override
	public List<Class<?>> getEnclosingTestClasses() {
		return emptyList();
	}

	@Override
	public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
		return Optional.empty();
	}

	@Override
	public Optional<Object> getTestInstance() {
		return Optional.empty();
	}

	@Override
	public Optional<TestInstances> getTestInstances() {
		return Optional.empty();
	}

	@Override
	public Optional<Method> getTestMethod() {
		return Optional.empty();
	}

	@Override
	public Optional<Throwable> getExecutionException() {
		return Optional.empty();
	}

	@Override
	protected Node.ExecutionMode getPlatformExecutionMode() {
		return getTestDescriptor().getExecutionMode();
	}

}
