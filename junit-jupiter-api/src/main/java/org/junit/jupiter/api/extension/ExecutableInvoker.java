/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * {@code ExecutableInvoker} allows invoking methods and constructors
 * with support for dynamic resolution of parameters via
 * {@link ParameterResolver ParameterResolvers}.
 *
 * @since 5.9
 */
@API(status = STABLE, since = "5.11")
public interface ExecutableInvoker {

	/**
	 * Invoke the supplied {@code static} method with dynamic parameter resolution.
	 *
	 * @param method the method to invoke and resolve parameters for
	 * @see #invoke(Method, Object)
	 */
	default @Nullable Object invoke(Method method) {
		return invoke(method, null);
	}

	/**
	 * Invoke the supplied method with dynamic parameter resolution.
	 *
	 * @param method the method to invoke and resolve parameters for
	 * @param target the target on which the executable will be invoked;
	 * can be {@code null} for {@code static} methods
	 */
	@Nullable
	Object invoke(Method method, @Nullable Object target);

	/**
	 * Invoke the supplied top-level constructor with dynamic parameter resolution.
	 *
	 * @param constructor the constructor to invoke and resolve parameters for
	 * @see #invoke(Constructor, Object)
	 */
	default <T> T invoke(Constructor<T> constructor) {
		return invoke(constructor, null);
	}

	/**
	 * Invoke the supplied constructor with the supplied outer instance and
	 * dynamic parameter resolution.
	 *
	 * <p>Use this method when invoking the constructor for an <em>inner</em> class.
	 *
	 * @param constructor the constructor to invoke and resolve parameters for
	 * @param outerInstance the outer instance to supply as the first argument
	 * to the constructor; must be {@code null} for top-level classes
	 * or {@code static} nested classes
	 */
	<T> T invoke(Constructor<T> constructor, @Nullable Object outerInstance);

}
