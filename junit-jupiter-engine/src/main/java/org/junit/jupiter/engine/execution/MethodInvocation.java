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

import static java.util.Collections.unmodifiableList;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.engine.support.MethodReflectionUtils;

class MethodInvocation<T extends @Nullable Object> implements Invocation<T>, ReflectiveInvocationContext<Method> {

	private final Method method;
	private final @Nullable Object target;
	private final @Nullable Object[] arguments;

	MethodInvocation(Method method, @Nullable Object target, @Nullable Object[] arguments) {
		this.method = method;
		this.target = target;
		this.arguments = arguments;
	}

	@Override
	public Class<?> getTargetClass() {
		return this.target != null ? this.target.getClass() : this.method.getDeclaringClass();
	}

	@Override
	public Optional<Object> getTarget() {
		return Optional.ofNullable(this.target);
	}

	@Override
	public Method getExecutable() {
		return this.method;
	}

	@Override
	public List<@Nullable Object> getArguments() {
		List<@Nullable Object> list = Arrays.asList(this.arguments);
		return unmodifiableList(list);
	}

	@Override
	@SuppressWarnings({ "unchecked", "NullAway" })
	public T proceed() {
		return (T) MethodReflectionUtils.invoke(this.method, this.target, this.arguments);
	}

}
