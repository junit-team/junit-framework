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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ClassLoaderUtils;

/**
 * {@code DefaultConversionService} is the default implementation of the
 * {@link Converter} API.
 *
 * <p>The {@code DefaultConversionService} is able to convert from strings to a
 * number of primitive types and their corresponding wrapper types (Byte, Short,
 * Integer, Long, Float, and Double), date and time types from the
 * {@code java.time} package, and some additional common Java types such as
 * {@link File}, {@link BigDecimal}, {@link BigInteger}, {@link Currency},
 * {@link Locale}, {@link URI}, {@link URL}, {@link UUID}, etc.
 *
 * <p>If the source and target types are identical, the source object will not
 * be modified.
 *
 * @since 6.0
 */
@API(status = INTERNAL, since = "6.0")
public class DefaultConverter implements Converter<String, @Nullable Object> {

	static final DefaultConverter INSTANCE = new DefaultConverter();

	private static final List<Converter<String, ?>> stringToObjectConverters = List.of( //
		new StringToBooleanConverter(), //
		new StringToCharacterConverter(), //
		new StringToNumberConverter(), //
		new StringToClassConverter(), //
		new StringToEnumConverter(), //
		new StringToJavaTimeConverter(), //
		new StringToCommonJavaTypesConverter(), //
		new FallbackStringToObjectConverter() //
	);

	private DefaultConverter() {
		// nothing to initialize
	}

	/**
	 * Determine if the supplied conversion context is supported.
	 * <p>FIXME add more content from {@link Converter#convert} about the conversion algorithm
	 *
	 * @param context the context for the conversion; never {@code null}
	 * @return {@code true} if the conversion is supported
	 */
	@Override
	public boolean canConvert(ConversionContext context) {
		if (context.sourceType().equals(TypeDescriptor.NONE)) {
			return !context.targetType().isPrimitive();
		}

		if (!String.class.equals(context.sourceType().getType())) {
			return false;
		}

		if (String.class.equals(context.targetType().getType())) {
			return true;
		}

		return stringToObjectConverters.stream().anyMatch(candidate -> candidate.canConvert(context));
	}

	/**
	 * Convert the supplied source {@link String} into an instance of the specified
	 * target type.
	 * <p>If the target type is {@code String}, the source {@code String} will not
	 * be modified.
	 * <p>Some forms of conversion require a {@link ClassLoader}. If none is
	 * provided, the {@linkplain ClassLoaderUtils#getDefaultClassLoader() default
	 * ClassLoader} will be used.
	 * <p>This method is able to convert strings into primitive types and their
	 * corresponding wrapper types ({@link Boolean}, {@link Character}, {@link Byte},
	 * {@link Short}, {@link Integer}, {@link Long}, {@link Float}, and
	 * {@link Double}), enum constants, date and time types from the
	 * {@code java.time} package, as well as common Java types such as {@link Class},
	 * {@link java.io.File}, {@link java.nio.file.Path}, {@link java.nio.charset.Charset},
	 * {@link java.math.BigDecimal}, {@link java.math.BigInteger},
	 * {@link java.util.Currency}, {@link java.util.Locale}, {@link java.util.UUID},
	 * {@link java.net.URI}, and {@link java.net.URL}.
	 * <p>If the target type is not covered by any of the above, a convention-based
	 * conversion strategy will be used to convert the source {@code String} into the
	 * given target type by invoking a static factory method or factory constructor
	 * defined in the target type. The search algorithm used in this strategy is
	 * outlined below.
	 * <h4>Search Algorithm</h4>
	 * <ol>
	 * <li>Search for a single, non-private static factory method in the target
	 * type that converts from a String to the target type. Use the factory method
	 * if present.</li>
	 * <li>Search for a single, non-private constructor in the target type that
	 * accepts a String. Use the constructor if present.</li>
	 * </ol>
	 * <p>If multiple suitable factory methods are discovered, they will be ignored.
	 * If neither a single factory method nor a single constructor is found, the
	 * convention-based conversion strategy will not apply.
	 *
	 * @param source the source {@link String} to convert; may be {@code null}
	 * but only if the target type is a reference type
	 * @param context the context for the conversion; never {@code null}
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ConversionException if an error occurs during the conversion
	 */
	@Override
	public @Nullable Object convert(@Nullable String source, ConversionContext context) throws ConversionException {
		if (source == null) {
			if (context.targetType().isPrimitive()) {
				throw new ConversionException("Cannot convert null to primitive value of type " + context.targetType());
			}
			return null;
		}

		if (String.class.equals(context.targetType().getType())) {
			return source;
		}

		Optional<Converter<String, ?>> converter = stringToObjectConverters.stream().filter(
			candidate -> candidate.canConvert(context)).findFirst();
		if (converter.isPresent()) {
			try {
				return converter.get().convert(source, context);
			}
			catch (Exception ex) {
				if (ex instanceof ConversionException conversionException) {
					// simply rethrow it
					throw conversionException;
				}
				// else
				throw new ConversionException(
					"Failed to convert String \"%s\" to type %s".formatted(source, context.targetType()), ex);
			}
		}

		throw new ConversionException(
			"No built-in converter for source type java.lang.String and target type " + context.targetType());
	}

}
