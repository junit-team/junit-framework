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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code SimpleConverter} is an abstract base class for {@link Converter}
 * implementations that always convert objects of a given source type into a
 * given target type.
 *
 * @param <S> the type of the source argument to convert
 * @param <T> the type of the target object to create from the source
 * @since 6.0
 */
@API(status = EXPERIMENTAL, since = "6.0")
public abstract class SimpleConverter<S, T extends @Nullable Object> implements Converter<S, T> {

	private final Class<S> sourceType;
	private final Class<T> targetType;

	/**
	 * Create a new {@code SimpleConverter}.
	 *
	 * @param sourceType the type of the argument to convert; never {@code null}
	 * @param targetType the type of the target object to create from the source;
	 * never {@code null}
	 */
	protected SimpleConverter(Class<S> sourceType, Class<T> targetType) {
		this.sourceType = Preconditions.notNull(sourceType, "sourceType must not be null");
		this.targetType = Preconditions.notNull(targetType, "targetType must not be null");
	}

	@Override
	public final boolean canConvert(ConversionContext context) {
		// FIXME adjust for subtypes
		return !context.sourceType().equals(TypeDescriptor.NONE) //
				&& this.sourceType == context.sourceType().getType() //
				&& this.targetType == context.targetType().getType();
	}

	@Override
	public final T convert(@Nullable S source, ConversionContext context) {
		Preconditions.notNull(source, "source cannot be null");
		return convert(source);
	}

	/**
	 * Convert the supplied {@code source} object of type {@code S} into an object
	 * of type {@code T}.
	 *
	 * @param source the source object to convert; never {@code null}
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ConversionException if an error occurs during the conversion
	 */
	protected abstract T convert(S source) throws ConversionException;

}
