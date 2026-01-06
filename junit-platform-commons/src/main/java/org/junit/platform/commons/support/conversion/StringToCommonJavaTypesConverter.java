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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.platform.commons.util.Preconditions;

class StringToCommonJavaTypesConverter extends StringToTargetTypeConverter<Object> {

	private static final Map<Class<?>, Function<String, ?>> CONVERTERS = Map.of( //
		// java.io and java.nio
		File.class, File::new, //
		Charset.class, Charset::forName, //
		Path.class, Paths::get,
		// java.net
		URI.class, URI::create, //
		URL.class, StringToCommonJavaTypesConverter::toURL,
		// java.util
		Currency.class, Currency::getInstance, //
		Locale.class, Locale::forLanguageTag, //
		UUID.class, UUID::fromString //
	);

	@Override
	boolean canConvert(Class<?> targetType) {
		return CONVERTERS.containsKey(targetType);
	}

	@Override
	Object convert(String source, Class<?> targetType) {
		Function<String, ?> converter = Preconditions.notNull(CONVERTERS.get(targetType),
			() -> "No registered converter for %s".formatted(targetType.getName()));
		return converter.apply(source);
	}

	private static URL toURL(String url) {
		try {
			return URI.create(url).toURL();
		}
		catch (MalformedURLException ex) {
			throw new ConversionException("Failed to convert String \"" + url + "\" to type java.net.URL", ex);
		}
	}

}
