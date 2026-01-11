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

import java.lang.reflect.Method;

import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of {@link MethodDescriptor}, backed by
 * a {@link MethodBasedTestDescriptor}.
 *
 * @since 5.4
 */
class DefaultMethodDescriptor extends AbstractAnnotatedDescriptorWrapper<Method> implements MethodDescriptor {

	DefaultMethodDescriptor(MethodBasedTestDescriptor testDescriptor) {
		super(testDescriptor, testDescriptor.getTestMethod());
	}

	@Override
	public final Method getMethod() {
		return getAnnotatedElement();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("method", getMethod().toGenericString()).toString();
	}

}
