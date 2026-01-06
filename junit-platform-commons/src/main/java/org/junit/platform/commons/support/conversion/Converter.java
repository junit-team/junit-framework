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

/**
 * {@code Converter} is an abstraction that allows an input object to
 * be converted to an instance of a different class.
 *
 * <p>Implementations are loaded via the {@link java.util.ServiceLoader} and must
 * follow the service provider requirements. They should not make any assumptions
 * regarding when they are instantiated or how often they are called. Since
 * instances may potentially be cached and called from different threads, they
 * should be thread-safe.
 *
 * <p>Extend {@link SimpleConverter} if your implementation always converts
 * from a given source type into a given target type and does not need access to
 * the {@link ClassLoader} to perform the conversion.
 *
 * @param <S> the type of the source to convert
 * @param <T> the type the source should be converted into
 *
 * @since 6.0
 * @see ConversionSupport
 * @see SimpleConverter
 */
@API(status = EXPERIMENTAL, since = "6.0")
public interface Converter<S, T extends @Nullable Object> {

	/**
	 * Determine if the supplied conversion context is supported.
	 *
	 * @param context the context for the conversion; never {@code null}
	 * @return {@code true} if the conversion is supported
	 */
	boolean canConvert(ConversionContext context);

	/**
	 * Convert the supplied source object according to the supplied conversion context.
	 * <p>This method will only be invoked if {@link #canConvert(ConversionContext)}
	 * returns {@code true} for the same context.
	 *
	 * @param source the source object to convert; may be {@code null}
	 * but only if the target type is a reference type
	 * @param context the context for the conversion; never {@code null}
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ConversionException if an error occurs during the conversion
	 */
	T convert(@Nullable S source, ConversionContext context) throws ConversionException;

}
