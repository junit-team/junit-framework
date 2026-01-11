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

import java.util.Optional;

import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default implementation of the {@link TestInstanceFactoryContext} API.
 *
 * @since 5.3
 */
record DefaultTestInstanceFactoryContext(Class<?> testClass, Optional<Object> outerInstance)
		implements TestInstanceFactoryContext {

	@Override
	public Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public Optional<Object> getOuterInstance() {
		return this.outerInstance;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("testClass", this.testClass)
				.append("outerInstance", this.outerInstance)
				.toString();
		// @formatter:on
	}

}
