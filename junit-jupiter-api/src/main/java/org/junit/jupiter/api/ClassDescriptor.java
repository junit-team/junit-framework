/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code ClassDescriptor} encapsulates functionality for a given {@link Class}.
 *
 * @since 5.8
 * @see ClassOrdererContext
 */
@API(status = STABLE, since = "5.10")
public interface ClassDescriptor {

	/**
	 * Get the class for this descriptor.
	 *
	 * @return the class; never {@code null}
	 */
	Class<?> getTestClass();

	/**
	 * Get the display name for this descriptor's {@link #getTestClass() class}.
	 *
	 * @return the display name for this descriptor's class; never {@code null}
	 * or blank
	 */
	String getDisplayName();

	/**
	 * Determine if an annotation of {@code annotationType} is either
	 * <em>present</em> or <em>meta-present</em> on the {@link Class} for
	 * this descriptor.
	 *
	 * @param annotationType the annotation type to search for; never {@code null}
	 * @return {@code true} if the annotation is present or meta-present
	 * @see #findAnnotation(Class)
	 * @see #findRepeatableAnnotations(Class)
	 */
	boolean isAnnotated(Class<? extends Annotation> annotationType);

	/**
	 * Find the first annotation of {@code annotationType} that is either
	 * <em>present</em> or <em>meta-present</em> on the {@link Class} for
	 * this descriptor.
	 *
	 * @param <A> the annotation type
	 * @param annotationType the annotation type to search for; never {@code null}
	 * @return an {@code Optional} containing the annotation; never {@code null} but
	 * potentially empty
	 * @see #isAnnotated(Class)
	 * @see #findRepeatableAnnotations(Class)
	 */
	<A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType);

	/**
	 * Find all <em>repeatable</em> {@linkplain Annotation annotations} of
	 * {@code annotationType} that are either <em>present</em> or
	 * <em>meta-present</em> on the {@link Class} for this descriptor.
	 *
	 * @param <A> the annotation type
	 * @param annotationType the repeatable annotation type to search for; never
	 * {@code null}
	 * @return the list of all such annotations found; neither {@code null} nor
	 * mutable, but potentially empty
	 * @see #isAnnotated(Class)
	 * @see #findAnnotation(Class)
	 * @see java.lang.annotation.Repeatable
	 */
	<A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType);

}
