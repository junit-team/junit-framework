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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.util.ReflectionUtils;

class ConstructorInvocation<T> implements Invocation<T>, ReflectiveInvocationContext<Constructor<T>> {

	private final Constructor<T> constructor;
	private final @Nullable Object[] arguments;

	ConstructorInvocation(Constructor<T> constructor, @Nullable Object[] arguments) {
		this.constructor = constructor;
		this.arguments = arguments;
	}

	@Override
	public Class<?> getTargetClass() {
		return this.constructor.getDeclaringClass();
	}

	@Override
	public Constructor<T> getExecutable() {
		return this.constructor;
	}

	@Override
	public List<Object> getArguments() {
		return unmodifiableList(Arrays.asList(this.arguments));
	}

	@Override
	public Optional<Object> getTarget() {
		return Optional.empty();
	}

	@Override
	public T proceed() {
		return ReflectionUtils.newInstance(this.constructor, this.arguments);
	}

}
