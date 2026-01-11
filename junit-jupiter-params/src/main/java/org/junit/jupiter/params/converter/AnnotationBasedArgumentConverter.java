/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static java.util.Objects.requireNonNull;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Annotation;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.junit.jupiter.params.support.FieldContext;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code AnnotationBasedArgumentConverter} is an abstract base class for
 * {@link ArgumentConverter} implementations that also need to consume an
 * annotation in order to perform the conversion.
 *
 * @since 5.10
 * @see ArgumentConverter
 * @see AnnotationConsumer
 * @see SimpleArgumentConverter
 */
@API(status = MAINTAINED, since = "5.13.3")
public abstract class AnnotationBasedArgumentConverter<A extends Annotation>
		implements ArgumentConverter, AnnotationConsumer<A> {

	private @Nullable A annotation;

	public AnnotationBasedArgumentConverter() {
	}

	@Override
	public final void accept(A annotation) {
		this.annotation = Preconditions.notNull(annotation, "annotation must not be null");

	}

	@Override
	public final @Nullable Object convert(@Nullable Object source, ParameterContext context)
			throws ArgumentConversionException {
		return convert(source, context.getParameter().getType(), requireNonNull(this.annotation));
	}

	@Override
	public final @Nullable Object convert(@Nullable Object source, FieldContext context)
			throws ArgumentConversionException {
		return convert(source, context.getField().getType(), requireNonNull(this.annotation));
	}

	/**
	 * Convert the supplied {@code source} object into the supplied {@code targetType},
	 * based on metadata in the provided annotation.
	 *
	 * @param source the source object to convert; may be {@code null}
	 * @param targetType the target type the source object should be converted
	 * into; never {@code null}
	 * @param annotation the annotation to process; never {@code null}
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ArgumentConversionException in case an error occurs during the
	 * conversion
	 */
	protected abstract @Nullable Object convert(@Nullable Object source, Class<?> targetType, A annotation)
			throws ArgumentConversionException;

}
