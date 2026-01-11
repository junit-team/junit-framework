/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;

/**
 * @since 1.0
 */
public class FilterStub<T> implements Filter<T> {

	private final Function<T, FilterResult> function;
	private final Supplier<String> toString;

	public FilterStub(String toString) {
		this(o -> FilterResult.included("always"), () -> toString);
	}

	public FilterStub(Function<T, FilterResult> function, Supplier<String> toString) {
		this.function = function;
		this.toString = toString;
	}

	@Override
	public FilterResult apply(T object) {
		return function.apply(object);
	}

	@Override
	public String toString() {
		return toString.get();
	}

}
