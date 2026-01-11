/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.Preconditions;

/**
 * Collection of utilities for retrieving the test instance lifecycle mode.
 *
 * @since 5.0
 * @see TestInstance
 * @see TestInstance.Lifecycle
 */
@API(status = INTERNAL, since = "5.0")
public final class TestInstanceLifecycleUtils {

	private TestInstanceLifecycleUtils() {
		/* no-op */
	}

	static TestInstance.Lifecycle getTestInstanceLifecycle(Class<?> testClass, JupiterConfiguration configuration) {
		Preconditions.notNull(testClass, "testClass must not be null");
		Preconditions.notNull(configuration, "configuration must not be null");

		// @formatter:off
		return AnnotationSupport.findAnnotation(testClass, TestInstance.class)
				.map(TestInstance::value)
				.orElseGet(configuration::getDefaultTestInstanceLifecycle);
		// @formatter:on
	}

}
