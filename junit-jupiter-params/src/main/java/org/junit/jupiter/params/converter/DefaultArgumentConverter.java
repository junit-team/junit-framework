/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ClassLoaderUtils.getClassLoader;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.support.FieldContext;
import org.junit.platform.commons.support.conversion.ConversionException;
import org.junit.platform.commons.support.conversion.ConversionSupport;
import org.junit.platform.commons.support.conversion.TypeDescriptor;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * {@code DefaultArgumentConverter} is the default implementation of the
 * {@link ArgumentConverter} API.
 *
 * <p>The {@code DefaultArgumentConverter} is able to convert from strings to a
 * number of primitive types and their corresponding wrapper types (Byte, Short,
 * Integer, Long, Float, and Double), date and time types from the
 * {@code java.time} package, and some additional common Java types such as
 * {@link File}, {@link BigDecimal}, {@link BigInteger}, {@link Currency},
 * {@link Locale}, {@link URI}, {@link URL}, {@link UUID}, etc.
 *
 * <p>If the source and target types are identical, the source object will not
 * be modified.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.converter.ArgumentConverter
 * @see org.junit.platform.commons.support.conversion.ConversionSupport
 */
@API(status = INTERNAL, since = "5.0")
public class DefaultArgumentConverter implements ArgumentConverter {

	/**
	 * Property name used to set the format for the conversion of {@link Locale}
	 * arguments: {@value}
	 *
	 * <h4>Supported Values</h4>
	 * <ul>
	 * <li>{@code bcp_47}: uses the IETF BCP 47 language tag format, delegating
	 * the conversion to {@link Locale#forLanguageTag(String)}</li>
	 * <li>{@code iso_639}: uses the ISO 639 alpha-2 or alpha-3 language code
	 * format, delegating the conversion to {@link Locale#Locale(String)}</li>
	 * </ul>
	 *
	 * <p>If not specified, the default is {@code bcp_47}.
	 *
	 * @since 5.13
	 */
	public static final String DEFAULT_LOCALE_CONVERSION_FORMAT_PROPERTY_NAME = "junit.jupiter.params.arguments.conversion.locale.format";

	private static final Function<String, LocaleConversionFormat> TRANSFORMER = value -> LocaleConversionFormat.valueOf(
		value.strip().toUpperCase(Locale.ROOT));

	private final ExtensionContext context;

	public DefaultArgumentConverter(ExtensionContext context) {
		this.context = context;
	}

	@Override
	public final @Nullable Object convert(@Nullable Object source, ParameterContext context) {
		ClassLoader classLoader = getClassLoader(context.getDeclaringExecutable().getDeclaringClass());
		return convert(source, TypeDescriptor.forParameter(context.getParameter()), classLoader);
	}

	@Override
	public final @Nullable Object convert(@Nullable Object source, FieldContext context)
			throws ArgumentConversionException {
		ClassLoader classLoader = getClassLoader(context.getField().getDeclaringClass());
		return convert(source, TypeDescriptor.forField(context.getField()), classLoader);
	}

	public final @Nullable Object convert(@Nullable Object source, TypeDescriptor targetType, ClassLoader classLoader) {
		if (source == null) {
			if (targetType.isPrimitive()) {
				throw new ArgumentConversionException(
					"Cannot convert null to primitive value of type " + targetType.getType().getTypeName());
			}
			return null;
		}

		if (ReflectionUtils.isAssignableTo(source, targetType.getType())) {
			return source;
		}

		if (source instanceof String //
				&& targetType.getType() == Locale.class //
				&& getLocaleConversionFormat() == LocaleConversionFormat.BCP_47) {
			return Locale.forLanguageTag((String) source);
		}

		try {
			return delegateConversion(source, targetType, classLoader);
		}
		catch (ConversionException ex) {
			throw new ArgumentConversionException(ex.getMessage(), ex);
		}
	}

	private LocaleConversionFormat getLocaleConversionFormat() {
		return context.getConfigurationParameter(DEFAULT_LOCALE_CONVERSION_FORMAT_PROPERTY_NAME, TRANSFORMER) //
				.orElse(LocaleConversionFormat.BCP_47);
	}

	@Nullable
	Object delegateConversion(@Nullable Object source, TypeDescriptor targetType, ClassLoader classLoader) {
		return ConversionSupport.convert(source, targetType, classLoader);
	}

	enum LocaleConversionFormat {

		BCP_47,

		ISO_639

	}

}
