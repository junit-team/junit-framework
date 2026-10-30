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

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;

/**
 * Default implementation of {@link ClassOrdererContext}.
 *
 * @since 5.8
 */
class DefaultClassOrdererContext extends DefaultDiscoveryContext implements ClassOrdererContext {

	private final List<? extends ClassDescriptor> classDescriptors;

	DefaultClassOrdererContext(List<? extends ClassDescriptor> classDescriptors, JupiterConfiguration configuration) {
		super(configuration);
		this.classDescriptors = classDescriptors;
	}

	@Override
	public List<? extends ClassDescriptor> getClassDescriptors() {
		return this.classDescriptors;
	}

}
