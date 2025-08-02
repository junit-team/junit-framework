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
import static org.junit.platform.commons.util.ClassLoaderUtils.getDefaultClassLoader;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * {@code ConversionContext} encapsulates the <em>context</em> in which the
 * current conversion is being executed.
 *
 * <p>{@link Converter Converters} are provided an instance of
 * {@code ConversionContext} to perform their work.
 *
 * @param sourceType the descriptor of the source type
 * @param targetType the descriptor of the type the source should be converted into
 * @param classLoader the {@code ClassLoader} to use
 *
 * @since 6.0
 * @see Converter
 */
@API(status = EXPERIMENTAL, since = "6.0")
public record ConversionContext(TypeDescriptor sourceType, TypeDescriptor targetType, ClassLoader classLoader) {

	/**
	 * Create a new {@code ConversionContext}, expecting an instance of the
	 * source instead of its type descriptor.
	 *
	 * @param source the source instance; may be {@code null}
	 * @param targetType the descriptor of the type the source should be converted into
	 * @param classLoader the {@code ClassLoader} to use; may be {@code null} to
	 * use the default {@code ClassLoader}
	 */
	public ConversionContext(@Nullable Object source, TypeDescriptor targetType, @Nullable ClassLoader classLoader) {
		this(TypeDescriptor.forInstance(source), targetType,
			classLoader != null ? classLoader : getDefaultClassLoader());
	}

}
