/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * Internal API for converting arguments of type {@link String} to a specified
 * target type.
 */
abstract class StringToTargetTypeConverter<T extends @Nullable Object> implements Converter<String, T> {

	@Override
	public final boolean canConvert(ConversionContext context) {
		return !context.sourceType().equals(TypeDescriptor.NONE) && canConvert(context.targetType().getType());
	}

	/**
	 * Determine if this converter can convert from a {@link String} to the
	 * supplied target type.
	 */
	abstract boolean canConvert(Class<?> targetType);

	@Override
	public final T convert(@Nullable String source, ConversionContext context) {
		Preconditions.notNull(source, "source cannot be null");
		return convert(source, context.targetType().getType());
	}

	/**
	 * Convert the supplied {@link String} to the supplied target type.
	 *
	 * <p>This method will only be invoked if {@link #canConvert(Class)}
	 * returns {@code true} for the same target type.
	 */
	abstract T convert(String source, Class<?> targetType);

}
