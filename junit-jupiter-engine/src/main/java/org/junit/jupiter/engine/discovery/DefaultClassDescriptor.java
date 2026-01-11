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

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of {@link ClassDescriptor}, backed by
 * a {@link ClassBasedTestDescriptor}.
 *
 * @since 5.8
 */
class DefaultClassDescriptor extends AbstractAnnotatedDescriptorWrapper<Class<?>> implements ClassDescriptor {

	DefaultClassDescriptor(ClassBasedTestDescriptor testDescriptor) {
		super(testDescriptor, testDescriptor.getTestClass());
	}

	@Override
	public final Class<?> getTestClass() {
		return getAnnotatedElement();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("class", getTestClass().toGenericString()).toString();
	}

}
