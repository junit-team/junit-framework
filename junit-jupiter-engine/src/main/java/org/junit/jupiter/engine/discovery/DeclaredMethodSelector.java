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

import java.lang.reflect.Method;
import java.util.List;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;

/**
 * @since 6.0.1
 */
record DeclaredMethodSelector(List<Class<?>> testClasses, Method method) implements DiscoverySelector {
	DeclaredMethodSelector {
		Preconditions.notEmpty(testClasses, "testClasses must not be empty");
		Preconditions.containsNoNullElements(testClasses, "testClasses must not contain null elements");
		Preconditions.notNull(method, "method must not be null");
	}
}
