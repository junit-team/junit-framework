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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.engine.execution.ParameterResolutionUtils.resolveParameters;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.jupiter.engine.support.MethodReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.9
 */
@API(status = INTERNAL, since = "5.9")
public class DefaultExecutableInvoker implements ExecutableInvoker {

	private final ExtensionContext extensionContext;
	private final ExtensionRegistry extensionRegistry;

	public DefaultExecutableInvoker(ExtensionContext extensionContext, ExtensionRegistry extensionRegistry) {
		this.extensionContext = extensionContext;
		this.extensionRegistry = extensionRegistry;
	}

	@Override
	public <T> T invoke(Constructor<T> constructor, @Nullable Object outerInstance) {
		@Nullable
		Object[] arguments = resolveParameters(constructor, Optional.empty(), Optional.ofNullable(outerInstance),
			extensionContext, extensionRegistry);
		return ReflectionUtils.newInstance(constructor, arguments);
	}

	@Override
	public @Nullable Object invoke(Method method, @Nullable Object target) {
		@Nullable
		Object[] arguments = resolveParameters(method, Optional.ofNullable(target), extensionContext,
			extensionRegistry);
		return MethodReflectionUtils.invoke(method, target, arguments);
	}

}
