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

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;

class StringToClassConverter implements Converter<String, Class<?>> {

	@Override
	public boolean canConvert(ConversionContext context) {
		return !context.sourceType().equals(TypeDescriptor.NONE) && context.targetType().getType() == Class.class;
	}

	@Override
	public Class<?> convert(@Nullable String className, ConversionContext context) {
		Preconditions.notNull(className, "className cannot be null");
		// @formatter:off
		return ReflectionSupport.tryToLoadClass(className, context.classLoader())
				.getNonNullOrThrow(cause -> new ConversionException(
						"Failed to convert String \"" + className + "\" to type java.lang.Class", cause));
		// @formatter:on
	}

}
