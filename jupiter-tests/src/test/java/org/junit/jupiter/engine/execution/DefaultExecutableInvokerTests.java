/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;

import org.jspecify.annotations.Nullable;

/**
 * Unit tests for {@link DefaultExecutableInvoker}.
 *
 * @since 5.9
 */
class DefaultExecutableInvokerTests extends AbstractExecutableInvokerTests {

	@Override
	void invokeMethod() {
		newInvoker().invoke(requireNonNull(this.method), this.instance);
	}

	@Override
	<T> T invokeConstructor(Constructor<T> constructor, @Nullable Object outerInstance) {
		return newInvoker().invoke(constructor, outerInstance);
	}

	private DefaultExecutableInvoker newInvoker() {
		return new DefaultExecutableInvoker(this.extensionContext, this.extensionRegistry);
	}

}
