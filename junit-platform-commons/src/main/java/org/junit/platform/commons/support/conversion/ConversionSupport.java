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

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.junit.platform.commons.util.ReflectionUtils.getWrapperType;

import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ClassLoaderUtils;

/**
 * {@code ConversionSupport} provides static utility methods for converting a
 * given object into an instance of a specified type.
 *
 * @since 1.11
 */
@API(status = MAINTAINED, since = "1.13.3")
public final class ConversionSupport {

	private static final List<StringToObjectConverter> stringToObjectConverters = List.of( //
		new StringToBooleanConverter(), //
		new StringToCharacterConverter(), //
		new StringToNumberConverter(), //
		new StringToClassConverter(), //
		new StringToEnumConverter(), //
		new StringToJavaTimeConverter(), //
		new StringToCommonJavaTypesConverter(), //
		new FallbackStringToObjectConverter() //
	);

	private ConversionSupport() {
		/* no-op */
	}

	/**
	 * Convert the supplied source {@code String} into an instance of the specified
	 * target type.
	 *
	 * <p>If the target type is {@code String}, the source {@code String} will not
	 * be modified.
	 *
	 * <p>Some forms of conversion require a {@link ClassLoader}. If none is
	 * provided, the {@linkplain ClassLoaderUtils#getDefaultClassLoader() default
	 * ClassLoader} will be used.
	 *
	 * <p>This method is able to convert strings into primitive types and their
	 * corresponding wrapper types ({@link Boolean}, {@link Character}, {@link Byte},
	 * {@link Short}, {@link Integer}, {@link Long}, {@link Float}, and
	 * {@link Double}), enum constants, date and time types from the
	 * {@code java.time} package, as well as common Java types such as {@link Class},
	 * {@link java.io.File}, {@link java.nio.file.Path}, {@link java.nio.charset.Charset},
	 * {@link java.math.BigDecimal}, {@link java.math.BigInteger},
	 * {@link java.util.Currency}, {@link java.util.Locale}, {@link java.util.UUID},
	 * {@link java.net.URI}, and {@link java.net.URL}.
	 *
	 * <p>If the target type is not covered by any of the above, a convention-based
	 * conversion strategy will be used to convert the source {@code String} into the
	 * given target type by invoking a static factory method or factory constructor
	 * defined in the target type. The search algorithm used in this strategy is
	 * outlined below.
	 *
	 * <h4>Search Algorithm</h4>
	 *
	 * <ol>
	 * <li>Search for a single, non-private static factory method in the target
	 * type that converts from a {@link String} to the target type. Use the
	 * factory method if present.</li>
	 * <li>Search for a single, non-private constructor in the target type that
	 * accepts a {@link String}. Use the constructor if present.</li>
	 * <li>Search for a single, non-private static factory method in the target
	 * type that converts from a {@link CharSequence} to the target type. Use the
	 * factory method if present.</li>
	 * <li>Search for a single, non-private constructor in the target type that
	 * accepts a {@link CharSequence}. Use the constructor if present.</li>
	 * </ol>
	 *
	 * <p>If multiple suitable factory methods or constructors are discovered they
	 * will be ignored. If neither a single factory method nor a single constructor
	 * is found, the convention-based conversion strategy will not apply.
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
	 */
	@SuppressWarnings("unchecked")
	public static <T> @Nullable T convert(@Nullable String source, Class<T> targetType,
			@Nullable ClassLoader classLoader) {
		if (source == null) {
			if (targetType.isPrimitive()) {
				throw new ConversionException(
					"Cannot convert null to primitive value of type " + targetType.getTypeName());
			}
			return null;
		}

		if (String.class.equals(targetType)) {
			return (T) source;
		}

		Class<?> targetTypeToUse = toWrapperType(targetType);
		Optional<StringToObjectConverter> converter = stringToObjectConverters.stream().filter(
			candidate -> candidate.canConvertTo(targetTypeToUse)).findFirst();
		if (converter.isPresent()) {
			try {
				ClassLoader classLoaderToUse = classLoader != null ? classLoader
						: ClassLoaderUtils.getDefaultClassLoader();
				return (T) converter.get().convert(source, targetTypeToUse, classLoaderToUse);
			}
			catch (Exception ex) {
				if (ex instanceof ConversionException conversionException) {
					// simply rethrow it
					throw conversionException;
				}
				// else
				throw new ConversionException(
					"Failed to convert String \"%s\" to type %s".formatted(source, targetType.getTypeName()), ex);
			}
		}

		throw new ConversionException(
			"No built-in converter for source type java.lang.String and target type " + targetType.getTypeName());
	}

	private static Class<?> toWrapperType(Class<?> targetType) {
		Class<?> wrapperType = getWrapperType(targetType);
		return wrapperType != null ? wrapperType : targetType;
	}

}
