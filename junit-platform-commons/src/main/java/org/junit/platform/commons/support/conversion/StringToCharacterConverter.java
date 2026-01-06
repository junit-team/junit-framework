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

import org.junit.platform.commons.util.Preconditions;

class StringToCharacterConverter extends StringToWrapperTypeConverter<Character> {

	@Override
	boolean canConvert(Class<?> targetType) {
		return targetType == Character.class;
	}

	@Override
	Character convert(String source, Class<?> targetType) throws ConversionException {
		Preconditions.condition(source.length() == 1, () -> "String must have length of 1: " + source);
		return source.charAt(0);
	}

}
