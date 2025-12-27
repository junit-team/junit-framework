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

@SuppressWarnings("rawtypes")
class StringToEnumConverter extends StringToTargetTypeConverter<Enum> {

	@Override
	boolean canConvert(Class<?> targetType) {
		return targetType.isEnum();
	}

	@SuppressWarnings("unchecked")
	@Override
	Enum convert(String source, Class targetType) throws ConversionException {
		return Enum.valueOf(targetType, source);
	}

}
