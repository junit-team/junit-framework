/*
 * Copyright 2015-2026 the original author or authors.
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

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;

/**
 * Default implementation of {@link ClassOrdererContext}.
 *
 * @since 5.8
 */
class DefaultClassOrdererContext implements ClassOrdererContext {

	private final List<? extends ClassDescriptor> classDescriptors;
	private final JupiterConfiguration configuration;

	DefaultClassOrdererContext(List<? extends ClassDescriptor> classDescriptors, JupiterConfiguration configuration) {
		this.classDescriptors = classDescriptors;
		this.configuration = configuration;
	}

	@Override
	public List<? extends ClassDescriptor> getClassDescriptors() {
		return this.classDescriptors;
	}

	@Override
	public Optional<String> getConfigurationParameter(String key) {
		return this.configuration.getRawConfigurationParameter(key);
	}

}
