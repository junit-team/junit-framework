/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrdererContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of {@link MethodOrdererContext}.
 *
 * @since 5.4
 */
class DefaultMethodOrdererContext implements MethodOrdererContext {

	private final Class<?> testClass;
	private final List<? extends MethodDescriptor> methodDescriptors;
	private final JupiterConfiguration configuration;

	DefaultMethodOrdererContext(Class<?> testClass, List<? extends MethodDescriptor> methodDescriptors,
			JupiterConfiguration configuration) {

		this.testClass = testClass;
		this.methodDescriptors = methodDescriptors;
		this.configuration = configuration;
	}

	@Override
	public final Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public List<? extends MethodDescriptor> getMethodDescriptors() {
		return this.methodDescriptors;
	}

	@Override
	public Optional<String> getConfigurationParameter(String key) {
		return this.configuration.getRawConfigurationParameter(key);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("methodDescriptors", methodDescriptors).toString();
	}

}
