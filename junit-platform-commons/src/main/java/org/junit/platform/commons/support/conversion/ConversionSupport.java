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

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * {@code ConversionSupport} provides static utility methods for converting a
 * given object into an instance of a specified type.
 *
 * @since 1.11
 */
@API(status = MAINTAINED, since = "1.13.3")
public final class ConversionSupport {

	private ConversionSupport() {
		/* no-op */
	}

	/**
	 * Convert the supplied source {@code String} into an instance of the specified
	 * target type.
	 *
	 * @param source the source {@code String} to convert; may be {@code null}
	 * but only if the target type is a reference type
	 * @param targetType the target type the source should be converted into;
	 * never {@code null}
	 * @param classLoader the {@code ClassLoader} to use; may be {@code null} to
	 * use the default {@code ClassLoader}
	 * @param <T> the type of the target
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 *
	 * @since 1.11
	 * @see DefaultConverter
	 * @deprecated Use {@link #convert(Object, ConversionContext)} instead.
	 */
	@Deprecated
	@API(status = DEPRECATED, since = "6.0")
	public static <T> @Nullable T convert(@Nullable String source, Class<T> targetType,
			@Nullable ClassLoader classLoader) {
		ConversionContext context = new ConversionContext(source, TypeDescriptor.forClass(targetType), classLoader);
		return convert(source, context);
	}

	/**
	 * Convert the supplied source object into an instance of the specified
	 * target type.
	 *
	 * @param source the source object to convert; may be {@code null}
	 * but only if the target type is a reference type
	 * @param context the context for the conversion
	 * @param <T> the type of the target
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 * @since 6.0
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	@SuppressWarnings({ "unchecked", "rawtypes", "TypeParameterUnusedInFormals" })
	public static <T> @Nullable T convert(@Nullable Object source, ConversionContext context) {
		ServiceLoader<Converter> serviceLoader = ServiceLoader.load(Converter.class, context.classLoader());

		Converter converter = Stream.concat( //
			StreamSupport.stream(serviceLoader.spliterator(), false), //
			Stream.of(DefaultConverter.INSTANCE)) //
				.filter(candidate -> candidate.canConvert(context)) //
				.findFirst() //
				.orElseThrow(() -> new ConversionException(
					"No registered or built-in converter for source '%s' and target type %s".formatted( //
						source, context.targetType())));

		return (T) converter.convert(source, context);
	}

}
