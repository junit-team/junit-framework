/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Collection of utilities for working with {@link Function Functions},
 * {@link Predicate Predicates}, etc.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class FunctionUtils {

	private FunctionUtils() {
		/* no-op */
	}

	/**
	 * Return a predicate that first applies the specified function and then
	 * tests the specified predicate against the result of the function.
	 *
	 * @param function the function to apply
	 * @param predicate the predicate to test against the result of the function
	 */
	public static <T extends @Nullable Object, V extends @Nullable Object> Predicate<T> where(Function<T, V> function,
			Predicate<? super V> predicate) {
		Preconditions.notNull(function, "function must not be null");
		Preconditions.notNull(predicate, "predicate must not be null");
		return input -> predicate.test(function.apply(input));
	}

}
