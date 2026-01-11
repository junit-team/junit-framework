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
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code MethodDescriptor} encapsulates functionality for a given {@link Method}.
 *
 * @since 5.4
 * @see MethodOrdererContext
 */
@API(status = STABLE, since = "5.7")
public interface MethodDescriptor {

	/**
	 * Get the method for this descriptor.
	 *
	 * @return the method; never {@code null}
	 */
	Method getMethod();

	/**
	 * Get the display name for this descriptor's {@link #getMethod() method}.
	 *
	 * @return the display name for this descriptor's method; never {@code null}
	 * or blank
	 * @since 5.7
	 */
	@API(status = STABLE, since = "5.10")
	String getDisplayName();

	/**
	 * Determine if an annotation of {@code annotationType} is either
	 * <em>present</em> or <em>meta-present</em> on the {@link Method} for
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
	 * <em>present</em> or <em>meta-present</em> on the {@link Method} for
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
	 * <em>meta-present</em> on the {@link Method} for this descriptor.
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
