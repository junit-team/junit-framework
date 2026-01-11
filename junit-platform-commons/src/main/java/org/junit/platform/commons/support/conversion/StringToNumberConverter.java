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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

import org.junit.platform.commons.util.Preconditions;

class StringToNumberConverter implements StringToObjectConverter {

	private static final Map<Class<?>, Function<String, ?>> CONVERTERS = Map.of( //
		Byte.class, Byte::decode, //
		Short.class, Short::decode, //
		Integer.class, Integer::decode, //
		Long.class, Long::decode, //
		Float.class, Float::valueOf, //
		Double.class, Double::valueOf, //
		// Technically, BigInteger and BigDecimal constructors are covered by
		// FallbackStringToObjectConverter, but we have explicit conversion
		// configured for them anyway.
		BigInteger.class, BigInteger::new, //
		BigDecimal.class, BigDecimal::new //
	);

	@Override
	public boolean canConvertTo(Class<?> targetType) {
		return CONVERTERS.containsKey(targetType);
	}

	@Override
	public Object convert(String source, Class<?> targetType) {
		Function<String, ?> converter = Preconditions.notNull(CONVERTERS.get(targetType),
			() -> "No registered converter for %s".formatted(targetType.getName()));
		return converter.apply(source.replace("_", ""));
	}

}
