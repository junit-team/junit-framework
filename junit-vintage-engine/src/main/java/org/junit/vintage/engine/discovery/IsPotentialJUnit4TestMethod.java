/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.junit.Test;

/**
 * @since 4.12
 */
class IsPotentialJUnit4TestMethod implements Predicate<Method> {

	@Override
	public boolean test(Method method) {
		// Don't use AnnotationUtils.isAnnotated since JUnit 4 does not support
		// meta-annotations
		return method.isAnnotationPresent(Test.class);
	}

}
