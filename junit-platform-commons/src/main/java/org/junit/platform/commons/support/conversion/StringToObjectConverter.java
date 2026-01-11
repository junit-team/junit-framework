/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import org.jspecify.annotations.Nullable;

/**
 * Internal API for converting arguments of type {@link String} to a specified
 * target type.
 */
interface StringToObjectConverter {

	/**
	 * Determine if this converter can convert from a {@link String} to the
	 * supplied target type (which is guaranteed to be a wrapper type for
	 * primitives &mdash; for example, {@link Integer} instead of {@code int}).
	 */
	boolean canConvertTo(Class<?> targetType);

	/**
	 * Convert the supplied {@link String} to the supplied target type (which is
	 * guaranteed to be a wrapper type for primitives &mdash; for example,
	 * {@link Integer} instead of {@code int}).
	 *
	 * <p>This method will only be invoked if {@link #canConvertTo(Class)}
	 * returns {@code true} for the same target type.
	 */
	@Nullable
	Object convert(String source, Class<?> targetType) throws Exception;

	/**
	 * Convert the supplied {@link String} to the supplied target type (which is
	 * guaranteed to be a wrapper type for primitives &mdash; for example,
	 * {@link Integer} instead of {@code int}).
	 *
	 * <p>This method will only be invoked if {@link #canConvertTo(Class)}
	 * returns {@code true} for the same target type.
	 *
	 * <p>The default implementation simply delegates to {@link #convert(String, Class)}.
	 * Can be overridden by concrete implementations of this interface that need
	 * access to the supplied {@link ClassLoader}.
	 */
	default @Nullable Object convert(String source, Class<?> targetType, ClassLoader classLoader) throws Exception {
		return convert(source, targetType);
	}

}
